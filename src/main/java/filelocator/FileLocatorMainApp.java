package filelocator;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.plaf.ColorUIResource;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import filelocator.model.UserPreferences;
import filelocator.repository.InMemoryIndexRepository;
import filelocator.repository.IndexRepository;
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
            // Prevent FlatLaf from loading native libraries (DLLs) which can be blocked by
            // Endpoint Protection
            System.setProperty("flatlaf.useNativeLibrary", "false");

            // Apply saved theme preference
            UserPreferences prefs = UserPreferences.load();
            applyTheme(prefs.getTheme());

        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to initialize FlatLaf", ex);
        }
    }

    public static void applyTheme(String themeName) {
        try {
            // Custom Slate-and-Purple Palette / Light Palette using ColorUIResource for
            // runtime dynamic updates
            Color primaryBg;
            Color secondaryBg;
            Color accentColor;
            Color accentHover;
            Color textColor;
            Color whiteColor = new ColorUIResource(255, 255, 255);

            if ("Light".equalsIgnoreCase(themeName)) {
                primaryBg = new ColorUIResource(245, 245, 247); // Warm Light Grey
                secondaryBg = new ColorUIResource(255, 255, 255); // Clean White
                textColor = new ColorUIResource(28, 28, 30); // Dark Grey/Black
                accentColor = new ColorUIResource(29, 78, 216); // Refined Royal/Navy Blue (#1D4ED8)
                accentHover = new ColorUIResource(59, 130, 246); // Bright Accent Blue (#3B82F6)
            } else {
                primaryBg = new ColorUIResource(18, 18, 24); // Deep Slate background
                secondaryBg = new ColorUIResource(30, 30, 40); // Lighter panel background
                textColor = new ColorUIResource(243, 244, 246); // Off-white text
                accentColor = new ColorUIResource(124, 58, 237); // Vibrant Purple accent (#7C3AED)
                accentHover = new ColorUIResource(139, 92, 246); // Highlight Purple (#8B5CF6)
            }

            // Apply Look and Feel first, so that custom UIManager properties override its
            // defaults correctly
            if ("Light".equalsIgnoreCase(themeName)) {
                FlatLightLaf.setup();
            } else {
                FlatDarkLaf.setup();
            }

            // Global Font
            String fontName = System.getProperty("os.name").toLowerCase().contains("win") ? "Segoe UI" : "Arial";
            java.awt.Font modernFont = new java.awt.Font(fontName, java.awt.Font.PLAIN, 13);
            javax.swing.UIManager.put("defaultFont", modernFont);

            // Global Arc Roundness
            javax.swing.UIManager.put("Component.arc", 8);
            javax.swing.UIManager.put("Button.arc", 8);
            javax.swing.UIManager.put("TextComponent.arc", 8);

            // Theme Overrides
            javax.swing.UIManager.put("background", primaryBg);
            javax.swing.UIManager.put("Panel.background", primaryBg);
            javax.swing.UIManager.put("control", secondaryBg);
            javax.swing.UIManager.put("text", textColor);

            // Accent & Selections
            javax.swing.UIManager.put("AccentColor", accentColor);
            javax.swing.UIManager.put("Component.focusColor", accentColor);
            javax.swing.UIManager.put("List.selectionBackground", accentColor);
            javax.swing.UIManager.put("List.selectionForeground", whiteColor);
            javax.swing.UIManager.put("Table.selectionBackground", accentColor);
            javax.swing.UIManager.put("Table.selectionForeground", whiteColor);

            // Buttons
            javax.swing.UIManager.put("Button.background", secondaryBg);
            javax.swing.UIManager.put("Button.foreground", textColor);
            javax.swing.UIManager.put("Button.hoverBackground", accentColor);
            javax.swing.UIManager.put("Button.hoverForeground", whiteColor);
            javax.swing.UIManager.put("Button.focusedBorderColor", accentColor);

            // Inputs (TextField, ComboBox, Spinner)
            javax.swing.UIManager.put("TextField.background", secondaryBg);
            javax.swing.UIManager.put("TextField.foreground", textColor);
            javax.swing.UIManager.put("ComboBox.background", secondaryBg);
            javax.swing.UIManager.put("ComboBox.foreground", textColor);
            javax.swing.UIManager.put("Spinner.background", secondaryBg);
            javax.swing.UIManager.put("Spinner.foreground", textColor);

            // Table & Table Header
            javax.swing.UIManager.put("Table.background", secondaryBg);
            javax.swing.UIManager.put("Table.foreground", textColor);
            javax.swing.UIManager.put("Table.gridColor", primaryBg);
            javax.swing.UIManager.put("TableHeader.background", secondaryBg);
            javax.swing.UIManager.put("TableHeader.foreground", textColor);

            // Tabs
            javax.swing.UIManager.put("TabbedPane.background", primaryBg);
            javax.swing.UIManager.put("TabbedPane.tabAreaBackground", primaryBg);
            javax.swing.UIManager.put("TabbedPane.selectedForeground", whiteColor);
            javax.swing.UIManager.put("TabbedPane.selectedBackground", accentColor);
            javax.swing.UIManager.put("TabbedPane.focusColor", accentColor);
            javax.swing.UIManager.put("TabbedPane.underlineColor", accentColor);
            javax.swing.UIManager.put("TabbedPane.hoverColor", accentColor);
            javax.swing.UIManager.put("TabbedPane.hoverForeground", whiteColor);

            // Update UI on all open windows
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }

        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to apply theme: " + themeName, ex);
        }
    }
}