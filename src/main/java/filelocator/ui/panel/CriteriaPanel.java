package filelocator.ui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;

import filelocator.FileLocatorMainApp;
import filelocator.model.SearchCriteria;
import filelocator.model.UserPreferences;

public class CriteriaPanel extends JPanel {
    // Tab 1: Name & Location
    private final JTextField searchField = new JTextField(20);
    private final JTextField extensionField = new JTextField(20);
    private final JComboBox<String> locationCombo = new JComboBox<>();
    private final JCheckBox subDirCheckBox = new JCheckBox("Search subdirectories", true);

    // Tab 2: Size & Date
    private final JCheckBox minSizeCheck = new JCheckBox("Minimum file size:");
    private final JTextField minSizeField = new JTextField("0", 6);
    private final JComboBox<String> minSizeUnit = new JComboBox<>(new String[] { "KB", "MB", "GB" });

    private final JCheckBox maxSizeCheck = new JCheckBox("Maximum file size:");
    private final JTextField maxSizeField = new JTextField("0", 6);
    private final JComboBox<String> maxSizeUnit = new JComboBox<>(new String[] { "KB", "MB", "GB" });

    private final JCheckBox minDateCheck = new JCheckBox("Files newer than:");
    private final JSpinner minDateSpinner = new JSpinner(new SpinnerDateModel());

    private final JCheckBox maxDateCheck = new JCheckBox("Files older than:");
    private final JSpinner maxDateSpinner = new JSpinner(new SpinnerDateModel());

    // Tab 3: Advanced
    private final JCheckBox foldersCheckBox = new JCheckBox("Include Folders in Results", false);
    private final JCheckBox regexCheckBox = new JCheckBox("Use Regular Expressions (Regex)", true);
    private final JCheckBox duplicatesCheckBox = new JCheckBox("Find Duplicates (Name & Size)", false);
    private final JComboBox<String> sortCombo = new JComboBox<>(
            new String[] { "Name", "Size", "Date Modified", "File Path" });
    private final JComboBox<String> sortDirCombo = new JComboBox<>(new String[] { "Ascending", "Descending" });
    private final JComboBox<String> themeCombo = new JComboBox<>(new String[] { "Dark", "Light" });

    private final UserPreferences userPrefs;
    private boolean isUpdatingDropdown = false;

    public CriteriaPanel() {
        userPrefs = UserPreferences.load();
        
        setLayout(new BorderLayout());
        
        locationCombo.setEditable(true);
        updateLocationsDropdown();
        themeCombo.setSelectedItem(userPrefs.getTheme());
        sortCombo.setSelectedItem("Date Modified");
        sortDirCombo.setSelectedItem("Descending");

        minDateSpinner.setEditor(new JSpinner.DateEditor(minDateSpinner, "yyyy-MM-dd"));
        maxDateSpinner.setEditor(new JSpinner.DateEditor(maxDateSpinner, "yyyy-MM-dd"));

        minSizeField.setEnabled(false);
        minSizeUnit.setEnabled(false);
        maxSizeField.setEnabled(false);
        maxSizeUnit.setEnabled(false);
        minDateSpinner.setEnabled(false);
        maxDateSpinner.setEnabled(false);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty("JTabbedPane.tabType", "card");
        tabbedPane.setBorder(new EmptyBorder(5, 10, 5, 10));

        tabbedPane.addTab("Name & Location", buildNameLocationTab());
        tabbedPane.addTab("Size and Date", buildSizeDateTab());
        tabbedPane.addTab("Advanced", buildAdvancedTab());

        add(tabbedPane, BorderLayout.CENTER);
        setupListeners();
    }

