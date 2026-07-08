package filelocator.service;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import java.util.logging.Logger;
import java.util.logging.Level;

import filelocator.model.FileEntry;
import filelocator.model.SearchCriteria;
import filelocator.repository.IndexRepository;

@RequiredArgsConstructor
public class SearchService {
    private static final Logger log = Logger.getLogger(SearchService.class.getName());
    private final IndexRepository indexRepository;

    public List<FileEntry> search(SearchCriteria criteria) {
        log.fine("Executing search with criteria: " + criteria);
        
        final List<Pattern> extPatterns = new java.util.ArrayList<>();
        String rawExt = (criteria.extension() != null) ? criteria.extension().replace(" ", "") : "";
        if (!rawExt.isEmpty()) {
            String[] parts = rawExt.split(",");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    String glob = part.toLowerCase();
                    if (glob.startsWith("*.")) {
                        // Keep as is
                    } else if (glob.startsWith(".")) {
                        glob = "*" + glob;
                    } else {
                        glob = "*." + glob;
                    }
                    try {
                        extPatterns.add(Pattern.compile(convertGlobToRegex(glob)));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Invalid extension pattern: " + part, e);
                    }
                }
            }
        }
        String scope = (criteria.locationScope() != null) ? criteria.locationScope().trim() : "";
        if (!scope.isEmpty() && !"This PC".equalsIgnoreCase(scope)) {
            // Normalize single drive letters or drive specs (e.g. "C", "C:", "c", "c:") to drive roots (e.g. "C:\")
            if (scope.length() == 1 && Character.isLetter(scope.charAt(0))) {
                scope = scope + ":" + java.io.File.separator;
            } else if (scope.length() == 2 && Character.isLetter(scope.charAt(0)) && scope.charAt(1) == ':') {
                scope = scope + java.io.File.separator;
            }
            try {
                scope = new java.io.File(scope).getAbsolutePath();
                if (!scope.endsWith(java.io.File.separator)) {
                    scope += java.io.File.separator;
                }
                scope = scope.toLowerCase();
            } catch (Exception e) {
                scope = scope.toLowerCase();
            }
        } else {
            scope = "";
        }
        final String scopeLower = scope;

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
            log.log(Level.WARNING, "Invalid regex pattern: " + cleanQuery, e);
            pattern = null;
        }

        final Pattern finalPattern = pattern;
        final String finalTerm = cleanQuery;
        final boolean finalExact = isExactMatch;

        // Sorting Logic
        Comparator<FileEntry> comparator = switch (criteria.sortBy()) {
            case "Size" -> Comparator.comparingLong(FileEntry::size);
            case "Date Modified" -> Comparator.comparingLong(FileEntry::lastModified);
            case "File Path" -> Comparator.comparing(FileEntry::path);
            default -> Comparator.comparing(FileEntry::nameLower); // Default to Name
        };
        
        if (!criteria.sortAsc()) {
            comparator = comparator.reversed();
        }

        java.util.stream.Stream<FileEntry> stream = indexRepository.getAll().parallelStream()
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
                            int lastSlash = p.lastIndexOf(java.io.File.separatorChar);
                            if (lastSlash == -1) {
                                return false;
                            }
                            String parent = p.substring(0, lastSlash + 1);
                            return parent.equals(scopeLower);
                        }
                    }
                    return true;
                })
                // 5. Extension Filter (Skip extension checks on folders)
                .filter(e -> {
                    if (e.isDirectory() || extPatterns.isEmpty()) {
                        return true;
                    }
                    String name = e.nameLower();
                    for (Pattern p : extPatterns) {
                        if (p.matcher(name).matches()) {
                            return true;
                        }
                    }
                    return false;
                })
                // 6. Name Filter
                .filter(e -> {
                    if (finalTerm.isEmpty()) return true;
                    if (finalPattern != null) return finalPattern.matcher(e.nameLower()).find();
                    if (finalExact) return e.nameLower().equals(finalTerm);
                    return e.nameLower().contains(finalTerm);
                });

        if (criteria.findDuplicates()) {
            // Group by name + size
            java.util.Map<String, List<FileEntry>> grouped = stream.collect(Collectors.groupingBy(
                    e -> e.nameLower() + "::" + e.size()
            ));
            stream = grouped.values().stream()
                    .filter(list -> list.size() > 1)
                    .flatMap(List::stream);
        }

        List<FileEntry> results = stream
                .sorted(comparator)
                .limit(200)
                .collect(Collectors.toList());
                
        log.info("Search pattern: '" + (criteria.query() != null ? criteria.query() : "") + 
                 "', Target directory: '" + (scopeLower.isEmpty() ? "This PC" : scopeLower) + 
                 "', Count: " + results.size());
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
