package filelocator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class FileSearcher {
    private static final File INDEX_FILE = new File(System.getProperty("user.home"), ".file-search/files.idx");
    private static final ConcurrentHashMap<String, FileEntry> indexMap = new ConcurrentHashMap<>();

    // Magic number to detect old vs new index formats
    private static final int INDEX_VERSION = 2;

    // Updated Record to hold all metadata
    public record FileEntry(String path, String name, String nameLower, boolean isDirectory, long size,
            long lastModified) {
    }

    public static void loadIndex() {
        if (!INDEX_FILE.exists())
            return;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(INDEX_FILE)))) {
            // Check version
            int version = dis.readInt();
            if (version != INDEX_VERSION) {
                System.out.println("Old index format detected. Please re-index.");
                return;
            }

            indexMap.clear();
            while (dis.available() > 0) {
                String path = dis.readUTF();
                String name = dis.readUTF();
                boolean isDir = dis.readBoolean();
                long size = dis.readLong();
                long lastMod = dis.readLong();
                indexMap.put(path, new FileEntry(path, name, name.toLowerCase(), isDir, size, lastMod));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveIndex() {
        if (!INDEX_FILE.getParentFile().exists())
            INDEX_FILE.getParentFile().mkdirs();
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(INDEX_FILE)))) {
            dos.writeInt(INDEX_VERSION); // Write format version
            for (FileEntry e : indexMap.values()) {
                dos.writeUTF(e.path());
                dos.writeUTF(e.name());
                dos.writeBoolean(e.isDirectory());
                dos.writeLong(e.size());
                dos.writeLong(e.lastModified());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateEntry(String path, String name, boolean isDir, long size, long lastMod) {
        indexMap.put(path, new FileEntry(path, name, name.toLowerCase(), isDir, size, lastMod));
    }

    // --- MEGA SEARCH LOGIC ---
    public static List<FileEntry> search(
            String query, String extension, String locationScope, boolean recursive, boolean useRegex,
            boolean includeFolders, long minSize, long maxSize, long minDate, long maxDate,
            String sortBy, boolean sortAsc) {

        final String extLower = (extension != null && !extension.isBlank())
                ? (extension.startsWith(".") ? extension.toLowerCase() : "." + extension.toLowerCase())
                : null;
        final String scopeLower = (locationScope != null) ? locationScope.toLowerCase() : "";

        Pattern pattern = null;
        boolean isExactMatch = false;
        String cleanQuery = (query != null) ? query.toLowerCase() : "";

        try {
            if (useRegex && !cleanQuery.isEmpty()) {
                pattern = Pattern.compile(cleanQuery);
            } else if (!cleanQuery.isEmpty()) {
                if ((cleanQuery.startsWith("\"") && cleanQuery.endsWith("\""))
                        || (cleanQuery.startsWith("'") && cleanQuery.endsWith("'"))) {
                    isExactMatch = true;
                    if (cleanQuery.length() > 2)
                        cleanQuery = cleanQuery.substring(1, cleanQuery.length() - 1);
                } else if (cleanQuery.contains("*") || cleanQuery.contains("?")) {
                    pattern = Pattern.compile(convertGlobToRegex(cleanQuery));
                }
            }
        } catch (PatternSyntaxException e) {
            pattern = null;
        }

        final Pattern finalPattern = pattern;
        final String finalTerm = cleanQuery;
        final boolean finalExact = isExactMatch;

        // Sorting Logic
        Comparator<FileEntry> comparator = switch (sortBy) {
            case "Size" -> Comparator.comparingLong(FileEntry::size);
            case "Date Modified" -> Comparator.comparingLong(FileEntry::lastModified);
            default -> Comparator.comparing(FileEntry::nameLower); // Default to Name
        };
        if (!sortAsc)
            comparator = comparator.reversed();

        return indexMap.values().parallelStream()
                // 1. Directory Filter
                .filter(e -> includeFolders || !e.isDirectory())
                // 2. Size Filter (Ignore size for folders usually, but let's apply to both for
                // strictness)
                .filter(e -> (minSize <= 0 || e.size() >= minSize) && (maxSize <= 0 || e.size() <= maxSize))
                // 3. Date Filter
                .filter(e -> (minDate <= 0 || e.lastModified() >= minDate)
                        && (maxDate <= 0 || e.lastModified() <= maxDate))
                // 4. Scope Filter
                .filter(e -> {
                    if (!scopeLower.isEmpty()) {
                        String p = e.path().toLowerCase();
                        if (!p.startsWith(scopeLower))
                            return false;
                        if (!recursive) {
                            int lastSlash = p.lastIndexOf(File.separatorChar);
                            if (lastSlash == -1)
                                return false;
                            String parent = p.substring(0, lastSlash);
                            return parent.equals(scopeLower)
                                    || parent.equals(scopeLower.substring(0, scopeLower.length() - 1));
                        }
                    }
                    return true;
                })
                // 5. Extension Filter (Skip extension checks on folders)
                .filter(e -> e.isDirectory() || extLower == null || e.nameLower().endsWith(extLower))
                // 6. Name Filter
                .filter(e -> {
                    if (finalTerm.isEmpty())
                        return true;
                    if (finalPattern != null)
                        return finalPattern.matcher(e.nameLower()).find();
                    if (finalExact)
                        return e.nameLower().equals(finalTerm);
                    return e.nameLower().contains(finalTerm);
                })
                // Sort and Collect
                .sorted(comparator)
                .limit(200) // Increased limit since sorting makes scrolling through results more useful
                .collect(Collectors.toList());
    }

    public static int getIndexSize() {
        return indexMap.size();
    }

    private static String convertGlobToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append(".");
                    break;
                case '.':
                    sb.append("\\.");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '{':
                    sb.append("\\{");
                    break;
                case '}':
                    sb.append("\\}");
                    break;
                case '(':
                    sb.append("\\(");
                    break;
                case ')':
                    sb.append("\\)");
                    break;
                case '[':
                    sb.append("\\[");
                    break;
                case ']':
                    sb.append("\\]");
                    break;
                case '+':
                    sb.append("\\+");
                    break;
                case '^':
                    sb.append("\\^");
                    break;
                case '$':
                    sb.append("\\$");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append("$");
        return sb.toString();
    }
}