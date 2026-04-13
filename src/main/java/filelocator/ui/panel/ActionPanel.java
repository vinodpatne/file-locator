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
    private final JButton reIndexBtn = new JButton("Update Index");
    private final JButton clearBtn = new JButton("Clear Search");

    public ActionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(25, 5, 10, 15));

        Dimension btnSize = new Dimension(120, 30);
        reIndexBtn.setMaximumSize(btnSize);
        clearBtn.setMaximumSize(btnSize);

        add(reIndexBtn);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(clearBtn);
    }
}
