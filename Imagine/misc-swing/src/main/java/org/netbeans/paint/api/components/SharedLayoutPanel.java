package org.netbeans.paint.api.components;

import com.mastfrog.function.state.Dbl;
import com.mastfrog.function.state.Int;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
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
    private SharedLayoutData data;

    public SharedLayoutPanel() {
        super(new LDPLayout());
        super.setBorder(new SharedLayoutIndentBorder());
        initialized = true;
        if (TitledPanel2.isDebugLayout()) {
            setBackground(new Color(Color.HSBtoRGB(
                    ThreadLocalRandom.current().nextFloat(), 0.32F, 0.93F)));
        }
    }

    public SharedLayoutPanel(Component c) {
        this();
        add(c);
        if (c instanceof JLabel) {
            setName(((JLabel) c).getText());
        }
    }

    public SharedLayoutPanel(Component c1, Component c2) {
        this();
        if (c1 instanceof JLabel) {
            setName(((JLabel) c1).getText());
        }
        add(c1);
        add(c2);
    }

    public SharedLayoutPanel(Component c1, Component c2, Component c3) {
        this();
        if (c1 instanceof JLabel) {
            setName(((JLabel) c1).getText());
        }
        add(c1);
        add(c2);
        add(c3);
    }

    public SharedLayoutPanel(Component c1, Component... more) {
        this();
        if (c1 instanceof JLabel) {
            setName(((JLabel) c1).getText());
        }
        add(c1);
        for (Component m : more) {
            add(m);
        }
    }

    public SharedLayoutPanel(String caption, Component c) {
        this();
        setName(caption);
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, caption);
        setName(caption);
        add(lbl);
        lbl.setLabelFor(c);
        add(c);
    }

    public SharedLayoutPanel(String caption, Component c1, Component c2) {
        this();
        setName(caption);
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
        setName(caption);
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, caption);
        setName(caption);
        add(lbl);
        lbl.setLabelFor(c1);
        add(c1);
        add(c2);
        add(c3);
    }

    public SharedLayoutPanel(String caption, Component... comps) {
        this();
        setName(caption);
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, caption);
        add(lbl);
        if (comps.length > 0) {
            lbl.setLabelFor(comps[0]);
        }
        for (Component comp : comps) {
            add(comp);
        }
    }

    /**
     * Overridden to prevent abuse.
     *
     * @return The layout manager
     */
    @Override
    public final Component add(Component comp) {
        return super.add(comp);
    }

    /**
     * Overridden to prevent abuse.
     *
     * @return The layout manager
     */
    @Override
    public final void setName(String name) {
        super.setName(name);
    }

    SharedLayoutData dataUnsafe() {
        // optimized accessor for SharedLayoutData.find
        return data;
    }

    @Override
    public void setBorder(Border border) {
        // do nothing, the indent border manages this
        super.setBorder(border);
    }

    /**
     * Overridden to throw an exception if called by anything but this class'
     * constructor.
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

    protected SharedLayoutData data() {
        if (data == null) {
            // do have an ancestor, don't have a peer
            return SharedLayoutData.find(this);
        }
        return data;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        data = SharedLayoutData.find(this);
        if (data != null) {
            data.register(this);
        }
    }

    @Override
    public void removeNotify() {
        if (data != null) {
            data.unregister(this);
            data = null;
        }
        super.removeNotify();
    }

    /**
     * Overridden to prevent abuse.
     *
     * @return The layout manager
     */
    @Override
    public final LayoutManager getLayout() {
        return super.getLayout();
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
     * state, and notifying the shared layout data.
     *
     * @param val To expand or not
     */
    public void doSetExpanded(boolean val) {
        //do nothing by default
    }

    @Override
    protected final void addImpl(Component comp, Object constraints, int index) {
        if (TitledPanel2.isDebugLayout()) {
            comp.setBackground(new Color(Color.HSBtoRGB(
                    ThreadLocalRandom.current().nextFloat(), 0.32F, 0.93F)));
        }
        super.addImpl(comp, constraints, index);
    }

    @Override
    public String toString() {
        return "SharedLayoutPanel(" + getName() + " with " + getComponentCount() + " children "
                + " over " + data + " id " + Integer.toString(System.identityHashCode(this), 36)
                + " in " + getParent() + " with insets " + insetsString() + ")";
    }

    String insetsString() {
        Insets ins = getInsets();
        if (ins.left == 0 && ins.right == 0 && ins.top == 0 && ins.bottom == 0) {
            return "<0>";
        }
        return "<" + ins.top + "," + ins.left + " " + ins.right + " " + ins.bottom
                + " " + getBorder().getClass().getSimpleName() + ">";
    }

    public static Border createIndentBorder() {
        return new SharedLayoutIndentBorder();
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {

        Font f = new Font("Source Sans Pro", Font.PLAIN, 20);
        UIManager.put("controlFont", f);
        UIManager.put("Label.font", f);
        UIManager.put("Panel.font", f);
        UIManager.put("Slider.font", f);
        UIManager.put("ComboBox.font", f);
        UIManager.put("Tree.font", f);
        UIManager.put("Button.font", f);
        UIManager.put("TextField.font", f);
        UIManager.put("List.font", f);
        UIManager.put("ComboBox.font", f);

//        TitledPanel2.debugLayout(true);
        JFrame jf = new JFrame();
        JPanel jp = new SharedLayoutRootPanel();
//        jp.setBackground(new Color(255, 190, 0, 128));

        JComboBox box = new JComboBox(new String[]{"Hey", "there", "Whatever"});
        box.setFont(f.deriveFont(12F));

        box.setRenderer(new DefaultListCellRenderer());
        JLabel one = new JLabel("Box One");
        one.setName("oneLabel");
        SharedLayoutPanel p = new SharedLayoutPanel(one);
        SharedLayoutPanel pppp = p;
        jp.add(p);
        p.add(box);
        JLabel pubu = new JLabel("Pubu hoodge");
        JTextField you = new JTextField("You");
        pubu.setName("pubu");
        you.setName("you");
        box.setName("box");
        p.add(pubu);
        p.add(you);

//        jp.add(new DemoTitledPanel());
        JTree realTree = new JTree();
        realTree.setName("realTree");
        JScrollPane treePane = new JScrollPane(realTree);
        treePane.setName("treePane");

        JComponent tree = new SharedLayoutPanel("Inner", treePane);
        tree.setName("TreeShared");

        tree.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        JPanel inner = new JPanel();
        inner.setName("inner");
        inner.add(tree);
        inner.setBackground(Color.ORANGE);

        JSlider sli = new JSlider(10, 120, 15);
        PopupSliderUI.attach(sli);
        sli.setName("huer");

        TitledPanel2 tp2 = new TitledPanel2("Foogle", false, (exp) -> {
            return exp ? tree : sli;
        }, () -> {
            JOptionPane.showMessageDialog(jp, "Customize!");
        });

        tp2.add(new JLabel("And this is a label that should be at the bottom."));
        tp2.add(new JLabel("And this is another one."));

        jp.add(tp2);

        JComboBox<String> combo = new JComboBox<>();
        DefaultComboBoxModel<String> mdl = new DefaultComboBoxModel<>();
        mdl.addElement("Hello");
        mdl.addElement("You");
        mdl.addElement("me");
        combo.setModel(mdl);
        combo.setFont(f.deriveFont(12F));

        TitledPanel2 tp3 = new TitledPanel2("Woogle", false, (exp) -> {
            return exp ? new JComboBox() : combo;
        });
        jp.add(tp3);

        TitledPanel2 nested = new TitledPanel2("Nesty!", false, exp -> {
            return exp ? new JCheckBox("Well hello there") : null;
        });

        TitledPanel2 nester = new TitledPanel2("Nester", false, exp -> {
            return exp ? nested : new JLabel("try me!");
        });

        jp.add(nester);

        Dbl dbl = Dbl.create();
        NumberEditor ne = new NumberEditor(
                StandardNumericConstraints.DOUBLE_ZERO_TO_ONE.doubleModel(dbl, dbl),
                SwingConstants.LEADING);
        ne.putClientProperty("noStretch", true);
        jp.add(new SharedLayoutPanel(new JLabel("Numbers"), ne));
        Int in = Int.create();
        NumberEditor ne2 = new NumberEditor(StandardNumericConstraints.INTEGER_DEGREES.intModel(in, in));

        JButton repack = new JButton("Repack");
        repack.addActionListener(ae -> {
            jf.pack();
        });

        jp.add(new SharedLayoutPanel("PDegrees", ne2, repack));

        JSlider slid1 = new JSlider(10, 120, 15);
        PopupSliderUI.attach(slid1);
//        slid1.setUI(new SimpleSliderUI());
//        slid1.setOrientation(JSlider.HORIZONTAL);
        jp.add(new SharedLayoutPanel("Wookies", slid1));

        JButton but = new JButton("A button");
        JLabel three = new JLabel("Buttons align?");
        p = new SharedLayoutPanel(three, but);
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

        JComboBox fonts = new JComboBox(new FontComboBoxModel());
        fonts.setRenderer(FontCellRenderer.instance());
        two = new JLabel("The Fonts");
        p = new SharedLayoutPanel(two);
        p.add(fonts);
        jp.add(p);

        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(jp);
        jf.pack();
        jf.setVisible(true);
    }
}
