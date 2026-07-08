package filelocator.ui.worker;

import java.util.List;
import javax.swing.SwingWorker;
import filelocator.model.FileEntry;
import filelocator.model.SearchCriteria;
import filelocator.service.SearchService;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SearchWorker extends SwingWorker<List<FileEntry>, Void> {
    private static final Logger log = Logger.getLogger(SearchWorker.class.getName());

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
        } catch (java.util.concurrent.CancellationException e) {
            // Ignored, search was cancelled
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error in search execution", e);
        }
    }
}
