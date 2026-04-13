package filelocator.model;

public record FileEntry(
        String path,
        String name,
        String nameLower,
        boolean isDirectory,
        long size,
        long lastModified) {
}
