/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.svg.io;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.nio.charset.StandardCharsets.UTF_8;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.image.RenderingGoal;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.imagine.editor.api.Zoom;
import org.imagine.nbutil.filechooser.FileChooserBuilder;
import org.imagine.nbutil.filechooser.FileKinds;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;

@ActionID(
        category = "File",
        id = "org.imagine.svg.io.ExportSVGAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportSVGAction"
)
@ActionReference(path = "Menu/File", position = 2000, separatorBefore = 1950, separatorAfter = 2050)
@Messages({
    "CTL_ExportSVGAction=Export SVG",
    "SVG_FILES=SVG Files",
    "EXPORT=Export",
    "USE_CSS=Use CSS",
    "ESCAPED=Escaped",
    "TEXT_AS_SHAPES=Text As Shapes",
    "EXPORT_SVG=Export SVG",
    "# {0} - The file name",
    "SAVED_SVG=SVG saved: {0}"
})
public final class ExportSVGAction implements ActionListener {

    private final Picture picture;

    public ExportSVGAction(Picture context) {
        this.picture = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        JPanel optionsPanel = new JPanel(new VerticalFlowLayout());
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JCheckBox useCssBox = new JCheckBox();
        Mnemonics.setLocalizedText(useCssBox, Bundle.USE_CSS());

        JCheckBox escapedBox = new JCheckBox();
        Mnemonics.setLocalizedText(escapedBox, Bundle.ESCAPED());

        JCheckBox textAsShapesBox = new JCheckBox();
        Mnemonics.setLocalizedText(textAsShapesBox, Bundle.TEXT_AS_SHAPES());

        optionsPanel.add(useCssBox);
        optionsPanel.add(escapedBox);
        optionsPanel.add(textAsShapesBox);

        JFileChooser chooser = new FileChooserBuilder(ExportSVGAction.class)
                .setFileFilter(new SvgFileFilter())
                .setFileHiding(true)
                .setFileKinds(FileKinds.FILES_ONLY)
                .setApproveText(Bundle.EXPORT())
                .setTitle(Bundle.EXPORT_SVG())
                .createFileChooser();
        chooser.setAccessory(optionsPanel);;
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(WindowManager.getDefault().getMainWindow())) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".svg")) {
                file = new File(file.getParent(), file.getName() + ".svg");
            }
            saveTo(file, useCssBox.isSelected(), escapedBox.isSelected(), textAsShapesBox.isSelected());
            picture.associateFile(file.toPath());
            StatusDisplayer.getDefault().setStatusText(Bundle.SAVED_SVG(file.getName()));
        }
    }

    private void saveTo(File file, boolean useCSS, boolean escaped, boolean textAsShapes) {
        DOMImplementation domImpl
                = SVGDOMImplementation.getDOMImplementation();

        // Create a document with the appropriate namespace
        SVGDocument document
                = (SVGDocument) domImpl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);

        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);

        ctx.setEmbeddedFontsOn(!textAsShapes);

        ExtSvgGraphics2D graphics = new ExtSvgGraphics2D(ctx, textAsShapes);

        graphics.setExtensionHandler(new GradientExtensionHandler());
        graphics.setSVGCanvasSize(picture.getSize());
        picture.paint(RenderingGoal.PRODUCTION, graphics, null, false, Zoom.ONE_TO_ONE);
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file), 1024), UTF_8)) {
            graphics.stream(writer, useCSS, false);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
