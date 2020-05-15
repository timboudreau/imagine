package org.imagine.vector.editor.ui.palette;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Window",
        id = "org.imagine.vector.editor.ui.palette.OpenPaintPaletteAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenPaintPaletteAction",
        iconBase = "org/imagine/vector/editor/ui/fills.svg"
)
@ActionReferences({
    @ActionReference(path = "Menu/Window", position = 10455),
    @ActionReference(path = "Shortcuts", name = "OS-P")
})
@Messages("CTL_OpenPaintPaletteAction=Paint Palette")
public final class OpenPaintPaletteAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        PaintsPaletteTC tc = PaintsPaletteTC.getInstance();
        if (tc != null) {
            tc.open();
            tc.requestActive();
        }
    }
}
