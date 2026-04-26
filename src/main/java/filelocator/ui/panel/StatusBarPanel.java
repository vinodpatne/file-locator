package filelocator.ui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import lombok.Getter;

@Getter
public class StatusBarPanel extends JPanel {
    private final JLabel statusLabel = new JLabel("Status: Ready");
    private final JButton openBtn = new JButton("Open File");
    private final JButton openLocBtn = new JButton("Open Location");
    private final JButton deleteBtn = new JButton("Delete");

    public StatusBarPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 15, 8, 15));

        add(statusLabel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        openBtn.setEnabled(false);
        openLocBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        actionPanel.add(openBtn);
        actionPanel.add(openLocBtn);
        actionPanel.add(deleteBtn);

        add(actionPanel, BorderLayout.EAST);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setActionButtonsEnabled(boolean enabled) {
        openBtn.setEnabled(enabled);
        openLocBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
    }
}
