package filelocator.model;

public record SearchCriteria(
        String query,
        String extension,
        String locationScope,
        boolean recursive,
        boolean useRegex,
        boolean includeFolders,
        long minSize,
        long maxSize,
        long minDate,
        long maxDate,
        String sortBy,
        boolean sortAsc,
        boolean findDuplicates) {
}