    private JPanel buildNameLocationTab() {
        JPanel tab1 = new JPanel(new GridBagLayout());
        tab1.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        tab1.add(new JLabel("Named:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        tab1.add(searchField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        tab1.add(new JLabel("Extensions:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        tab1.add(extensionField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        tab1.add(new JLabel("Look in:"), gbc);
        
        JPanel locPanel = new JPanel(new BorderLayout(5, 0));
        locPanel.add(locationCombo, BorderLayout.CENTER);
        JButton browseBtn = new JButton("Browse...");
        locPanel.add(browseBtn, BorderLayout.EAST);
        
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                locationCombo.setSelectedItem(fc.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.gridx = 1; gbc.weightx = 1.0;
        tab1.add(locPanel, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        tab1.add(subDirCheckBox, gbc);

        gbc.gridy = 4; gbc.weighty = 1.0;
        tab1.add(new JLabel(""), gbc);
        
        return tab1;
    }

    private JPanel buildSizeDateTab() {
        JPanel tab2 = new JPanel(new GridBagLayout());
        tab2.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 10);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        tab2.add(minSizeCheck, gbc);
        JPanel minP = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        minP.add(minSizeField); minP.add(minSizeUnit);
        gbc.gridx = 1;
        tab2.add(minP, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        tab2.add(maxSizeCheck, gbc);
        JPanel maxP = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        maxP.add(maxSizeField); maxP.add(maxSizeUnit);
        gbc.gridx = 1;
        tab2.add(maxP, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        tab2.add(minDateCheck, gbc);
        gbc.gridx = 1;
        tab2.add(minDateSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        tab2.add(maxDateCheck, gbc);
        gbc.gridx = 1;
        tab2.add(maxDateSpinner, gbc);

        gbc.gridy = 4; gbc.weighty = 1.0; gbc.weightx = 1.0;
        tab2.add(new JLabel(""), gbc);
        
        return tab2;
    }

    private JPanel buildAdvancedTab() {
        JPanel tab3 = new JPanel(new GridBagLayout());
        tab3.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();

        // Left Panel for checkboxes
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(5, 5, 5, 10);
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.anchor = GridBagConstraints.WEST;

        gbcLeft.gridx = 0; gbcLeft.gridy = 0;
        leftPanel.add(regexCheckBox, gbcLeft);
        gbcLeft.gridx = 0; gbcLeft.gridy = 1;
        leftPanel.add(foldersCheckBox, gbcLeft);
        gbcLeft.gridx = 0; gbcLeft.gridy = 2;
        leftPanel.add(duplicatesCheckBox, gbcLeft);

        gbcLeft.gridy = 3; gbcLeft.weighty = 1.0; gbcLeft.weightx = 1.0;
        leftPanel.add(new JLabel(""), gbcLeft);

        // Right Panel for combo boxes
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.anchor = GridBagConstraints.WEST;

        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        themePanel.setOpaque(false);
        themePanel.add(new JLabel("Color Theme:"));
        themePanel.add(themeCombo);
        gbcRight.gridx = 0; gbcRight.gridy = 0; gbcRight.insets = new Insets(5, 0, 5, 10);
        rightPanel.add(themePanel, gbcRight);

        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sortPanel.setOpaque(false);
        sortPanel.add(new JLabel("Sort Results By:"));
        sortPanel.add(sortCombo);
        sortPanel.add(sortDirCombo);
        gbcRight.gridx = 0; gbcRight.gridy = 1; gbcRight.insets = new Insets(15, 0, 5, 10);
        rightPanel.add(sortPanel, gbcRight);

        gbcRight.gridy = 2; gbcRight.weighty = 1.0; gbcRight.weightx = 1.0;
        rightPanel.add(new JLabel(""), gbcRight);

        // Add left and right panels to tab3
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 0, 0, 10);
        tab3.add(leftPanel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 10, 0, 0);
        tab3.add(rightPanel, gbc);

        return tab3;
    }

    private void setupListeners() {
        minSizeCheck.addActionListener(e -> {
            minSizeField.setEnabled(minSizeCheck.isSelected());
            minSizeUnit.setEnabled(minSizeCheck.isSelected());
            updateAutoSort();
        });
        maxSizeCheck.addActionListener(e -> {
            maxSizeField.setEnabled(maxSizeCheck.isSelected());
            maxSizeUnit.setEnabled(maxSizeCheck.isSelected());
            updateAutoSort();
        });
        minDateCheck.addActionListener(e -> {
            minDateSpinner.setEnabled(minDateCheck.isSelected());
            updateAutoSort();
        });
        maxDateCheck.addActionListener(e -> {
            maxDateSpinner.setEnabled(maxDateCheck.isSelected());
            updateAutoSort();
        });
    }

    private void updateAutoSort() {
        boolean sizeApplied = minSizeCheck.isSelected() || maxSizeCheck.isSelected();
        boolean dateApplied = minDateCheck.isSelected() || maxDateCheck.isSelected();

        if (sizeApplied) {
            sortCombo.setSelectedItem("Size");
            sortDirCombo.setSelectedItem("Descending");
        } else if (dateApplied) {
            sortCombo.setSelectedItem("Date Modified");
            sortDirCombo.setSelectedItem("Descending");
        } else {
            sortCombo.setSelectedItem("Date Modified");
            sortDirCombo.setSelectedItem("Descending");
        }
    }

    public void addSearchListener(Runnable onSearch) {
        DocumentListener searchTrig = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { if (!isUpdatingDropdown) onSearch.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { if (!isUpdatingDropdown) onSearch.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { if (!isUpdatingDropdown) onSearch.run(); }
        };

        searchField.getDocument().addDocumentListener(searchTrig);
        extensionField.getDocument().addDocumentListener(searchTrig);
        minSizeField.getDocument().addDocumentListener(searchTrig);
        maxSizeField.getDocument().addDocumentListener(searchTrig);

        java.awt.Component editor = locationCombo.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            ((JTextField) editor).getDocument().addDocumentListener(searchTrig);
        }

        locationCombo.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                Object selectedObj = locationCombo.getSelectedItem();
                if (selectedObj != null) {
                    String selected = selectedObj.toString();
                    if (!selected.equals(userPrefs.getDefaultLocation())) {
                        userPrefs.setDefaultLocation(selected);
                        userPrefs.save();
                    }
                }
                onSearch.run();
            }
        });
        locationCombo.addActionListener(e -> {
            Object selectedObj = locationCombo.getSelectedItem();
            if (selectedObj != null) {
                String selected = selectedObj.toString();
                if (!selected.equals(userPrefs.getDefaultLocation())) {
                    userPrefs.setDefaultLocation(selected);
                    userPrefs.save();
                }
            }
            onSearch.run();
        });
        subDirCheckBox.addActionListener(e -> onSearch.run());
        regexCheckBox.addActionListener(e -> onSearch.run());
        foldersCheckBox.addActionListener(e -> onSearch.run());
        duplicatesCheckBox.addActionListener(e -> onSearch.run());
        themeCombo.addActionListener(e -> {
            String selectedTheme = (String) themeCombo.getSelectedItem();
            if (selectedTheme != null && !selectedTheme.equals(userPrefs.getTheme())) {
                userPrefs.setTheme(selectedTheme);
                userPrefs.save();
                FileLocatorMainApp.applyTheme(selectedTheme);
            }
        });
        sortCombo.addActionListener(e -> onSearch.run());
        sortDirCombo.addActionListener(e -> onSearch.run());
        minSizeUnit.addActionListener(e -> onSearch.run());
        maxSizeUnit.addActionListener(e -> onSearch.run());
        minDateSpinner.addChangeListener(e -> { if (minDateCheck.isSelected()) onSearch.run(); });
        maxDateSpinner.addChangeListener(e -> { if (maxDateCheck.isSelected()) onSearch.run(); });
        
        // Listeners for checkbox toggles simulating change events
        minSizeCheck.addActionListener(e -> onSearch.run());
        maxSizeCheck.addActionListener(e -> onSearch.run());
        minDateCheck.addActionListener(e -> onSearch.run());
        maxDateCheck.addActionListener(e -> onSearch.run());
    }

    public void clearFields() {
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
        regexCheckBox.setSelected(true);
        foldersCheckBox.setSelected(false);
        duplicatesCheckBox.setSelected(false);
        subDirCheckBox.setSelected(true);
        locationCombo.setSelectedItem(userPrefs.getDefaultLocation());
        sortCombo.setSelectedItem("Date Modified");
        sortDirCombo.setSelectedItem("Descending");
    }

    public SearchCriteria getCriteria() {
        long minSize = minSizeCheck.isSelected() ? parseSizeSafely(minSizeField.getText(), (String) minSizeUnit.getSelectedItem()) : 0;
        long maxSize = maxSizeCheck.isSelected() ? parseSizeSafely(maxSizeField.getText(), (String) maxSizeUnit.getSelectedItem()) : 0;

        long minDate = minDateCheck.isSelected() ? getStartOfDay((Date) minDateSpinner.getValue()) : 0;
        long maxDate = maxDateCheck.isSelected() ? getEndOfDay((Date) maxDateSpinner.getValue()) : 0;

        Object selectedLoc = locationCombo.getSelectedItem();
        String loc = selectedLoc != null ? selectedLoc.toString() : "";
        if ("This PC".equalsIgnoreCase(loc)) {
            loc = ""; // Global scope
        }

        return new SearchCriteria(
                searchField.getText(),
                extensionField.getText(),
                loc,
                subDirCheckBox.isSelected(),
                regexCheckBox.isSelected(),
                foldersCheckBox.isSelected(),
                minSize,
                maxSize,
                minDate,
                maxDate,
                (String) sortCombo.getSelectedItem(),
                sortDirCombo.getSelectedIndex() == 0,
                duplicatesCheckBox.isSelected()
        );
    }
    
    public String getRawLocation() {
        Object selectedLoc = locationCombo.getSelectedItem();
        return selectedLoc != null ? selectedLoc.toString() : "This PC";
    }

    private long parseSizeSafely(String text, String unit) {
        if (text.isBlank()) return 0;
        try {
            long val = Long.parseLong(text.trim());
            if ("MB".equals(unit)) return val * 1024 * 1024;
            if ("GB".equals(unit)) return val * 1024 * 1024 * 1024;
            return val * 1024;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long getStartOfDay(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDay(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        return cal.getTimeInMillis();
    }

    public void updateLocationsDropdown() {
        isUpdatingDropdown = true;
        java.awt.event.ActionListener[] listeners = locationCombo.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) {
            locationCombo.removeActionListener(l);
        }
        try {
            Object selected = locationCombo.getSelectedItem();
            locationCombo.removeAllItems();

            locationCombo.addItem("This PC");
            for (File root : File.listRoots()) {
                locationCombo.addItem(root.getAbsolutePath());
            }

            UserPreferences prefs = UserPreferences.load();
            for (String path : prefs.getRecentLocations()) {
                boolean exists = false;
                for (int i = 0; i < locationCombo.getItemCount(); i++) {
                    if (path.equalsIgnoreCase(locationCombo.getItemAt(i))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    locationCombo.addItem(path);
                }
            }

            if (selected != null) {
                locationCombo.setSelectedItem(selected);
            } else {
                locationCombo.setSelectedItem(prefs.getDefaultLocation());
            }
        } finally {
            for (java.awt.event.ActionListener l : listeners) {
                locationCombo.addActionListener(l);
            }
            isUpdatingDropdown = false;
        }
    }

    public void addLocationIfNew(String path) {
        if (path == null || path.isBlank() || "This PC".equalsIgnoreCase(path)) {
            return;
        }
        try {
            path = new File(path).getAbsolutePath();
        } catch (Exception e) {
            // Use fallback
        }
        
        boolean exists = false;
        for (int i = 0; i < locationCombo.getItemCount(); i++) {
            if (path.equalsIgnoreCase(locationCombo.getItemAt(i))) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            isUpdatingDropdown = true;
            try {
                locationCombo.addItem(path);
            } finally {
                isUpdatingDropdown = false;
            }
        }
    }
}
