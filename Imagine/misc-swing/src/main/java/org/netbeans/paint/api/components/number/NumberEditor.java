package org.netbeans.paint.api.components.number;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.openide.util.ChangeSupport;

/**
 * Simplified text editor for numbers with a bounded model which can be created
 * over a getter/setter pair.
 *
 * @author Tim Boudreau
 */
public final class NumberEditor extends JComponent {

    private final Fld field = new Fld();
    private final boolean floatingPoint;
    private final L l = new L();
    private final ChangeSupport supp = new ChangeSupport(this);
    private Number lastValue = 0;
    private boolean refreshing;
    private final ChangeFirer firer = new ChangeFirer();
    private final NumberModel<?> model;
    private final Border fieldBorder;
    private final Border errorBorder;

    public NumberEditor(NumberModel<?> model) {
        this(model, SwingConstants.TRAILING);
    }

    public NumberEditor(NumberModel<?> model, int horizontalAlignment) {
        this.model = model;
        floatingPoint = model.isFloatingPoint();
        setBorder(BorderFactory.createEmptyBorder());
        field.setColumns(model.maxCharacters());
        field.setHorizontalAlignment(horizontalAlignment);
        Border border = field.getBorder();
        if (border == null) {
            border = BorderFactory.createLineBorder(field.getForeground(), 1);
            field.setBorder(border);
        }
        fieldBorder = border;
        Insets ins = fieldBorder.getBorderInsets(field);
        errorBorder = BorderFactory.createMatteBorder(ins.top, ins.left, ins.bottom, ins.right, errorColor());
        add(field);
        refresh();
        addChangeListener(e -> {
            model.setValue(get());
        });
    }

    public NumberEditor setHorizontalAlignment(int align) {
        field.setHorizontalAlignment(align);
        return this;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        field.invalidate();
    }

    @Override
    public void revalidate() {
        super.revalidate();
        field.revalidate();
    }

    @Override
    public void validate() {
        super.validate();
        field.revalidate();
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public Insets getInsets(Insets insets) {
        insets.top = insets.left = insets.right = insets.bottom = 0;
        return insets;
    }

    public JTextField field() {
        return field;
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        field.setToolTipText(text);
    }

    @Override
    public void setFont(Font f) {
        super.setFont(f);
        field.setFont(f);
    }

    public void setBackground(Color bg) {
        super.setBackground(bg);
        field.setBackground(bg);
    }

    public void setForeground(Color fg) {
        super.setForeground(fg);
        field.setForeground(fg);
    }

    static Color errorColor;

    private static Color errorColor() {
        if (errorColor == null) {
            errorColor = UIManager.getColor("nb.errorForeground");
        }
        if (errorColor == null) {
            errorColor = Color.RED.darker();
        }
        return errorColor;
    }

    @Override
    public Dimension getPreferredSize() {
        return field.getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return field.getMaximumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return field.getMinimumSize();
    }

    @Override
    public void requestFocus() {
        // Ensure order
        super.requestFocus();
        field.requestFocus();
    }

    @Override
    protected boolean requestFocusInWindow(boolean temporary) {
        // bitwise or intentional- both need to be called
        return super.requestFocusInWindow(temporary) | field.requestFocusInWindow(temporary);
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        // Ensure order
        return super.requestFocus(temporary)
                | field.requestFocus(temporary);
    }

    @Override
    public boolean requestDefaultFocus() {
        // bitwise or intentional- both need to be called
        return super.requestDefaultFocus() | field.requestDefaultFocus();
    }

    @Override
    public boolean requestFocusInWindow() {
        // bitwise or intentional- both need to be called
        return super.requestFocusInWindow()
                | field.requestFocusInWindow();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refresh();
        field.addFocusListener(l);
        field.addMouseWheelListener(l);
        field.addKeyListener(l);
        field.getDocument().addDocumentListener(l);
    }

    @Override
    public void removeNotify() {
        field.getDocument().removeDocumentListener(l);
        field.removeKeyListener(l);
        field.removeMouseWheelListener(l);
        field.removeFocusListener(l);
        super.removeNotify();
    }

    private void refresh() {
        refreshing = true;
        lastValue = model.get();
        field.setText(model.stringValue());
        refreshing = false;
        checkValidValue();
    }

    @Override
    public void doLayout() {
        field.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override
    public int getBaseline(int width, int height) {
        if (width <= 0 || height <= 0) {
            Dimension d = field.getPreferredSize();
            width = d.width;
            height = d.height;
        }
        try {
            return field.getBaseline(width, height);
        } catch (IllegalArgumentException e) {
            doLayout();
            try {
                return field.getBaseline(width, height);
            } catch (IllegalArgumentException e1) {
                return field.getFontMetrics(field.getFont()).getAscent()
                        + field.getInsets().top;
            }
        }
    }

    boolean checkValidValue() {
        String txt = field.getText();
        boolean wasValid = isValidValue();
        boolean valid = model.isValid(txt);
        if (valid) {
            field.setBorder(fieldBorder);
        } else {
            field.setBorder(errorBorder);
        }
        if (wasValid != valid) {
            firePropertyChange("validity", wasValid, valid);
        }
        return valid;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        double pre = e.getPreciseWheelRotation();
        if (pre == 0) {
            return;
        }
        Number orig = model.get();
        Number num = model.increment(pre);
        if (!orig.equals(num)) {
            refresh();
            fireChange();
            e.consume();
        }
    }

    private void keyTyped(KeyEvent e) {
        boolean consume = false;
        NumberFormat fmt = model.getFormat();
        if (fmt instanceof DecimalFormat) {
            DecimalFormat dec = (DecimalFormat) fmt;
            DecimalFormatSymbols sym = dec.getDecimalFormatSymbols();
            if (sym != null) {
                if (e.getKeyChar() == sym.getDecimalSeparator()) {
                    return;
                }
                if (e.getKeyChar() == sym.getGroupingSeparator()) {
                    return;
                }
                if (e.getKeyChar() == sym.getMinusSign()) {
                    return;
                }
            }
            String s = new String(new char[]{e.getKeyChar()});
            if (s.equals(dec.getNegativeSuffix()) || s.equals(dec.getNegativeSuffix())) {
                return;
            }
            if (s.equals(dec.getPositivePrefix()) || s.equals(dec.getPositiveSuffix())) {
                return;
            }

        }
        switch (e.getKeyChar()) {
            case '-':
                consume = !getText().trim().isEmpty();
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                break;
            case '.':
                consume = getText().indexOf('.') >= 0;
                break;
            default:
                consume = true;
                break;
        }
        if (consume) {
            e.consume();
        }
    }

    private void keyPressed(KeyEvent e) {
        processKeyPress(e);
    }

    private void keyReleased(KeyEvent e) {
        processKeyPress(e);
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                model.decrement();
                break;
            case KeyEvent.VK_DOWN:
                model.increment();
                break;
        }
    }

    private void processKeyPress(KeyEvent e) {
        boolean consumeIt = false;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_CONTEXT_MENU:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_UP:
                break;
            case KeyEvent.VK_0:
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
            case KeyEvent.VK_6:
            case KeyEvent.VK_7:
            case KeyEvent.VK_8:
            case KeyEvent.VK_9:
            case KeyEvent.VK_PASTE:
                break;
            case KeyEvent.VK_V:
            case KeyEvent.VK_C:
            case KeyEvent.VK_A:
            case KeyEvent.VK_Z:
            case KeyEvent.VK_Y:
                if (e.getModifiersEx() == 0) {
                    consumeIt = true;
                }
                break;
            case KeyEvent.VK_MINUS:
                consumeIt = !field.getText().isEmpty();
                break;
            case KeyEvent.VK_PERIOD:
                consumeIt = field.getText().indexOf('.') >= 0
                        || !floatingPoint;
                break;
        }
        if (consumeIt) {
            e.consume();
        }
    }

