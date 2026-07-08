package filelocator;

import javax.swing.SwingUtilities;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.formdev.flatlaf.FlatIntelliJLaf;

import filelocator.repository.IndexRepository;
import filelocator.repository.InMemoryIndexRepository;
import filelocator.service.IndexingService;
import filelocator.service.SearchService;
import filelocator.ui.MainFrame;

public class FileLocatorMainApp {
    private static final Logger log = Logger.getLogger(FileLocatorMainApp.class.getName());

    public static void main(String[] args) {
        setupModernUI();

        // Initialize Repository & Services
        IndexRepository indexRepository = new InMemoryIndexRepository();
        IndexingService indexingService = new IndexingService(indexRepository);
        SearchService searchService = new SearchService(indexRepository);

        // Start UI immediately
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(indexRepository, indexingService, searchService);
            frame.setVisible(true);

            // Load index in background
            Thread loadThread = new Thread(() -> {
                long start = System.currentTimeMillis();
                indexRepository.load();
                log.info("Index loaded in " + (System.currentTimeMillis() - start) + " ms");

                // Notify UI when finished
                SwingUtilities.invokeLater(() -> {
                    frame.onIndexLoaded();
                });
            }, "IndexLoaderThread");
            loadThread.setDaemon(true);
            loadThread.start();
        });
    }

    private static void setupModernUI() {
        try {
            // Prevent FlatLaf from loading native libraries (DLLs) which can be blocked by Endpoint Protection
            System.setProperty("flatlaf.useNativeLibrary", "false");
            
            // Apply FlatLaf for a modern look
            FlatIntelliJLaf.setup();
            
            // Further font customizations if needed
            String fontName = System.getProperty("os.name").toLowerCase().contains("win") ? "Segoe UI" : "Arial";
            java.awt.Font modernFont = new java.awt.Font(fontName, java.awt.Font.PLAIN, 13);
            javax.swing.UIManager.put("defaultFont", modernFont);
            
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to initialize FlatLaf", ex);
        }
    }
}