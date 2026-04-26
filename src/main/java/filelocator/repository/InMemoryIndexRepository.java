package filelocator.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import filelocator.model.FileEntry;

@Slf4j
public class InMemoryIndexRepository implements IndexRepository {
    private static final File INDEX_FILE = new File("files.idx");
    private static final int INDEX_VERSION = 2;

    private volatile ConcurrentHashMap<String, FileEntry> indexMap = new ConcurrentHashMap<>();

    @Override
    public void load() {
        if (!INDEX_FILE.exists()) {
            log.info("Index file {} not found. Starting with empty index.", INDEX_FILE.getAbsolutePath());
            return;
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(INDEX_FILE)))) {
            int version = dis.readInt();
            if (version != INDEX_VERSION) {
                log.warn("Old index format detected (version {}). Please re-index.", version);
                return;
            }

            ConcurrentHashMap<String, FileEntry> newMap = new ConcurrentHashMap<>();
            while (dis.available() > 0) {
                String path = dis.readUTF();
                String name = dis.readUTF();
                boolean isDir = dis.readBoolean();
                long size = dis.readLong();
                long lastMod = dis.readLong();
                newMap.put(path, new FileEntry(path, name, name.toLowerCase(), isDir, size, lastMod));
            }
            this.indexMap = newMap;
            log.info("Loaded {} entries from index.", indexMap.size());
        } catch (IOException e) {
            log.error("Failed to load index from {}", INDEX_FILE.getAbsolutePath(), e);
        }
    }

    @Override
    public void replaceIndexAtomically(Collection<FileEntry> newEntries) {
        File tempFile = new File(INDEX_FILE.getParentFile() == null ? "." : INDEX_FILE.getParentFile().getPath(), INDEX_FILE.getName() + ".tmp");
        if (tempFile.getParentFile() != null && !tempFile.getParentFile().exists()) {
            tempFile.getParentFile().mkdirs();
        }
        
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            dos.writeInt(INDEX_VERSION);
            for (FileEntry e : newEntries) {
                dos.writeUTF(e.path());
                dos.writeUTF(e.name());
                dos.writeBoolean(e.isDirectory());
                dos.writeLong(e.size());
                dos.writeLong(e.lastModified());
            }
        } catch (IOException e) {
            log.error("Failed to save temp index file to {}", tempFile.getAbsolutePath(), e);
            throw new RuntimeException("Temp index file write failed", e);
        }

        try {
            java.nio.file.Files.move(tempFile.toPath(), INDEX_FILE.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Atomic move failed, attempting standard replace", e);
            try {
                java.nio.file.Files.move(tempFile.toPath(), INDEX_FILE.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                log.error("Standard replace failed", ex);
                throw new RuntimeException("Index file swap failed", ex);
            }
        }
        
        ConcurrentHashMap<String, FileEntry> newMap = new ConcurrentHashMap<>();
        for (FileEntry e : newEntries) {
            newMap.put(e.path(), e);
        }
        this.indexMap = newMap;
        log.info("Index atomically swapped. New size: {}", newMap.size());
    }

    @Override
    public void add(FileEntry entry) {
        indexMap.put(entry.path(), entry);
    }

    @Override
    public void addAll(Collection<FileEntry> entries) {
        for (FileEntry entry : entries) {
            indexMap.put(entry.path(), entry);
        }
    }

    @Override
    public Collection<FileEntry> getAll() {
        return indexMap.values();
    }

    @Override
    public int size() {
        return indexMap.size();
    }

    @Override
    public void remove(String path) {
        indexMap.remove(path);
    }

    @Override
    public void removeByPrefix(String prefixPath) {
        String prefix = prefixPath.toLowerCase() + File.separator;
        String exact = prefixPath.toLowerCase();
        indexMap.keySet().removeIf(k -> {
            String p = k.toLowerCase();
            return p.equals(exact) || p.startsWith(prefix);
        });
    }

    @Override
    public void save() {
        replaceIndexAtomically(this.indexMap.values());
    }

    @Override
    public void clear() {
        indexMap.clear();
    }
}
