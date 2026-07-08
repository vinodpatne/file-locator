package filelocator.service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

import filelocator.model.FileEntry;
import filelocator.model.UserPreferences;
import filelocator.repository.IndexRepository;

public class IndexingService {
    private static final Logger log = Logger.getLogger(IndexingService.class.getName());
    private static final Set<String> IGNORED_DIRS = Set.of(
            "c:\\windows", "c:\\program files", "c:\\$recycle.bin", "c:\\system volume information");

    private final IndexRepository indexRepository;
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private volatile long lastIndexTime = 0;
    private final ExecutorService indexExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "IndexingService-Thread");
        t.setDaemon(true);
        return t;
    });

    public IndexingService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
        startCpuMonitor();
    }

    private void startCpuMonitor() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "IndexingCpuMonitor");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (isIndexing.get()) {
                    return;
                }

                long now = System.currentTimeMillis();
                if (now - lastIndexTime < 10 * 60 * 1000) {
                    return; // 10-minute cooldown
                }

                com.sun.management.OperatingSystemMXBean osBean = 
                        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                double cpuLoad = osBean.getCpuLoad();
                
                if (cpuLoad >= 0 && cpuLoad < 0.15) {
                    log.info("System CPU utilization is low (" + String.format("%.2f%%", cpuLoad * 100) + "). Triggering automatic background indexing.");
                    triggerBackgroundIndex();
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error in CPU monitoring task", e);
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    private void triggerBackgroundIndex() {
        UserPreferences prefs = UserPreferences.load();
        String defaultLoc = prefs.getDefaultLocation();
        List<String> rootsToScan = new java.util.ArrayList<>();
        if ("This PC".equalsIgnoreCase(defaultLoc) || defaultLoc.isBlank()) {
            for (java.io.File root : java.io.File.listRoots()) {
                rootsToScan.add(root.getAbsolutePath());
            }
        } else {
            rootsToScan.add(defaultLoc);
        }

        buildIndex(rootsToScan, null);
    }

    public void buildIndex(List<String> rootPaths, Runnable onComplete) {
        if (isIndexing.get()) {
            log.warning("Indexing already in progress. Ignoring request.");
            return;
        }
        indexExecutor.submit(() -> {
            if (!isIndexing.compareAndSet(false, true)) {
                return;
            }
            try {
                buildIndexInternal(rootPaths);
                lastIndexTime = System.currentTimeMillis();
                if (onComplete != null) {
                    javax.swing.SwingUtilities.invokeLater(onComplete);
                }
            } finally {
                isIndexing.set(false);
            }
        });
    }

    private void buildIndexInternal(List<String> rootPaths) {
        log.info("Starting index build for roots: " + rootPaths);
        
        List<FileEntry> tempEntries = new java.util.ArrayList<>();
        com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        java.util.concurrent.atomic.AtomicInteger fileCounter = new java.util.concurrent.atomic.AtomicInteger(0);

        for (String rootPath : rootPaths) {
            Path startPath = Paths.get(rootPath);
            if (!Files.exists(startPath)) {
                log.warning("Path does not exist, skipping: " + startPath);
                continue;
            }

            try {
                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        checkCpuAndThrottle(osBean, fileCounter);

                        String pathStr = dir.toString().toLowerCase();
                        for (String ignored : IGNORED_DIRS) {
                            if (pathStr.startsWith(ignored)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }
                        
                        tempEntries.add(new FileEntry(
                                dir.toAbsolutePath().toString(),
                                dir.getFileName() != null ? dir.getFileName().toString() : dir.toString(),
                                dir.getFileName() != null ? dir.getFileName().toString().toLowerCase() : dir.toString().toLowerCase(),
                                true,
                                0,
                                attrs.lastModifiedTime().toMillis()
                        ));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        checkCpuAndThrottle(osBean, fileCounter);

                        tempEntries.add(new FileEntry(
                                file.toAbsolutePath().toString(),
                                file.getFileName().toString(),
                                file.getFileName().toString().toLowerCase(),
                                false,
                                attrs.size(),
                                attrs.lastModifiedTime().toMillis()
                        ));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to walk directory: " + startPath, e);
            }
        }
        
        indexRepository.replaceIndexAtomically(tempEntries);
        log.info("Finished index build. Total entries: " + indexRepository.size());
    }

    private void checkCpuAndThrottle(com.sun.management.OperatingSystemMXBean osBean, java.util.concurrent.atomic.AtomicInteger fileCounter) {
        int count = fileCounter.incrementAndGet();
        if (count % 100 == 0) {
            double load = osBean.getCpuLoad();
            if (load > 0.25) {
                try {
                    log.fine("CPU utilization is high (" + String.format("%.2f%%", load * 100) + "), throttling indexing task...");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
