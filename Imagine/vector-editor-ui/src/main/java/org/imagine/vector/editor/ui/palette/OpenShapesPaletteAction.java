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
        id = "org.imagine.vector.editor.ui.palette.OpenShapesPaletteAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenShapesPaletteAction",
        iconBase = "org/imagine/vector/editor/ui/shapes.svg"
)
@ActionReferences({
    @ActionReference(path = "Menu/Window", position = 10450),
    @ActionReference(path = "Shortcuts", name = "OS-S")
})
@Messages("CTL_OpenShapesPaletteAction=Shapes Palette")
public final class OpenShapesPaletteAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ShapesPaletteTC tc = ShapesPaletteTC.getInstance();
        if (tc != null) {
            tc.open();
            tc.requestActive();
        }
    }
}
