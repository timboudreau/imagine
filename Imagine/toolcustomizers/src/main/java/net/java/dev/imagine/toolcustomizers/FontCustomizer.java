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
import java.awt.Dimension;
import java.awt.Font;
import java.util.prefs.Preferences;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.netbeans.paint.api.components.FontComboBoxModel;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public final class FontCustomizer extends ListenableCustomizerSupport<Font> implements ListenableCustomizer<Font> {

    private final String name;
    private String fontName;
    private FontStyle style;
    private float size;

    public FontCustomizer(String name, Font existingValue) {
        this.name = name;
        if (existingValue == null) {
            loadValue();
        } else {
            name = existingValue.getFamily();
            size = existingValue.getSize2D();
            style = FontStyle.fromFontConstant(existingValue.getStyle());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Font get() {
        return new Font(fontName, Font.PLAIN, 1).deriveFont(size);
    }

    public JComponent getComponent() {
        JComboBox<FontStyle> styleBox;
        JLabel styleLabel;
        JComboBox<Font> fontSelectBox;
        JLabel fontLabel;
        Font ff = get();
        fontLabel = new JLabel(); //NOI18N
        Mnemonics.setLocalizedText(fontLabel, NbBundle.getMessage(FontCustomizer.class,
                "FONT_FACE"));
        fontSelectBox = FontComboBoxModel.newFontComboBox();
        fontSelectBox.setSelectedItem(ff);
        fontSelectBox.setMinimumSize(new Dimension(100, 20));
        fontSelectBox.addActionListener(ae -> {
            Font f = (Font) fontSelectBox.getSelectedItem();
            fontName = f.getFamily();
            fire();
        });
        fontLabel.setLabelFor(fontSelectBox);
        styleLabel = new JLabel(); //NOI18N
        Mnemonics.setLocalizedText(styleLabel, NbBundle.getMessage(FontCustomizer.class,
                "FONT_STYLE"));
        styleBox = EnumComboBoxModel.newComboBox(FontStyle.class);
        FontStyle style = FontStyle.fromFontConstant(ff.getStyle());
        styleBox.setSelectedItem(style);
        styleBox.addActionListener(ae -> {
            this.style = (FontStyle) styleBox.getSelectedItem();
            fire();
        });
        styleLabel.setLabelFor(styleBox);
        styleBox.setRenderer(new StyleRenderer());

        JPanel result = new JPanel(new VerticalFlowLayout());

        SharedLayoutPanel panel = new SharedLayoutPanel();
        panel.add(fontLabel);
        panel.add(fontSelectBox);
        result.add(panel);

        panel = new SharedLayoutPanel();
        panel.add(styleLabel);
        panel.add(styleBox);
        result.add(panel);

        SharedLayoutPanel sizePanel = new SharedLayoutPanel();
        JLabel sizeLabel = new JLabel();
        Mnemonics.setLocalizedText(sizeLabel, NbBundle.getMessage(FontCustomizer.class,
                "SIZE"));
        JSlider slider = new JSlider(4, 200, (int) size);
        PopupSliderUI.attach(slider);
        slider.addChangeListener(evt -> {
            this.size = slider.getValue();
            fire();
        });
        sizeLabel.setLabelFor(slider);
        sizePanel.add(sizeLabel);
        sizePanel.add(slider);
        result.add(sizePanel);

        return result;
    }

    @Override
    protected void onAfterFire() {
        saveValue(get());
    }

    protected void saveValue(Font value) {
        if (value == null) {
            return;
        }
        Preferences prefs = NbPreferences.forModule(FontCustomizer.class);
        prefs.putFloat(name + "-fontsize", size);
        prefs.put(name + "-fontname", fontName);
        prefs.putInt(name + "-fontstyle", style.ordinal());

    }

    private void loadValue() {
        Preferences prefs = NbPreferences.forModule(FontCustomizer.class);
        this.size = prefs.getFloat(name + "-fontsize", 75F);
        this.fontName = prefs.get(name + "-fontname", "Times New Roman");
        this.style = FontStyle.values()[prefs.getInt(name + "-fontstyle", 0)];
    }

    class StyleRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); //To change body of generated methods, choose Tools | Templates.
            Font f = get();
            if (f == null) {
                f = list.getFont();
            }
            f = f.deriveFont(list.getFont().getSize2D());
            result.setFont(f);
            return result;
        }
    }
}