    public boolean isValidValue() {
        return field.getBorder() != errorBorder;
    }

    public void addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
    }

    private void fireChange() {
        firer.enqueue();
    }

    public String getText() {
        return field.getText();
    }

    public void set(Number number) {
        if (!Objects.equals(number, model.get())) {
            model.setValue(number);
            refresh();
            fireChange();
        }
    }

    public double doubleValue() {
        return get().doubleValue();
    }

    public float floatValue() {
        return get().floatValue();
    }

    public long longValue() {
        return get().longValue();
    }

    public int intValue() {
        return get().intValue();
    }

    public Number get() {
        String txt = field.getText().trim();
        if (txt.isEmpty()) {
            return lastValue;
        }
        try {
            Number val;
            if (floatingPoint) {
                val = Double.parseDouble(txt.trim());
            } else {
                val = Integer.parseInt(txt.trim());
            }
            return val;
        } catch (NumberFormatException ex) {
            return lastValue;
        }
    }

    private boolean fireOnFocusChange;
    private boolean valueChanged;

    private void insertUpdate(DocumentEvent e) {
        if (refreshing) {
            return;
        }
        if (!fireOnFocusChange) {
            fireChange();
        } else {
            valueChanged = true;
        }
    }

    private void removeUpdate(DocumentEvent e) {
        if (refreshing) {
            return;
        }
        if (!fireOnFocusChange) {
            fireChange();
        } else {
            valueChanged = true;
        }
    }

    private void changedUpdate(DocumentEvent e) {
        if (refreshing) {
            return;
        }
        if (!fireOnFocusChange) {
            fireChange();
        } else {
            valueChanged = true;
        }
    }

    class ChangeFirer implements Runnable {

        private final AtomicBoolean enqueued = new AtomicBoolean();

        void enqueue() {
            if (enqueued.compareAndSet(false, true)) {
                EventQueue.invokeLater(this);
            }
        }

        @Override
        public void run() {
            enqueued.set(false);
            if (checkValidValue()) {
                supp.fireChange();
            }
        }
    }

    class L implements KeyListener, DocumentListener, FocusListener, MouseWheelListener {

        @Override
        public void keyTyped(KeyEvent e) {
            NumberEditor.this.keyTyped(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            NumberEditor.this.keyPressed(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            NumberEditor.this.keyReleased(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            NumberEditor.this.insertUpdate(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            NumberEditor.this.removeUpdate(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            NumberEditor.this.changedUpdate(e);
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (fireOnFocusChange) {
                refresh();
            }
            ((JTextComponent) e.getSource()).selectAll();
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (fireOnFocusChange) {
                if (valueChanged) {
                    valueChanged = false;
                    fireChange();
                }
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            NumberEditor.this.mouseWheelMoved(e);
        }
    }

    static class Fld extends JTextField {

        public Fld() {
        }

        public Fld(String text) {
            super(text);
        }

        public Fld(int columns) {
            super(columns);
        }

        public Fld(String text, int columns) {
            super(text, columns);
        }

        public Fld(Document doc, String text, int columns) {
            super(doc, text, columns);
        }

        @Override
        public boolean requestFocusInWindow(boolean temporary) {
            return super.requestFocusInWindow(temporary); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
