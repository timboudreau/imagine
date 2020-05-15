package org.imagine.svg.io;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.image.RenderingGoal;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.imagine.editor.api.Zoom;
import org.imagine.nbutil.filechooser.FileChooserBuilder;
import org.imagine.nbutil.filechooser.FileKinds;
import org.netbeans.paint.api.components.FlexEmptyBorder;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
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
    "USE_CSS=Use &CSS",
    "ESCAPED=&Escaped",
    "TEXT_AS_SHAPES=Text &As Shapes",
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
        optionsPanel.setBorder(new FlexEmptyBorder());

        JCheckBox useCssBox = new JCheckBox();
        Mnemonics.setLocalizedText(useCssBox, Bundle.USE_CSS());

        JCheckBox escapedBox = new JCheckBox();
        Mnemonics.setLocalizedText(escapedBox, Bundle.ESCAPED());

        JCheckBox textAsShapesBox = new JCheckBox();
        Mnemonics.setLocalizedText(textAsShapesBox, Bundle.TEXT_AS_SHAPES());

        Preferences prefs = NbPreferences.forModule(ExportSVGAction.class);

        useCssBox.setSelected(prefs.getBoolean("useCss", true));
        escapedBox.setSelected(prefs.getBoolean("escaped", false));
        textAsShapesBox.setSelected(prefs.getBoolean("textAsShapes", false));

        optionsPanel.add(useCssBox);
        optionsPanel.add(escapedBox);
        optionsPanel.add(textAsShapesBox);

        Path target = picture.associatedFile();
        if (target != null) {
            String name = target.getFileName().toString();
            if (!name.toLowerCase().endsWith(".svg")) {
                int ix = name.lastIndexOf('.');
                if (ix > 0) {
                    name = name.substring(0, ix);
                }
                target = target.getParent().resolve(name + ".svg");
            }
        }

        File file = new FileChooserBuilder(ExportSVGAction.class)
                .setFileFilter(new SvgFileFilter())
                .setInitialSelection(target)
                .confirmOverwrites()
                .forceExtension(".svg")
                .setFileHiding(true)
                .setFileKinds(FileKinds.FILES_ONLY)
                .setApproveText(Bundle.EXPORT())
                .setTitle(Bundle.EXPORT_SVG()).
                <JPanel>setAccessory(optionsPanel)
                .showSaveDialog();
        if (file != null) {
            saveTo(file, useCssBox.isSelected(), escapedBox.isSelected(), textAsShapesBox.isSelected());
            prefs.putBoolean("useCss", useCssBox.isSelected());
            prefs.putBoolean("escaped", escapedBox.isSelected());
            prefs.putBoolean("textAsShapes", textAsShapesBox.isSelected());
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

        ctx.setPrecision(12);
        
        ctx.setEmbeddedFontsOn(!textAsShapes);

        ExtSvgGraphics2D graphics = new ExtSvgGraphics2D(ctx, textAsShapes);

        graphics.setExtensionHandler(new GradientExtensionHandler());
        graphics.setSVGCanvasSize(pictureSize());
        picture.paint(RenderingGoal.PRODUCTION, graphics, null, false, Zoom.ONE_TO_ONE);
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file), 1024), UTF_8)) {
            graphics.stream(writer, useCSS, false);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private Dimension pictureSize() {
        Dimension base = picture.getSize();
        for (Layer layer : picture) {
            Rectangle r = layer.getBounds();
            base.width = Math.max(r.x + r.width, base.width);
            base.height = Math.max(r.y + r.height, base.height);
        }
        return base;
    }

}
