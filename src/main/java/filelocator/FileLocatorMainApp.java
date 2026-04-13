package filelocator;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatIntelliJLaf;
import lombok.extern.slf4j.Slf4j;

import filelocator.repository.IndexRepository;
import filelocator.repository.InMemoryIndexRepository;
import filelocator.service.IndexingService;
import filelocator.service.SearchService;
import filelocator.ui.MainFrame;

@Slf4j
public class FileLocatorMainApp {

    public static void main(String[] args) {
        setupModernUI();

        // Initialize Repository
        IndexRepository indexRepository = new InMemoryIndexRepository();
        
        // Let's load the index in the background to not stall startup
        Thread loadThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            indexRepository.load();
            log.info("Index loaded in {} ms", (System.currentTimeMillis() - start));
            
            // Services
            IndexingService indexingService = new IndexingService(indexRepository);
            SearchService searchService = new SearchService(indexRepository);

            // Start UI
            SwingUtilities.invokeLater(() -> {
                MainFrame frame = new MainFrame(indexRepository, indexingService, searchService);
                frame.setVisible(true);
            });
        });
        loadThread.start();
    }

    private static void setupModernUI() {
        try {
            // Apply FlatLaf for a modern look
            FlatIntelliJLaf.setup();
            
            // Further font customizations if needed
            String fontName = System.getProperty("os.name").toLowerCase().contains("win") ? "Segoe UI" : "Arial";
            java.awt.Font modernFont = new java.awt.Font(fontName, java.awt.Font.PLAIN, 13);
            javax.swing.UIManager.put("defaultFont", modernFont);
            
        } catch (Exception ex) {
            log.error("Failed to initialize FlatLaf", ex);
        }
    }
}