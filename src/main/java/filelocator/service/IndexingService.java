package filelocator.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import filelocator.model.FileEntry;
import filelocator.repository.IndexRepository;

@Slf4j
@RequiredArgsConstructor
public class IndexingService {
    private static final Set<String> IGNORED_DIRS = Set.of(
            "c:\\windows", "c:\\program files", "c:\\$recycle.bin", "c:\\system volume information");

    private final IndexRepository indexRepository;

    public void buildIndex(List<String> rootPaths) {
        log.info("Starting index build for roots: {}", rootPaths);
        
        List<FileEntry> tempEntries = new java.util.ArrayList<>();

        for (String rootPath : rootPaths) {
            Path startPath = Paths.get(rootPath);
            if (!Files.exists(startPath)) {
                log.warn("Path does not exist, skipping: {}", startPath);
                continue;
            }

            try {
                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
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
                        return FileVisitResult.SKIP_SUBTREE; // Skip access denied
                    }
                });
            } catch (IOException e) {
                log.error("Failed to walk directory: {}", startPath, e);
            }
        }
        
        indexRepository.replaceIndexAtomically(tempEntries);
        log.info("Finished index build. Total entries: {}", indexRepository.size());
    }
}
