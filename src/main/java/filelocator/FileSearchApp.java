package filelocator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class FileSearchApp {
    // --- UI Components ---
    private static final String[] COLUMNS = { "Name", "Path", "Size", "Date Modified" };
    private static final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final JTable table = new JTable(model);
    private static final JLabel statusLabel = new JLabel("Status: Ready");

    // Search Fields
    private static final JTextField searchField = new JTextField(15);
    private static final JTextField extensionField = new JTextField(4);

    // NEW: Combo Box for Location (Replaces locationField)
    private static final JComboBox<String> locationCombo = new JComboBox<>();

    // Size & Date Fields
    private static final JTextField minSizeField = new JTextField(4);
    private static final JComboBox<String> minSizeUnit = new JComboBox<>(new String[] { "KB", "MB", "GB" });
    private static final JTextField maxSizeField = new JTextField(4);
    private static final JComboBox<String> maxSizeUnit = new JComboBox<>(new String[] { "KB", "MB", "GB" });
    private static final JCheckBox useDateFilter = new JCheckBox("Filter Dates:", false);
    private static final JSpinner minDateSpinner = new JSpinner(new SpinnerDateModel());
    private static final JSpinner maxDateSpinner = new JSpinner(new SpinnerDateModel());

    // Checkboxes & Combos
    private static final JCheckBox subDirCheckBox = new JCheckBox("Recursive", true);
    private static final JCheckBox regexCheckBox = new JCheckBox("Regex", false);
    private static final JCheckBox foldersCheckBox = new JCheckBox("Find Folders", false);
    private static final JComboBox<String> sortCombo = new JComboBox<>(
            new String[] { "Name", "Size", "Date Modified" });
    private static final JComboBox<String> sortDirCombo = new JComboBox<>(new String[] { "Ascending", "Descending" });

    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        setupModernUI();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FileHound Advanced Search");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);

            // --- Configure Location Combo ---
            locationCombo.setEditable(true);
            locationCombo.addItem("This PC"); // Add universal option
            for (File root : File.listRoots()) {
                locationCombo.addItem(root.getAbsolutePath()); // Add C:\, D:\, etc.
            }
            // Ensure combo looks the same width as the old text field
            locationCombo.setPreferredSize(new Dimension(150, 25));

            // --- Table & Spinners ---
            table.setRowHeight(28);
            table.setShowVerticalLines(false);
            table.setGridColor(new Color(230, 230, 230));
            table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));
            table.getTableHeader().setBackground(new Color(245, 245, 245));
            table.getTableHeader().setPreferredSize(new Dimension(0, 35));

            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
            rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
            table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

            minDateSpinner.setEditor(new JSpinner.DateEditor(minDateSpinner, "yyyy-MM-dd"));
            maxDateSpinner.setEditor(new JSpinner.DateEditor(maxDateSpinner, "yyyy-MM-dd"));
            minDateSpinner.setEnabled(false);
            maxDateSpinner.setEnabled(false);

            // --- TOP PANEL ---
            JPanel topPanel = new JPanel(new GridBagLayout());
            topPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Row 1
            gbc.gridy = 0;
            gbc.gridx = 0;
            topPanel.add(new JLabel("File Name:"), gbc);
            gbc.gridx = 1;
            topPanel.add(searchField, gbc);
            gbc.gridx = 2;
            topPanel.add(new JLabel("Ext:"), gbc);
            gbc.gridx = 3;
            topPanel.add(extensionField, gbc);
            gbc.gridx = 4;
            topPanel.add(new JLabel("Look In:"), gbc);

            JPanel locPanel = new JPanel(new BorderLayout(5, 0));
            locPanel.add(locationCombo, BorderLayout.CENTER);
            JButton browseBtn = new JButton("Browse...");
            locPanel.add(browseBtn, BorderLayout.EAST);
            gbc.gridx = 5;
            gbc.weightx = 1.0;
            topPanel.add(locPanel, gbc);

            // Row 2
            gbc.gridy = 1;
            gbc.gridx = 0;
            gbc.weightx = 0;
            topPanel.add(new JLabel("Size Min:"), gbc);
            JPanel minSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            minSizePanel.add(minSizeField);
            minSizePanel.add(minSizeUnit);
            gbc.gridx = 1;
            topPanel.add(minSizePanel, gbc);

            gbc.gridx = 2;
            topPanel.add(new JLabel("Max:"), gbc);
            JPanel maxSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            maxSizePanel.add(maxSizeField);
            maxSizePanel.add(maxSizeUnit);
            gbc.gridx = 3;
            topPanel.add(maxSizePanel, gbc);

            gbc.gridx = 4;
            gbc.gridwidth = 2;
            JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            datePanel.add(useDateFilter);
            datePanel.add(minDateSpinner);
            datePanel.add(new JLabel(" To "));
            datePanel.add(maxDateSpinner);
            topPanel.add(datePanel, gbc);

            // Row 3
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.gridwidth = 6;
            JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            optionsPanel.add(subDirCheckBox);
            optionsPanel.add(regexCheckBox);
            optionsPanel.add(foldersCheckBox);
            optionsPanel.add(new JSeparator(SwingConstants.VERTICAL));
            optionsPanel.add(new JLabel("Sort By:"));
            optionsPanel.add(sortCombo);
            optionsPanel.add(sortDirCombo);

            JButton reIndexBtn = new JButton("Update Index");
            reIndexBtn.setBackground(new Color(220, 235, 255));
            reIndexBtn.addActionListener(e -> runIndexer(frame));
            optionsPanel.add(Box.createHorizontalStrut(20));
            optionsPanel.add(reIndexBtn);

            topPanel.add(optionsPanel, gbc);

            // --- CENTER & BOTTOM PANELS ---
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(200, 200, 200)));

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
            bottomPanel.add(statusLabel, BorderLayout.WEST);

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            JButton openBtn = new JButton("Open File");
            JButton openLocBtn = new JButton("Open Location");
            openBtn.setEnabled(false);
            openLocBtn.setEnabled(false);
            openBtn.setPreferredSize(new Dimension(100, 30));
            openLocBtn.setPreferredSize(new Dimension(130, 30));

            actionPanel.add(openBtn);
            actionPanel.add(openLocBtn);
            bottomPanel.add(actionPanel, BorderLayout.EAST);

            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            table.getColumnModel().getColumn(0).setPreferredWidth(250);
            table.getColumnModel().getColumn(1).setPreferredWidth(450);
            table.getColumnModel().getColumn(2).setPreferredWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(120);

            // --- LISTENERS ---
            useDateFilter.addActionListener(e -> {
                boolean en = useDateFilter.isSelected();
                minDateSpinner.setEnabled(en);
                maxDateSpinner.setEnabled(en);
                triggerSearch();
            });

            DocumentListener searchTrig = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    triggerSearch();
                }

                public void removeUpdate(DocumentEvent e) {
                    triggerSearch();
                }

                public void changedUpdate(DocumentEvent e) {
                    triggerSearch();
                }
            };

            searchField.getDocument().addDocumentListener(searchTrig);
            extensionField.getDocument().addDocumentListener(searchTrig);
            minSizeField.getDocument().addDocumentListener(searchTrig);
            maxSizeField.getDocument().addDocumentListener(searchTrig);

            // Listen to Combo Box text changes
            Component editor = locationCombo.getEditor().getEditorComponent();
            if (editor instanceof JTextField) {
                ((JTextField) editor).getDocument().addDocumentListener(searchTrig);
            }
            locationCombo.addActionListener(e -> triggerSearch());

            subDirCheckBox.addActionListener(e -> triggerSearch());
            regexCheckBox.addActionListener(e -> triggerSearch());
            foldersCheckBox.addActionListener(e -> triggerSearch());
            sortCombo.addActionListener(e -> triggerSearch());
            sortDirCombo.addActionListener(e -> triggerSearch());
            minSizeUnit.addActionListener(e -> triggerSearch());
            maxSizeUnit.addActionListener(e -> triggerSearch());
            minDateSpinner.addChangeListener(e -> {
                if (useDateFilter.isSelected())
                    triggerSearch();
            });
            maxDateSpinner.addChangeListener(e -> {
                if (useDateFilter.isSelected())
                    triggerSearch();
            });

            browseBtn.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    locationCombo.setSelectedItem(fc.getSelectedFile().getAbsolutePath());
                }
            });

            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    boolean hasRow = table.getSelectedRow() != -1;
                    openBtn.setEnabled(hasRow);
                    openLocBtn.setEnabled(hasRow);
                }
            });

            ActionListener openAction = e -> openSelected(e.getSource() == openBtn);
            openBtn.addActionListener(openAction);
            openLocBtn.addActionListener(openAction);
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() != -1)
                        openSelected(true);
                }
            });

            // --- STARTUP LOGIC: Load Index & Prompt Initial Scan ---
            new Thread(() -> {
                FileSearcher.loadIndex();
                SwingUtilities.invokeLater(() -> {
                    if (FileSearcher.getIndexSize() == 0) {
                        int response = JOptionPane.showConfirmDialog(frame,
                                "No database found. Would you like to scan 'This PC' (All Drives) now?\nThis may take several minutes depending on your storage size.",
                                "Initial Scan Required",
                                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                        if (response == JOptionPane.YES_OPTION) {
                            locationCombo.setSelectedItem("This PC");
                            runIndexer(frame);
                        }
                    } else {
                        statusLabel.setText("Index loaded: " + FileSearcher.getIndexSize() + " items.");
                    }
                });
            }).start();

            frame.setVisible(true);
        });
    }

    private static void setupModernUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String fontName = System.getProperty("os.name").toLowerCase().contains("win") ? "Segoe UI" : "Arial";
            Font modernFont = new Font(fontName, Font.PLAIN, 13);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(modernFont));
                }
            }
        } catch (Exception e) {
        }
    }

    // --- LOGIC: Run Indexer ---
    private static void runIndexer(JFrame frame) {
        Object selected = locationCombo.getSelectedItem();
        String loc = selected != null ? selected.toString() : "This PC";

        List<String> rootsToScan = new ArrayList<>();

        // Handle "This PC" Logic
        if (loc.equalsIgnoreCase("This PC")) {
            // Confirm with user if they are voluntarily clicking the Update button
            if (FileSearcher.getIndexSize() > 0) {
                int res = JOptionPane.showConfirmDialog(frame,
                        "You are about to re-index ALL drives on this PC.\nThis can take a significant amount of time.\nDo you want to proceed?",
                        "Confirm Full Scan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION)
                    return;
            }

            for (File root : File.listRoots()) {
                rootsToScan.add(root.getAbsolutePath());
            }
        } else {
            rootsToScan.add(loc);
        }

        statusLabel.setText("Status: Scanning drives... (Please wait)");
        FileIndexer.runIncrementalUpdate(rootsToScan, () -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Status: Index Complete. Total Items: " + FileSearcher.getIndexSize());
                JOptionPane.showMessageDialog(frame,
                        "Indexing Complete!\nAdded/Updated " + rootsToScan.size() + " location(s).");
            });
        });
    }

    // --- LOGIC: Trigger Search ---
    private static void triggerSearch() {
        String name = searchField.getText();
        if (name.length() < 1 && extensionField.getText().length() < 1)
            return;

        long minSize = parseSizeSafely(minSizeField.getText(), (String) minSizeUnit.getSelectedItem());
        long maxSize = parseSizeSafely(maxSizeField.getText(), (String) maxSizeUnit.getSelectedItem());

        // Ensure variables are effectively final for the background thread lambda
        final long minDate = useDateFilter.isSelected() ? getStartOfDay((Date) minDateSpinner.getValue()) : 0;
        final long maxDate = useDateFilter.isSelected() ? getEndOfDay((Date) maxDateSpinner.getValue()) : 0;

        boolean subDir = subDirCheckBox.isSelected();
        boolean regex = regexCheckBox.isSelected();
        boolean incFolders = foldersCheckBox.isSelected();
        String sort = (String) sortCombo.getSelectedItem();
        boolean asc = sortDirCombo.getSelectedIndex() == 0;
        String ext = extensionField.getText();

        // Handle "This PC" in Search Scope (Treat as empty scope to search everywhere)
        Object selectedLoc = locationCombo.getSelectedItem();
        String loc = selectedLoc != null ? selectedLoc.toString() : "";
        if (loc.equalsIgnoreCase("This PC")) {
            loc = "";
        }

        final String finalLoc = loc;
        new Thread(() -> {
            var results = FileSearcher.search(
                    name, ext, finalLoc, subDir, regex, incFolders, minSize, maxSize, minDate, maxDate, sort, asc);

            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                for (FileSearcher.FileEntry e : results) {
                    String sizeStr = e.isDirectory() ? "<DIR>" : formatSize(e.size());
                    String dateStr = displayFormat.format(new Date(e.lastModified()));
                    model.addRow(new Object[] { e.name(), e.path(), sizeStr, dateStr });
                }
                statusLabel.setText("Found " + results.size() + " matches.");
            });
        }).start();
    }

    private static long parseSizeSafely(String text, String unit) {
        if (text.isBlank())
            return 0;
        try {
            long val = Long.parseLong(text.trim());
            if ("MB".equals(unit))
                return val * 1024 * 1024;
            if ("GB".equals(unit))
                return val * 1024 * 1024 * 1024;
            return val * 1024;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatSize(long bytes) {
        if (bytes > 1024 * 1024 * 1024)
            return (bytes / (1024 * 1024 * 1024)) + " GB";
        if (bytes > 1024 * 1024)
            return (bytes / (1024 * 1024)) + " MB";
        return (bytes / 1024) + " KB";
    }

    private static long getStartOfDay(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    private static long getEndOfDay(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        return cal.getTimeInMillis();
    }

    private static void openSelected(boolean openFile) {
        int row = table.getSelectedRow();
        if (row == -1)
            return;
        String path = (String) model.getValueAt(row, 1);
        File file = new File(path);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(null, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (openFile)
                Desktop.getDesktop().open(file);
            else {
                if (System.getProperty("os.name").toLowerCase().contains("win"))
                    new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                else
                    Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }
}