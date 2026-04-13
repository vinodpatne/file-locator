package filelocator.service;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import filelocator.model.FileEntry;
import filelocator.model.SearchCriteria;
import filelocator.repository.IndexRepository;

@Slf4j
@RequiredArgsConstructor
public class SearchService {
    private final IndexRepository indexRepository;

    public List<FileEntry> search(SearchCriteria criteria) {
        log.debug("Executing search with criteria: {}", criteria);
        
        final String extLower = (criteria.extension() != null && !criteria.extension().isBlank())
                ? (criteria.extension().startsWith(".") ? criteria.extension().toLowerCase() : "." + criteria.extension().toLowerCase())
                : null;
        final String scopeLower = (criteria.locationScope() != null) ? criteria.locationScope().toLowerCase() : "";

        Pattern pattern = null;
        boolean isExactMatch = false;
        String cleanQuery = (criteria.query() != null) ? criteria.query().toLowerCase() : "";

        try {
            if (criteria.useRegex() && !cleanQuery.isEmpty()) {
                pattern = Pattern.compile(cleanQuery);
            } else if (!cleanQuery.isEmpty()) {
                if ((cleanQuery.startsWith("\"") && cleanQuery.endsWith("\""))
                        || (cleanQuery.startsWith("'") && cleanQuery.endsWith("'"))) {
                    isExactMatch = true;
                    if (cleanQuery.length() > 2) {
                        cleanQuery = cleanQuery.substring(1, cleanQuery.length() - 1);
                    }
                } else if (cleanQuery.contains("*") || cleanQuery.contains("?")) {
                    pattern = Pattern.compile(convertGlobToRegex(cleanQuery));
                }
            }
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern: {}", cleanQuery, e);
            pattern = null;
        }

        final Pattern finalPattern = pattern;
        final String finalTerm = cleanQuery;
        final boolean finalExact = isExactMatch;

        // Sorting Logic
        Comparator<FileEntry> comparator = switch (criteria.sortBy()) {
            case "Size" -> Comparator.comparingLong(FileEntry::size);
            case "Date Modified" -> Comparator.comparingLong(FileEntry::lastModified);
            default -> Comparator.comparing(FileEntry::nameLower); // Default to Name
        };
        
        if (!criteria.sortAsc()) {
            comparator = comparator.reversed();
        }

        List<FileEntry> results = indexRepository.getAll().parallelStream()
                // 1. Directory Filter
                .filter(e -> criteria.includeFolders() || !e.isDirectory())
                // 2. Size Filter
                .filter(e -> (criteria.minSize() <= 0 || e.size() >= criteria.minSize()) &&
                             (criteria.maxSize() <= 0 || e.size() <= criteria.maxSize()))
                // 3. Date Filter
                .filter(e -> (criteria.minDate() <= 0 || e.lastModified() >= criteria.minDate()) &&
                             (criteria.maxDate() <= 0 || e.lastModified() <= criteria.maxDate()))
                // 4. Scope Filter
                .filter(e -> {
                    if (!scopeLower.isEmpty()) {
                        String p = e.path().toLowerCase();
                        if (!p.startsWith(scopeLower)) {
                            return false;
                        }
                        if (!criteria.recursive()) {
                            int lastSlash = p.lastIndexOf(File.separatorChar);
                            if (lastSlash == -1) {
                                return false;
                            }
                            String parent = p.substring(0, lastSlash);
                            return parent.equals(scopeLower) || parent.equals(scopeLower.substring(0, scopeLower.length() - 1));
                        }
                    }
                    return true;
                })
                // 5. Extension Filter (Skip extension checks on folders)
                .filter(e -> e.isDirectory() || extLower == null || e.nameLower().endsWith(extLower))
                // 6. Name Filter
                .filter(e -> {
                    if (finalTerm.isEmpty()) return true;
                    if (finalPattern != null) return finalPattern.matcher(e.nameLower()).find();
                    if (finalExact) return e.nameLower().equals(finalTerm);
                    return e.nameLower().contains(finalTerm);
                })
                .sorted(comparator)
                .limit(200)
                .collect(Collectors.toList());
                
        log.info("Search returned {} files.", results.size());
        return results;
    }

    private String convertGlobToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append(".");
                case '.' -> sb.append("\\.");
                case '\\' -> sb.append("\\\\");
                case '{' -> sb.append("\\{");
                case '}' -> sb.append("\\}");
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");
                case '[' -> sb.append("\\[");
                case ']' -> sb.append("\\]");
                case '+' -> sb.append("\\+");
                case '^' -> sb.append("\\^");
                case '$' -> sb.append("\\$");
                default -> sb.append(c);
            }
        }
        sb.append("$");
        return sb.toString();
    }
}
