package net.java.dev.imagine.layers.text.widget;

import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.InplaceEditorProvider;
import org.netbeans.api.visual.action.InplaceEditorProvider.EditorController;
import org.netbeans.api.visual.action.InplaceEditorProvider.ExpansionDirection;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class TextAreaInplaceEditorProvider implements InplaceEditorProvider<JTextArea> {

    private TextFieldInplaceEditor editor;
    private EnumSet<InplaceEditorProvider.ExpansionDirection> expansionDirections;
    private KeyListener keyListener;
    private FocusListener focusListener;
    private DocumentListener documentListener;

    public TextAreaInplaceEditorProvider(TextFieldInplaceEditor editor, EnumSet<InplaceEditorProvider.ExpansionDirection> expansionDirections) {
        this.editor = editor;
        this.expansionDirections = expansionDirections;
    }

    static WidgetAction create(TextFieldInplaceEditor editor) {
        return ActionFactory.createInplaceEditorAction(new TextAreaInplaceEditorProvider(editor, EnumSet.of(ExpansionDirection.BOTTOM)));
    }

    public JTextArea createEditorComponent(EditorController controller, Widget widget) {
        if (!editor.isEnabled(widget)) {
            return null;
        }
        JTextArea area = new JTextArea(editor.getText(widget));
        area.setFont(widget.getFont());
//        area.setForeground(widget.getForeground());
//        area.setBackground(widget.getBackground() instanceof Color ? (Color) widget.getParentWidget().getBackground() : Color.WHITE);
        area.setColumns(20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.selectAll();
        Insets insets = widget.getBorder() == null ? new Insets(0, 0, 0, 0) : widget.getBorder().getInsets();
//        area.setBorder(BorderFactory.createMatteBorder(insets.top, insets.left, insets.right, insets.bottom, UIManager.getColor("controlShadow")));
        area.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.right, insets.bottom));

        Scene scene = widget.getScene();
        double zoomFactor = scene.getZoomFactor();
        Font font = widget.getFont();
        font = font.deriveFont((float) (font.getSize2D() * zoomFactor));
        area.setFont(font);
        return area;
    }

    public void notifyOpened(final EditorController controller, Widget widget, JTextArea area) {
        area.setMinimumSize(widget.getPreferredSize());
        area.setFont(widget.getFont());

        Scene scene = widget.getScene();
        double zoomFactor = scene.getZoomFactor();
        Font font = widget.getFont();
        font = font.deriveFont((float) (font.getSize2D() * zoomFactor));
        area.setFont(font);
        area.setForeground(widget.getForeground());
        area.setColumns(20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        keyListener = new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case KeyEvent.VK_ESCAPE:
                        e.consume();
                        controller.closeEditor(false);
                        break;
                    case KeyEvent.VK_ENTER:
                        e.consume();
                        controller.closeEditor(true);
                        break;
                }
            }
        };
        focusListener = new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                controller.closeEditor(true);
            }
        };
        documentListener = new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                controller.notifyEditorComponentBoundsChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                controller.notifyEditorComponentBoundsChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                controller.notifyEditorComponentBoundsChanged();
            }
        };
        area.addKeyListener(keyListener);
        area.addFocusListener(focusListener);
        area.getDocument().addDocumentListener(documentListener);
        area.selectAll();
    }

    public void notifyClosing(EditorController controller, Widget widget, JTextArea editor, boolean commit) {
        editor.getDocument().removeDocumentListener(documentListener);
        editor.removeFocusListener(focusListener);
        editor.removeKeyListener(keyListener);
        if (commit) {
            this.editor.setText(widget, editor.getText());
            if (widget != null) {
                widget.getScene().validate();
            }
        }
    }

    public Rectangle getInitialEditorComponentBounds(EditorController controller, Widget widget, JTextArea editor, Rectangle viewBounds) {
        Rectangle widgetBoundsInView = widget.getScene().convertSceneToView(widget.convertLocalToScene(widget.getBounds()));
        String prototype = widget instanceof LabelWidget ? ((LabelWidget) widget).getLabel() : null;
        if (prototype == null || prototype.length() < 10) {
            prototype = "000000000";
        }
        int protoWidth = editor.getFontMetrics(editor.getFont()).stringWidth(prototype) + editor.getInsets().left + editor.getInsets().right;
        protoWidth *= widget.getScene().getZoomFactor();
        widgetBoundsInView.width = Math.max(widgetBoundsInView.width, protoWidth);
        return widgetBoundsInView;
    }

    public EnumSet<ExpansionDirection> getExpansionDirections(EditorController controller, Widget widget, JTextArea editor) {
        return expansionDirections;
    }
}
