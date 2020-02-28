/*
 * TextCustomizer.java
 *
 * Created on September 30, 2006, 2:31 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.java.dev.imagine.api.toolcustomizers.AbstractCustomizer;
import org.openide.awt.Mnemonics;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public final class TextCustomizer extends AbstractCustomizer<String>
        implements DocumentListener, ListenableCustomizer<String> {

    private String text;

    public TextCustomizer(String name, String existingValue) {
        super(name);
        if (existingValue == null) {
            existingValue = loadValue();
        }
        this.text = existingValue;
    }

    @Override
    public String getValue() {
        return text;
    }

    @Override
    protected void onAfterFire() {
        change();
    }

    private void updateText(DocumentEvent e) {
        Document d = e.getDocument();
        d.render(() -> {
            try {
                text = d.getText(0, d.getLength());
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
        fire();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateText(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateText(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateText(e);
    }

    protected JComponent[] createComponents() {
        JTextField f = new JTextField(text);
        f.setColumns(20);
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, NbBundle.getMessage(TextCustomizer.class, "TEXT")); //NOI18N

        f.getDocument().addDocumentListener(this);
        return new JComponent[]{lbl, f};
    }

    private String loadValue() {
        return NbPreferences.forModule(getClass()).get(getName() + ".text",
                NbBundle.getMessage(TextCustomizer.class, "SAMPLE_TEXT"));
    }

    @Override
    protected void saveValue(String value) {
        NbPreferences.forModule(getClass()).put(getName() + ".text", value);
    }
}
