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
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
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

    // Tab 1: Name & Location
    private static final JTextField searchField = new JTextField(20);
    private static final JTextField extensionField = new JTextField(20);
    private static final JComboBox<String> locationCombo = new JComboBox<>();
    private static final JCheckBox subDirCheckBox = new JCheckBox("Search subdirectories", true);

    // Tab 2: Size & Date
    private static final JCheckBox minSizeCheck = new JCheckBox("Minimum file size:");
    private static final JTextField minSizeField = new JTextField("0", 6);
    private static final JComboBox<String> minSizeUnit = new JComboBox<>(new String[] { "KB", "MB", "GB" });

    private static final JCheckBox maxSizeCheck = new JCheckBox("Maximum file size:");
    private static final JTextField maxSizeField = new JTextField("0", 6);
    private static final JComboBox<String> maxSizeUnit = new JComboBox<>(new String[] { "KB", "MB", "GB" });

    private static final JCheckBox minDateCheck = new JCheckBox("Files newer than:");
    private static final JSpinner minDateSpinner = new JSpinner(new SpinnerDateModel());

    private static final JCheckBox maxDateCheck = new JCheckBox("Files older than:");
    private static final JSpinner maxDateSpinner = new JSpinner(new SpinnerDateModel());

    // Tab 3: Advanced
    private static final JCheckBox foldersCheckBox = new JCheckBox("Include Folders in Results", false);
    private static final JCheckBox regexCheckBox = new JCheckBox("Use Regular Expressions (Regex)", false);
    private static final JComboBox<String> sortCombo = new JComboBox<>(
            new String[] { "Name", "Size", "Date Modified" });
    private static final JComboBox<String> sortDirCombo = new JComboBox<>(new String[] { "Ascending", "Descending" });

    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        setupModernUI();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FileHound Search");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 750);
            frame.setLocationRelativeTo(null);

            // --- Configure Data Models & Spinners ---
            locationCombo.setEditable(true);
            locationCombo.addItem("This PC");
            for (File root : File.listRoots())
                locationCombo.addItem(root.getAbsolutePath());

            minDateSpinner.setEditor(new JSpinner.DateEditor(minDateSpinner, "yyyy-MM-dd"));
            maxDateSpinner.setEditor(new JSpinner.DateEditor(maxDateSpinner, "yyyy-MM-dd"));

            // Disable size/date inputs by default
            minSizeField.setEnabled(false);
            minSizeUnit.setEnabled(false);
            maxSizeField.setEnabled(false);
            maxSizeUnit.setEnabled(false);
            minDateSpinner.setEnabled(false);
            maxDateSpinner.setEnabled(false);

            // --- BUILD TABS ---
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setBorder(new EmptyBorder(5, 10, 5, 10));

            // 1. Name & Location Tab
            JPanel tab1 = new JPanel(new GridBagLayout());
            tab1.setBorder(new EmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 15);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            tab1.add(new JLabel("Named:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            tab1.add(searchField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            tab1.add(new JLabel("Extensions:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            tab1.add(extensionField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            tab1.add(new JLabel("Look in:"), gbc);
            JPanel locPanel = new JPanel(new BorderLayout(5, 0));
            locPanel.add(locationCombo, BorderLayout.CENTER);
            JButton browseBtn = new JButton("Browse...");
            locPanel.add(browseBtn, BorderLayout.EAST);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            tab1.add(locPanel, gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            tab1.add(subDirCheckBox, gbc);

            // Push everything to the top
            gbc.gridy = 4;
            gbc.weighty = 1.0;
            tab1.add(new JLabel(""), gbc);
            tabbedPane.addTab("Name & Location", tab1);

            // 2. Size and Date Tab
            JPanel tab2 = new JPanel(new GridBagLayout());
            tab2.setBorder(new EmptyBorder(15, 15, 15, 15));
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 10);
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            tab2.add(minSizeCheck, gbc);
            JPanel minP = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            minP.add(minSizeField);
            minP.add(minSizeUnit);
            gbc.gridx = 1;
            tab2.add(minP, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            tab2.add(maxSizeCheck, gbc);
            JPanel maxP = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            maxP.add(maxSizeField);
            maxP.add(maxSizeUnit);
            gbc.gridx = 1;
            tab2.add(maxP, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            tab2.add(minDateCheck, gbc);
            gbc.gridx = 1;
            tab2.add(minDateSpinner, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            tab2.add(maxDateCheck, gbc);
            gbc.gridx = 1;
            tab2.add(maxDateSpinner, gbc);

            gbc.gridy = 4;
            gbc.weighty = 1.0;
            gbc.weightx = 1.0;
            tab2.add(new JLabel(""), gbc);
            tabbedPane.addTab("Size and Date", tab2);

            // 3. Advanced Tab
            JPanel tab3 = new JPanel(new GridBagLayout());
            tab3.setBorder(new EmptyBorder(15, 15, 15, 15));
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            tab3.add(foldersCheckBox, gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            tab3.add(regexCheckBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.insets = new Insets(15, 5, 5, 10);
            tab3.add(new JLabel("Sort Results By:"), gbc);

            JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            sortPanel.add(sortCombo);
            sortPanel.add(sortDirCombo);
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.insets = new Insets(0, 5, 5, 10);
            tab3.add(sortPanel, gbc);

            gbc.gridy = 4;
            gbc.weighty = 1.0;
            gbc.weightx = 1.0;
            tab3.add(new JLabel(""), gbc);
            tabbedPane.addTab("Advanced", tab3);

            // --- RIGHT SIDE PANEL (Action Buttons) ---
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.setBorder(new EmptyBorder(25, 5, 10, 15));

            JButton reIndexBtn = new JButton("Update Index");
            JButton clearBtn = new JButton("Clear Search");

            Dimension btnSize = new Dimension(120, 30);
            reIndexBtn.setMaximumSize(btnSize);
            clearBtn.setMaximumSize(btnSize);

            rightPanel.add(reIndexBtn);
            rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            rightPanel.add(clearBtn);

            // Assemble Top Section
            JPanel topContainer = new JPanel(new BorderLayout());
            topContainer.add(tabbedPane, BorderLayout.CENTER);
            topContainer.add(rightPanel, BorderLayout.EAST);

            // --- TABLE & BOTTOM PANEL ---
            table.setRowHeight(26);
            table.setShowVerticalLines(false);
            table.setGridColor(new Color(230, 230, 230));
            table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));
            table.getTableHeader().setBackground(new Color(245, 245, 245));
            table.getTableHeader().setPreferredSize(new Dimension(0, 30));

            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
            rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
            table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(200, 200, 200)));

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(new EmptyBorder(8, 15, 8, 15));
            bottomPanel.add(statusLabel, BorderLayout.WEST);

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            JButton openBtn = new JButton("Open File");
            JButton openLocBtn = new JButton("Open Location");
            openBtn.setEnabled(false);
            openLocBtn.setEnabled(false);
            actionPanel.add(openBtn);
            actionPanel.add(openLocBtn);
            bottomPanel.add(actionPanel, BorderLayout.EAST);

            frame.add(topContainer, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            table.getColumnModel().getColumn(0).setPreferredWidth(250);
            table.getColumnModel().getColumn(1).setPreferredWidth(450);
            table.getColumnModel().getColumn(2).setPreferredWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(120);

            // --- LISTENERS ---

            // Checkbox Enablers
            minSizeCheck.addActionListener(e -> {
                minSizeField.setEnabled(minSizeCheck.isSelected());
                minSizeUnit.setEnabled(minSizeCheck.isSelected());
                triggerSearch();
            });
            maxSizeCheck.addActionListener(e -> {
                maxSizeField.setEnabled(maxSizeCheck.isSelected());
                maxSizeUnit.setEnabled(maxSizeCheck.isSelected());
                triggerSearch();
            });
            minDateCheck.addActionListener(e -> {
                minDateSpinner.setEnabled(minDateCheck.isSelected());
                triggerSearch();
            });
            maxDateCheck.addActionListener(e -> {
                maxDateSpinner.setEnabled(maxDateCheck.isSelected());
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
                if (minDateCheck.isSelected())
                    triggerSearch();
            });
            maxDateSpinner.addChangeListener(e -> {
                if (maxDateCheck.isSelected())
                    triggerSearch();
            });

            browseBtn.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    locationCombo.setSelectedItem(fc.getSelectedFile().getAbsolutePath());
                }
            });

            // Clear Button Logic
            clearBtn.addActionListener(e -> {
                searchField.setText("");
                extensionField.setText("");
                minSizeCheck.setSelected(false);
                minSizeField.setEnabled(false);
                minSizeUnit.setEnabled(false);
                maxSizeCheck.setSelected(false);
                maxSizeField.setEnabled(false);
                maxSizeUnit.setEnabled(false);
                minDateCheck.setSelected(false);
                minDateSpinner.setEnabled(false);
                maxDateCheck.setSelected(false);
                maxDateSpinner.setEnabled(false);
                model.setRowCount(0);
                statusLabel.setText("Status: Ready");
            });

            reIndexBtn.addActionListener(e -> runIndexer(frame));

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

            // Startup Check
            new Thread(() -> {
                FileSearcher.loadIndex();
                SwingUtilities.invokeLater(() -> {
                    if (FileSearcher.getIndexSize() == 0) {
                        int response = JOptionPane.showConfirmDialog(frame,
                                "No database found. Would you like to scan 'This PC' (All Drives) now?",
                                "Initial Scan Required", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
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
            Font modernFont = new Font(fontName, Font.PLAIN, 12);
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

    private static void runIndexer(JFrame frame) {
        Object selected = locationCombo.getSelectedItem();
        String loc = selected != null ? selected.toString() : "This PC";
        List<String> rootsToScan = new ArrayList<>();

        if (loc.equalsIgnoreCase("This PC")) {
            if (FileSearcher.getIndexSize() > 0) {
                int res = JOptionPane.showConfirmDialog(frame,
                        "Re-index ALL drives? This may take time.", "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION)
                    return;
            }
            for (File root : File.listRoots())
                rootsToScan.add(root.getAbsolutePath());
        } else {
            rootsToScan.add(loc);
        }

        statusLabel.setText("Status: Scanning drives... (Please wait)");
        FileIndexer.runIncrementalUpdate(rootsToScan, () -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Status: Index Complete. Total Items: " + FileSearcher.getIndexSize());
            });
        });
    }

    private static void triggerSearch() {
        String name = searchField.getText();
        if (name.length() < 1 && extensionField.getText().length() < 1)
            return;

        long minSize = minSizeCheck.isSelected()
                ? parseSizeSafely(minSizeField.getText(), (String) minSizeUnit.getSelectedItem())
                : 0;
        long maxSize = maxSizeCheck.isSelected()
                ? parseSizeSafely(maxSizeField.getText(), (String) maxSizeUnit.getSelectedItem())
                : 0;

        final long minDate = minDateCheck.isSelected() ? getStartOfDay((Date) minDateSpinner.getValue()) : 0;
        final long maxDate = maxDateCheck.isSelected() ? getEndOfDay((Date) maxDateSpinner.getValue()) : 0;

        boolean subDir = subDirCheckBox.isSelected();
        boolean regex = regexCheckBox.isSelected();
        boolean incFolders = foldersCheckBox.isSelected();
        String sort = (String) sortCombo.getSelectedItem();
        boolean asc = sortDirCombo.getSelectedIndex() == 0;
        String ext = extensionField.getText();

        Object selectedLoc = locationCombo.getSelectedItem();
        String loc = selectedLoc != null ? selectedLoc.toString() : "";
        if (loc.equalsIgnoreCase("This PC"))
            loc = "";

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