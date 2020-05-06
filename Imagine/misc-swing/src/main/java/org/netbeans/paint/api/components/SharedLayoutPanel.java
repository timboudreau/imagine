package org.netbeans.paint.api.components;

import com.mastfrog.function.state.Dbl;
import com.mastfrog.function.state.Int;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import net.java.dev.colorchooser.ColorChooser;
import org.netbeans.paint.api.components.number.NumberEditor;
import org.netbeans.paint.api.components.number.StandardNumericConstraints;
import org.openide.awt.Mnemonics;

/**
 * A panel which, if it has an ancestor that implements SharedLayoutData, will
 * automatically align its so that all children of that parent have their
 * columns aligned. Use for a set of components (such as a label for something
 * followed by a control that affects that setting) which will appear in an
 * ancestor component, but may be children of each other, yet should nonetheless
 * have their columns aligned.
 * <p/>
 * Note that the layout manager may not be set at runtime on instances of
 * SharedLayoutPanel.
 * <p/>
 * SharedLayoutPanels also have an &quot;expanded&quot; property - a component
 * may hide some data until a button is clicked, at which point it grows and
 * displays a detail view. By default any SharedLayoutPanel shares its expanded
 * setting with all others owned by the same SharedLayoutData ancestor, so if
 * any one is expanded, all others are set to unexpanded - so only one is
 * expanded at a time.
 *
 * @author Tim Boudreau
 */
public class SharedLayoutPanel extends JPanel implements LayoutDataProvider {

    private boolean initialized;

    public SharedLayoutPanel() {
        super(new LDPLayout());
        initialized = true;
    }

    public SharedLayoutPanel(Component c) {
        this();
        add(c);
    }

    public SharedLayoutPanel(Component c1, Component c2) {
        this();
        add(c1);
        add(c2);
    }

    public SharedLayoutPanel(Component c1, Component c2, Component c3) {
        this();
        add(c1);
        add(c2);
        add(c3);
    }

    public SharedLayoutPanel(String caption, Component c) {
        this();
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, caption);
        setName(caption);
        add(lbl);
        lbl.setLabelFor(c);
        add(c);
    }

    public SharedLayoutPanel(String caption, Component c1, Component c2) {
        this();
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, caption);
        setName(caption);
        add(lbl);
        lbl.setLabelFor(c1);
        add(c1);
        add(c2);
    }

    public SharedLayoutPanel(String caption, Component c1, Component c2, Component c3) {
        this();
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, caption);
        setName(caption);
        add(lbl);
        lbl.setLabelFor(c1);
        add(c1);
        add(c2);
        add(c3);
    }


    /**
     * Overridden to throw an exception if called at runtime.
     *
     * @param mgr The layout manager
     */
    @Override
    public final void setLayout(LayoutManager mgr) {
        if (initialized) {
            throw new UnsupportedOperationException("Cannot set layout on a SharedLayoutPanel");
        }
        super.setLayout(mgr);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SharedLayoutData p = SharedLayoutData.find(this);
        if (p != null) {
            p.register(this);
        }
    }

    @Override
    public void removeNotify() {
        SharedLayoutData p = SharedLayoutData.find(this);
        if (p != null) {
            p.unregister(this);
        }
        super.removeNotify();
    }

    /**
     * Gets the column position for this column from the layout manager.
     *
     * @param col The column
     * @return A pixel position in the x axis
     */
    @Override
    public final int getColumnPosition(int col) {
        LDPLayout layout = (LDPLayout) getLayout();
        return layout.getColumnPosition(this, col);
    }

    /**
     * Returns false by default. Override if you want to support expansion.
     *
     * @return
     */
    public boolean isExpanded() {
        return false;
    }

    /**
     * Does nothing by default; override to handle the case that you actually
     * want to change the set of child components when expanded. You are
     * responsible for providing a control that actually sets the expanded
     * state.
     *
     * @param val To expand or not
     */
    public void doSetExpanded(boolean val) {
        //do nothing by default
    }

    public static void main(String[] args) {

        JPanel jp = new SharedLayoutRootPanel(3);

        JComboBox box = new JComboBox(new String[]{"Hey", "there", "Whatever"});
        JLabel one = new JLabel("Box One");
        SharedLayoutPanel p = new SharedLayoutPanel(one);
        p.add(box);
        jp.add(p);
        p.add(new JLabel("Hey"));
        p.add(new JTextField("You"));

        Dbl dbl = Dbl.create();
        NumberEditor ne = new NumberEditor(
                StandardNumericConstraints.DOUBLE_ZERO_TO_ONE.doubleModel(dbl, dbl),
                SwingConstants.LEADING);
        ne.putClientProperty("noStretch", true);
        jp.add(new SharedLayoutPanel(new JLabel("Numbers"), ne));
        Int in = Int.create();
        NumberEditor ne2 = new NumberEditor(StandardNumericConstraints.INTEGER_DEGREES.intModel(in, in));
        jp.add(new SharedLayoutPanel(new JLabel("PDegrees"), ne2));

        JSlider slid1 = new JSlider(10, 20, 15);
        PopupSliderUI.attach(slid1);
        jp.add(new SharedLayoutPanel(new JLabel("Wookies"), slid1));

        JButton but = new JButton("A button");
        JLabel three = new JLabel("Buttons align?");
        p = new SharedLayoutPanel(three);
        p.add(but);
        jp.add(p);

        JComboBox box2 = new JComboBox(new String[]{"Hey", "there", "Whatever"});
        JLabel two = new JLabel("Box Two");
        p = new SharedLayoutPanel(two);
        p.add(box2);
        jp.add(p);

        JTextField area = new JTextField("Woogle goovers");
        two = new JLabel("A field");
        p = new SharedLayoutPanel(two);
        p.add(area);
        jp.add(p);

        ColorChooser ch = new ColorChooser(Color.ORANGE);
//        ch.setPreferredSize(new Dimension(16, 16));
        two = new JLabel("A color chooser");
        p = new SharedLayoutPanel(two);
        p.add(ch);
        jp.add(p);

        JSlider slid = new JSlider();
        slid.setOpaque(true);
        slid.setBackground(Color.YELLOW);
        PopupSliderUI.attach(slid);
        two = new JLabel("A Popup Slider");
        p = new SharedLayoutPanel(two);
        p.add(slid);
        jp.add(p);

        slid = new JSlider();
        slid.setOpaque(true);
        slid.setPreferredSize(new Dimension(64, 64));
        RadialSliderUI.attach(slid);
        two = new JLabel("A Popup Slider");
        p = new SharedLayoutPanel(two);
        p.add(slid);
        jp.add(p);

//        JComboBox fonts = new JComboBox(new FontComboBoxModel());
//        fonts.setRenderer(FontCellRenderer.instance());
//        two = new JLabel("The Fonts");
//        p = new SharedLayoutPanel(two);
//        p.add(fonts);
//        jp.add(p);
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(jp);
        jf.pack();
        jf.setVisible(true);
    }
}
