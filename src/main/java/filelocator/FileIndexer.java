package filelocator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

public class FileIndexer {
    // Folders to strictly ignore to prevent hanging or permission loops
    private static final Set<String> IGNORED_DIRS = Set.of("c:\\windows", "c:\\program files", "c:\\$recycle.bin",
            "c:\\system volume information");

    public static void runIncrementalUpdate(List<String> rootPaths, Runnable onComplete) {
        new Thread(() -> {
            try {
                for (String rootPath : rootPaths) {
                    Path startPath = Paths.get(rootPath);

                    // Skip if the drive/path doesn't exist or isn't ready (like an empty DVD drive)
                    if (!Files.exists(startPath))
                        continue;

                    Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            String pathStr = dir.toString().toLowerCase();
                            for (String ignored : IGNORED_DIRS) {
                                if (pathStr.startsWith(ignored))
                                    return FileVisitResult.SKIP_SUBTREE;
                            }
                            // Add folder to index
                            FileSearcher.updateEntry(
                                    dir.toAbsolutePath().toString(),
                                    dir.getFileName() != null ? dir.getFileName().toString() : dir.toString(),
                                    true,
                                    0,
                                    attrs.lastModifiedTime().toMillis());
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            // Add file to index
                            FileSearcher.updateEntry(
                                    file.toAbsolutePath().toString(),
                                    file.getFileName().toString(),
                                    false,
                                    attrs.size(),
                                    attrs.lastModifiedTime().toMillis());
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.SKIP_SUBTREE; // Skip access denied
                        }
                    });
                }

                FileSearcher.saveIndex();
                if (onComplete != null)
                    onComplete.run();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}