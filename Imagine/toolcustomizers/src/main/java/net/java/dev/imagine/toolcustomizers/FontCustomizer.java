/*
 * FontCustomizer.java
 *
 * Created on September 30, 2006, 2:33 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.java.dev.imagine.api.toolcustomizers.AbstractCustomizer;
import org.netbeans.paint.api.components.FontCellRenderer;
import org.netbeans.paint.api.components.FontComboBoxModel;
import org.netbeans.paint.api.components.Fonts;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public final class FontCustomizer extends AbstractCustomizer<Font> implements ActionListener {

    private enum FontStyle {
        PLAIN, BOLD, ITALIC, BOLD_ITALIC;

        @Override
        public String toString() {
            return NbBundle.getMessage(FontCustomizer.class, name());
        }

        int toFontConstant() {
            switch (this) {
                case PLAIN:
                    return Font.PLAIN;
                case BOLD:
                    return Font.BOLD;
                case ITALIC:
                    return Font.ITALIC;
                case BOLD_ITALIC:
                    return Font.BOLD | Font.ITALIC;
                default:
                    throw new AssertionError();
            }
        }

        static FontStyle fromFontConstant(int val) {
            switch (val) {
                case Font.PLAIN:
                    return PLAIN;
                case Font.BOLD:
                    return BOLD;
                case Font.ITALIC:
                    return ITALIC;
                case Font.BOLD | Font.ITALIC:
                    return BOLD_ITALIC;
                default:
                    return PLAIN;
            }
        }
    }

    private boolean initialized;

    /**
     * Creates a new instance of FontCustomizer
     */
    public FontCustomizer(String name) {
        super(name);
        getComponents();
        initialized = true;
    }

    @Override
    public Font get() {
        return getValue();
    }

    public Font getValue() {
        if (fontSelectBox == null) {
            getComponent();
        }
        Font f = (Font) fontSelectBox.getSelectedItem();
        FontStyle style = (FontStyle) styleBox.getSelectedItem();
        if (style != FontStyle.PLAIN) {
            f = f.deriveFont(style.toFontConstant());
        }
        return f;
    }

    JComboBox styleBox;
    JLabel styleLabel;
    JComboBox fontSelectBox;

    protected JComponent[] createComponents() {
        JLabel fontLabel = new JLabel(NbBundle.getMessage(FontCustomizer.class,
                "FONT_FACE")); //NOI18N
        if (fontSelectBox == null) {
            fontSelectBox = new JComboBox<Font>();
            fontSelectBox.addActionListener(this);
            Font[] f = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            ComboBoxModel<Font> fontsModel = new FontComboBoxModel();
            fontSelectBox.setModel(fontsModel);
            Font val = loadValue();
            fontSelectBox.setSelectedItem(val == null ? f[0] : val);
            fontSelectBox.setRenderer(FontCellRenderer.instance());
            fontSelectBox.setMinimumSize(new Dimension(100, 20));
        }
        if (styleLabel == null) {
            styleLabel = new JLabel(NbBundle.getMessage(FontCustomizer.class, "FONT_STYLE")); //NOI18N
        }
        if (styleBox == null) {
            DefaultComboBoxModel styleModel = new DefaultComboBoxModel(FontStyle.values());
            styleBox = new JComboBox(styleModel);
            Font ff = getValue();
            FontStyle style = FontStyle.fromFontConstant(ff.getStyle());
            styleBox.setSelectedItem(style);
            styleBox.addActionListener(this);
        }
        return new JComponent[]{
            fontLabel, fontSelectBox, styleLabel, styleBox
        };
//        JPanel font = new SharedLayoutPanel();
//        font.add(fontLabel);
//        font.add(fontSelectBox);
//        JPanel style = new SharedLayoutPanel();
//        style.add(styleLabel);
//        style.add(styleBox);
//        return new JComponent[]{font, style};
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (initialized) {
            change();
        }
    }

    @Override
    protected void saveValue(Font value) {
        if (value == null) {
            return;
        }
        Fonts.getDefault().set(getName(), value);
    }

    private Font loadValue() {
        return Fonts.getDefault().get(getName());
    }
}
