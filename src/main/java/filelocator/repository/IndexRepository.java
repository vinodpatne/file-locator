package filelocator.repository;

import java.util.Collection;
import filelocator.model.FileEntry;

public interface IndexRepository {
    void load();
    void add(FileEntry entry);
    void addAll(Collection<FileEntry> entries);
    void replaceIndexAtomically(Collection<FileEntry> newEntries);
    Collection<FileEntry> getAll();
    int size();
    void clear();
}
