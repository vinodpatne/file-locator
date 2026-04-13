package filelocator.ui.worker;

import java.util.List;
import javax.swing.SwingWorker;
import filelocator.service.IndexingService;
import filelocator.repository.IndexRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IndexWorker extends SwingWorker<Void, Void> {
    private final IndexingService indexingService;
    private final IndexRepository indexRepository;
    private final List<String> rootPaths;
    private final Runnable onComplete;

    @Override
    protected Void doInBackground() {
        indexingService.buildIndex(rootPaths);
        return null;
    }

    @Override
    protected void done() {
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
