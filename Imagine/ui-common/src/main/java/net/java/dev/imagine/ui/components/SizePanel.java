/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.components;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NumberEditor;
import org.netbeans.paint.api.components.number.NumberModel;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.netbeans.paint.api.components.number.StandardNumericConstraints;
import org.openide.awt.Mnemonics;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "width=&Width",
    "height=&Height",})
public class SizePanel extends SharedLayoutRootPanel {

    private final NumericConstraint sizeConstraint = StandardNumericConstraints.INTEGER_NON_NEGATIVE
            .withMinimum(4).withMaximum(24000).withStep(1);
    private final NumberModel widthModel = NumberModel.ofInt(sizeConstraint,
            this::width, val -> this.width = val);
    private final NumberModel heightModel = NumberModel.ofInt(sizeConstraint,
            this::height, val -> this.height = val);
    private final NumberEditor widthEditor;
    private final NumberEditor heightEditor;
    private final SharedLayoutPanel widthPanel = new SharedLayoutPanel();
    private final SharedLayoutPanel heightPanel = new SharedLayoutPanel();
    private final JLabel widthLabel = new JLabel();
    private final JLabel heightLabel = new JLabel();
    private final ChangeSupport supp = new ChangeSupport(this);
    private final String key;

    private int width = 1280;
    private int height = 720;
    private L listener;

    public SizePanel(String key) {
        setLayout(new VerticalFlowLayout());
        setBorder(BorderFactory.createEmptyBorder());
        this.key = key;
        load();
        Mnemonics.setLocalizedText(widthLabel, Bundle.width());
        Mnemonics.setLocalizedText(heightLabel, Bundle.height());
        widthEditor = new NumberEditor(widthModel);
        heightEditor = new NumberEditor(heightModel);
        widthEditor.field().setHorizontalAlignment(JTextField.LEADING);
        heightEditor.field().setHorizontalAlignment(JTextField.LEADING);
        widthLabel.setLabelFor(widthEditor);
        heightLabel.setLabelFor(heightEditor);
        widthPanel.add(widthLabel);
        widthPanel.add(widthEditor);
        heightPanel.add(heightLabel);
        heightPanel.add(heightEditor);
        add(widthPanel);
        add(heightPanel);
    }

    private void load() {
        Preferences prefs = NbPreferences.forModule(SizePanel.class);
        int width = prefs.getInt(key + "-width", 1280);
        if (!sizeConstraint.isValidValue(width)) {
            width = sizeConstraint.maximum().intValue();
        }
        int height = prefs.getInt(key + "-height", 720);
        if (!sizeConstraint.isValidValue(height)) {
            height = sizeConstraint.maximum().intValue();
        }
        this.width = width;
        this.height = height;
    }

    public void save() {
        Preferences prefs = NbPreferences.forModule(SizePanel.class);
        prefs.putInt(key + "-width", width);
        prefs.putInt(key + "-height", height);
    }

    public Dimension getDimension() {
        return new Dimension(width, height);
    }

    private int width() {
        return width;
    }

    private int height() {
        return height;
    }

    private boolean listening;

    private void ensureListening() {
        if (!listening) {
            startListening();
            listening = true;
        }
    }

    private void ensureNotListening() {
        if (listening) {
            stopListening();
            listening = false;
        }
    }

    public void addChangeListener(ChangeListener listener) {
        ensureListening();
        supp.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
        if (!supp.hasListeners()) {
            ensureNotListening();
        }
    }

    public boolean isValidValue() {
        return widthEditor.isValidValue()
                && heightEditor.isValidValue();
    }

    private void startListening() {
        if (listener == null) {
            listener = new L();
        }
        widthEditor.addChangeListener(listener);
        heightEditor.addChangeListener(listener);
        widthEditor.addPropertyChangeListener("validity", listener);
        heightEditor.addPropertyChangeListener("validity", listener);
    }

    private void stopListening() {
        assert listener != null;
        widthEditor.removeChangeListener(listener);
        heightEditor.removeChangeListener(listener);
        widthEditor.removePropertyChangeListener("validity", listener);
        heightEditor.removePropertyChangeListener("validity", listener);
    }

    class L implements ChangeListener, PropertyChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            supp.fireChange();
        }
    }
}
