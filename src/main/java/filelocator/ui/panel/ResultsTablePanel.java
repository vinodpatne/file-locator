package filelocator.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.Desktop;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import filelocator.model.FileEntry;

@Slf4j
@Getter
public class ResultsTablePanel extends JPanel {
    private static final String[] COLUMNS = { "Name", "Path", "Size", "Date Modified" };
    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final DefaultTableModel model;
    private final JTable table;

    public ResultsTablePanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
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

        add(scrollPane, BorderLayout.CENTER);

        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setPreferredWidth(450);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
    }

    public void clear() {
        model.setRowCount(0);
    }

    public void updateResults(List<FileEntry> results) {
        clear();
        for (FileEntry e : results) {
            String sizeStr = e.isDirectory() ? "<DIR>" : formatSize(e.size());
            String dateStr = displayFormat.format(new Date(e.lastModified()));
            model.addRow(new Object[] { e.name(), e.path(), sizeStr, dateStr });
        }
    }

    private String formatSize(long bytes) {
        if (bytes > 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)) + " GB";
        if (bytes > 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / 1024) + " KB";
    }

    public void openSelected(boolean openFile) {
        int row = table.getSelectedRow();
        if (row == -1) return;
        
        String path = (String) model.getValueAt(row, 1);
        File file = new File(path);
        
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            if (openFile) {
                Desktop.getDesktop().open(file);
            } else {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                } else {
                    Desktop.getDesktop().open(file.getParentFile());
                }
            }
        } catch (IOException ex) {
            log.error("Error opening file: {}", file.getAbsolutePath(), ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
