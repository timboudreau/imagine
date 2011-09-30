/*
 * FontCustomizer.java
 *
 * Created on September 30, 2006, 2:33 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.toolcustomizers;

import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import net.java.dev.imagine.api.toolcustomizers.AbstractCustomizer;
import org.netbeans.paint.api.components.FontComboBoxModel;
import org.netbeans.paint.api.components.Fonts;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public final class FontCustomizer extends AbstractCustomizer <Font> implements ActionListener {
    
    private enum FontStyle {
        PLAIN, BOLD, ITALIC, BOLD_ITALIC;
        @Override
        public String toString() {
            return NbBundle.getMessage(FontCustomizer.class, name());
        }
        
        int toFontConstant() {
            switch (this) {
                case PLAIN :
                    return Font.PLAIN;
                case BOLD :
                    return Font.BOLD;
                case ITALIC :
                    return Font.ITALIC;
                case BOLD_ITALIC :
                    return Font.BOLD | Font.ITALIC;
                default : 
                    throw new AssertionError();
            }
        }
        
        static FontStyle fromFontConstant(int val) {
            switch (val) {
                case Font.PLAIN : return PLAIN;
                case Font.BOLD : return BOLD;
                case Font.ITALIC : return ITALIC;
                case Font.BOLD | Font.ITALIC : return BOLD_ITALIC;
                default : return PLAIN;
            }
        }
    }
    
    /** Creates a new instance of FontCustomizer */
    public FontCustomizer(String name) {
        super (name);
    }

    public Font getValue() {
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
        fontSelectBox = new JComboBox();
        fontSelectBox.addActionListener(this);
        Font[] f = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        DefaultComboBoxModel fontsModel = new FontComboBoxModel();
        JLabel fontLabel = new JLabel (NbBundle.getMessage(FontCustomizer.class, 
                "FONT_FACE")); //NOI18N
        fontSelectBox.setModel (fontsModel);
        fontSelectBox.setSelectedItem(loadValue());
        fontSelectBox.setRenderer(new FontRenderer());
        if (styleBox == null) {
            DefaultComboBoxModel styleModel = new DefaultComboBoxModel (FontStyle.values());
            styleBox = new JComboBox(styleModel);
            styleLabel = new JLabel (NbBundle.getMessage(FontCustomizer.class, "FONT_STYLE")); //NOI18N
            Font ff = getValue();
            FontStyle style = f == null ? FontStyle.PLAIN : FontStyle.fromFontConstant(ff.getStyle());
            styleBox.setSelectedItem(style);
            styleBox.addActionListener(this);
        }
        JPanel font = new SharedLayoutPanel();
        font.add (fontLabel, fontSelectBox);
        JPanel style = new SharedLayoutPanel();
        style.add (styleLabel, styleBox);
        return new JComponent[] { font, style };
    }
    
    public void actionPerformed(ActionEvent e) {
        change();
    }
    
    
    private static final class FontRenderer extends DefaultListCellRenderer {
        public void propertyChange (String s, Object a, Object b) {
            //performance - do nothing
        }
        
        @Override
        public void repaint(int x, int y, int w, int h) {}
        @Override
        public void validate() {}
        @Override
        public void invalidate() {}
        @Override
        public void revalidate() {}

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component retValue = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                Font f = (Font) value;
                retValue.setFont (f);
                setText (f.getName());
            } else {
                setText ("");
            }
            return retValue;
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
