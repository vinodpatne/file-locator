package filelocator.ui;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;

import filelocator.model.SearchCriteria;
import filelocator.model.UserPreferences;
import filelocator.repository.IndexRepository;
import filelocator.service.IndexingService;
import filelocator.service.SearchService;
import filelocator.ui.panel.ActionPanel;
import filelocator.ui.panel.CriteriaPanel;
import filelocator.ui.panel.ResultsTablePanel;
import filelocator.ui.panel.StatusBarPanel;
import filelocator.ui.worker.SearchWorker;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MainFrame extends JFrame {
    private static final Logger log = Logger.getLogger(MainFrame.class.getName());

    private final IndexRepository indexRepository;
    private final IndexingService indexingService;
    private final SearchService searchService;

    private final CriteriaPanel criteriaPanel;
    private final ResultsTablePanel resultsTablePanel;
    private final ActionPanel actionPanel;
    private final StatusBarPanel statusBarPanel;

    private SearchWorker currentSearchWorker;
    private boolean isClearing = false;

    public MainFrame(IndexRepository indexRepository, IndexingService indexingService, SearchService searchService) {
        super("File Locator");
        this.indexRepository = indexRepository;
        this.indexingService = indexingService;
        this.searchService = searchService;

        this.criteriaPanel = new CriteriaPanel();
        this.resultsTablePanel = new ResultsTablePanel();
        this.actionPanel = new ActionPanel();
        this.statusBarPanel = new StatusBarPanel();

        initUI();
        wireEvents();
        statusBarPanel.setStatus("Loading index... Please wait.");
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(criteriaPanel, BorderLayout.CENTER);
        topContainer.add(actionPanel, BorderLayout.EAST);

        add(topContainer, BorderLayout.NORTH);
        add(resultsTablePanel, BorderLayout.CENTER);
        add(statusBarPanel, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        // criteria triggers search
        criteriaPanel.addSearchListener(this::triggerSearch);

        // Action Panel buttons
        actionPanel.getClearBtn().addActionListener(e -> {
            isClearing = true;
            try {
                criteriaPanel.clearFields();
                if (currentSearchWorker != null && !currentSearchWorker.isDone()) {
                    currentSearchWorker.cancel(true);
                }
                resultsTablePanel.clear();
                statusBarPanel.setStatus("Status: Ready");
            } finally {
                isClearing = false;
            }
        });

        actionPanel.getReIndexBtn().addActionListener(e -> runIndexer());

        // Table selection changes open buttons in Status Bar
        resultsTablePanel.getTable().getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasRow = resultsTablePanel.getTable().getSelectedRow() != -1;
                statusBarPanel.setActionButtonsEnabled(hasRow);
            }
        });

        // Open Files buttons
        statusBarPanel.getOpenBtn().addActionListener(e -> resultsTablePanel.openSelected(true));
        statusBarPanel.getOpenLocBtn().addActionListener(e -> resultsTablePanel.openSelected(false));
        statusBarPanel.getDeleteBtn().addActionListener(e -> deleteSelectedFiles());

        resultsTablePanel.getTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && resultsTablePanel.getTable().getSelectedRow() != -1) {
                    resultsTablePanel.openSelected(true);
                }
            }
        });

        resultsTablePanel.getTable().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    deleteSelectedFiles();
                }
            }
        });
    }

    private void deleteSelectedFiles() {
        List<String> paths = resultsTablePanel.getSelectedFilePaths();
        if (paths.isEmpty()) return;

        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete " + paths.size() + " item(s)?\nThis action cannot be undone.",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            int deletedCount = 0;
            for (String path : paths) {
                File file = new File(path);
                if (deleteRecursively(file)) {
                    indexRepository.removeByPrefix(path);
                    deletedCount++;
                }
            }
            if (deletedCount > 0) {
                indexRepository.save();
                triggerSearch();
                JOptionPane.showMessageDialog(this, "Successfully deleted " + deletedCount + " item(s).",
                        "Deletion Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private boolean deleteRecursively(File file) {
        if (!file.exists()) return true;
        boolean success = true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    success &= deleteRecursively(child);
                }
            }
        }
        return success && file.delete();
    }

    public void onIndexLoaded() {
        statusBarPanel.setStatus("Index loaded: " + indexRepository.size() + " items.");
        triggerSearch();

        if (indexRepository.size() == 0) {
            int response = JOptionPane.showConfirmDialog(this,
                    "No database found. Would you like to scan 'This PC' (All Drives) now?",
                    "Initial Scan Required", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                runIndexer();
            }
        }
    }

    private void runIndexer() {
        String loc = criteriaPanel.getRawLocation();
        List<String> rootsToScan = new ArrayList<>();

        if (loc.equalsIgnoreCase("This PC")) {
            if (indexRepository.size() > 0) {
                int res = JOptionPane.showConfirmDialog(this,
                        "Re-index ALL drives? This may take time.", "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            for (File root : File.listRoots()) {
                rootsToScan.add(root.getAbsolutePath());
            }
        } else {
            rootsToScan.add(loc);
        }

        statusBarPanel.setStatus("Status: Scanning drives... (Please wait)");

        indexingService.buildIndex(rootsToScan, () -> {
            statusBarPanel.setStatus("Status: Index Complete. Total Items: " + indexRepository.size());
            triggerSearch();
        });
    }

    private void triggerSearch() {
        if (isClearing) {
            return;
        }

        SearchCriteria criteria = criteriaPanel.getCriteria();
        String loc = criteria.locationScope();
        if (loc != null && !loc.isBlank() && !"This PC".equalsIgnoreCase(loc)) {
            File locFile = new File(loc);
            if (locFile.exists() && locFile.isDirectory()) {
                UserPreferences prefs = UserPreferences.load();
                prefs.addRecentLocation(loc);
                criteriaPanel.addLocationIfNew(loc);
            }
        }

        if (currentSearchWorker != null && !currentSearchWorker.isDone()) {
            currentSearchWorker.cancel(true);
        }

        currentSearchWorker = new SearchWorker(searchService, criteria, results -> {
            if (results != null) {
                log.fine("Search completed cleanly, updating UI with " + results.size() + " results.");
                resultsTablePanel.updateResults(results);
                statusBarPanel.setStatus("Found " + results.size() + " matches.");
            } else {
                log.severe("Search results returned null.");
            }
        });
        currentSearchWorker.execute();
    }
}
