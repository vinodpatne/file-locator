package filelocator.ui.panel;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import lombok.Getter;

@Getter
public class ActionPanel extends JPanel {
    private final JButton searchBtn = new JButton("Search");
    private final JButton clearBtn = new JButton("Clear Search");
    private final JButton reIndexBtn = new JButton("Update Index");
    private int currentTopPadding = 25;

    public ActionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        updateBorder();

        Dimension btnSize = new Dimension(120, 30);
        searchBtn.setMaximumSize(btnSize);
        clearBtn.setMaximumSize(btnSize);
        reIndexBtn.setMaximumSize(btnSize);

        add(searchBtn);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(clearBtn);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(reIndexBtn);
    }

    public void setTopPadding(int topPadding) {
        if (this.currentTopPadding != topPadding) {
            this.currentTopPadding = topPadding;
            updateBorder();
            revalidate();
            repaint();
        }
    }

    private void updateBorder() {
        setBorder(new EmptyBorder(currentTopPadding, 5, 10, 15));
    }
}
