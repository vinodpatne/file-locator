package filelocator.ui.worker;

import java.util.List;
import javax.swing.SwingWorker;
import filelocator.model.FileEntry;
import filelocator.model.SearchCriteria;
import filelocator.service.SearchService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SearchWorker extends SwingWorker<List<FileEntry>, Void> {
    private final SearchService searchService;
    private final SearchCriteria criteria;
    private final Consumer<List<FileEntry>> onResult;

    @Override
    protected List<FileEntry> doInBackground() {
        return searchService.search(criteria);
    }

    @Override
    protected void done() {
        try {
            List<FileEntry> results = get();
            if (onResult != null) {
                onResult.accept(results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
