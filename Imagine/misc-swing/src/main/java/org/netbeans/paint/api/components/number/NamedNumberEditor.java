/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.number;

import com.mastfrog.function.FloatConsumer;
import com.mastfrog.function.FloatSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.swing.JLabel;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.openide.awt.Mnemonics;

/**
 *
 * @author Tim Boudreau
 */
public final class NamedNumberEditor extends SharedLayoutPanel {

    private final NumberEditor editor;
    private final JLabel label;

    public NamedNumberEditor(String name, NumberModel model) {
        this(name, new NumberEditor(model));
    }

    public NamedNumberEditor(String name, NumericConstraint constraint, DoubleSupplier getter, DoubleConsumer setter) {
        this(name, new NumberEditor(NumberModel.ofDouble(constraint, getter, setter)));
    }

    public NamedNumberEditor(String name, NumericConstraint constraint, FloatSupplier getter, FloatConsumer setter) {
        this(name, new NumberEditor(NumberModel.ofFloat(constraint, getter, setter)));
    }

    public NamedNumberEditor(String name, NumericConstraint constraint, IntSupplier getter, IntConsumer setter) {
        this(name, new NumberEditor(NumberModel.ofInt(constraint, getter, setter)));
    }

    public NamedNumberEditor(String name, NumberEditor editor) {
        this.editor = editor;
        label = new JLabel();
        Mnemonics.setLocalizedText(label, name);
        label.setLabelFor(editor.field());
        add(label);
        add(editor);
    }
}
