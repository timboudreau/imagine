/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public abstract class TitledPanel extends JPanel implements LayoutDataProvider {

    private static final int GAP = 5;
    private final JButton expandButton = new JButton(new ExpandAction());
    private final JPanel innerPanel = new SharedLayoutPanel();
    private boolean expanded = false;
    private final JLabel nameLabel = new JLabel();
    private final JPanel infoButtonPanel = new JPanel();
    protected final JButton configButton = new JButton();
    protected final JPanel buttonAndNamePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

    protected TitledPanel(Class clazz, String bundleKey) {
        this(NbBundle.getMessage(clazz, bundleKey));
    }

    protected TitledPanel(String title) {
        Mnemonics.setLocalizedText(nameLabel, title);
//        innerPanel.setLayout(new LDPLayout(GAP));
//        innerPanel.setLayout(new BorderLayout());

        buttonAndNamePanel.add(expandButton);
        buttonAndNamePanel.add(nameLabel);
        innerPanel.add(buttonAndNamePanel, BorderLayout.CENTER);
        expandButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/netbeans/paint/api/components/turner-right.png"))); //NOI18N
        setLayout(new BorderLayout());
        add(innerPanel, BorderLayout.WEST);
        configButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCustomize();
            }
        });
        infoButtonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        infoButtonPanel.add(configButton);
        configButton.setContentAreaFilled(false);
        configButton.setBorder(BorderFactory.createEmptyBorder());
        ImageIcon icon = new ImageIcon(ImageUtilities.loadImage(
                "org/netbeans/paint/api/components/info.png")); //NOI18N
//        config.setText("I"); //XXX i18n
        configButton.setIcon(icon);
        configButton.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        expandButton.setContentAreaFilled(false);
        expandButton.setBorder(BorderFactory.createEmptyBorder());
        expandButton.setFocusable(true);
        expandButton.addMouseListener(new MML());
        nameLabel.setLabelFor(expandButton);
        nameLabel.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && !e.isPopupTrigger()) {
                    expandButton.doClick();
                }
            }
        });
    }

    protected void setTitle(String ttl) {
        nameLabel.setText(ttl);
    }

    public Component setExpanded(boolean val) {
        if (expanded != val) {
            expanded = val;
            if (val) {
                innerPanel.add(infoButtonPanel, BorderLayout.SOUTH);
                if (center != null) {
                    innerPanel.remove(center);
                    remove(innerPanel);
                    add(innerPanel, BorderLayout.NORTH);
                    add(center, BorderLayout.CENTER);
                }
//                expandButton.setIcon(new ImageIcon (Utilities.loadImage("org/netbeans/paint/api/components/turner-down.png")));
            } else {
//                expandButton.setIcon(new ImageIcon (Utilities.loadImage("org/netbeans/paint/api/components/turner-right.png")));
                innerPanel.remove(infoButtonPanel);
                remove(innerPanel);
                add(innerPanel, BorderLayout.WEST);
                if (center != null) {
                    innerPanel.remove(center);
                    innerPanel.add(center, BorderLayout.CENTER);
                }
            }
            expandButton.setIcon(new ImageIcon(ImageUtilities.loadImage(getImageNameForState(expanded, containsMouse))));
            SharedLayoutData data = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, this);
            if (data != null) {
                data.expanded(this, val);
            }
            invalidate();
            revalidate();
            repaint();
        }
        return center;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    protected abstract void onCustomize();

    private Component center;

    public final void setCenterComponent(Component c) {
        if (center != null) {
            remove(center);
            innerPanel.remove(center);
        }
        center = c;
        if (center != null) {
            if (isExpanded()) {
                add(c, BorderLayout.CENTER);
            } else {
                innerPanel.add(c, BorderLayout.CENTER);
            }
        }
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SharedLayoutData d = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, this);
        if (d != null) {
            d.register(this);
        }
    }

    @Override
    public void removeNotify() {
        SharedLayoutData d = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, this);
        if (d != null) {
            d.unregister(this);
        }
        super.removeNotify();
    }

    public void doSetExpanded(boolean val) {
        if (val != isExpanded()) {
            Component c = setExpanded(val);
            if (c != center) {
                setCenterComponent(c);
            }
        }
    }

    private final class ExpandAction extends AbstractAction {

        ExpandAction() {
        }

        public void actionPerformed(ActionEvent e) {
            doSetExpanded(!isExpanded());
        }
    }

    public int getColumnPosition(int col) {
        if (true) {
            return 0;
        }
        if (!isExpanded()) {
            switch (col) {
                case 0:
                    return getInsets().left;
                case 1:
                    return expandButton.getPreferredSize().width + getInsets().left;
                case 2:
                    if (infoButtonPanel.isVisible()) {
                        return expandButton.getPreferredSize().width + getInsets().left
                                + infoButtonPanel.getPreferredSize().width;
                    }
                default :
                    return -1;
            }
        }
        if (col == 0) {
            return innerPanel.getInsets().left;
        }
        Component[] c = innerPanel.getComponents();
        int x = innerPanel.getInsets().left;
        if (col < c.length) {
            for (int i = 0; i < c.length; i++) {
                x += c[i].getPreferredSize().width;
                x += GAP;
                if (i == col - 1) {
                    break;
                }
            }
        }
        return x;
    }

    private String getImageNameForState(boolean expanded, boolean containsMouse) {
        return "org/netbeans/paint/api/components/turner-" + (expanded ? "down" : "right")
                + //NOI18N
                (containsMouse ? "-lit" : "") + ".png"; //NOI18N
    }

    boolean containsMouse;

    private void setContainsMouse(boolean val) {
        if (val != containsMouse) {
            containsMouse = val;
            boolean exp = isExpanded();
            try {
                expandButton.setIcon(new ImageIcon(ImageUtilities.loadImage(getImageNameForState(exp, containsMouse))));
            } catch (NullPointerException e) {
                throw new NullPointerException("Null icon for " + getImageNameForState(exp, containsMouse));
            }
        }
    }

    private final class MML implements MouseListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            setContainsMouse(true);
        }

        public void mouseExited(MouseEvent e) {
            setContainsMouse(false);
        }

    }
}
