package org.netbeans.paint.layersui;

import java.awt.BorderLayout;
import net.dev.java.imagine.spi.tools.Customizer;
import net.dev.java.imagine.spi.tools.CustomizerProvider;
import net.java.dev.imagine.api.image.Layer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
@TopComponent.Description(preferredID = "LayerCustomizerTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "layerCustomizerMode", openAtStartup = true)
@ActionID(category = "Window", id = "org.netbeans.paint.layersui.LayerCustomizerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_LayersCustomizerAction",
preferredID = "LayersCustomizerTopComponent")
public class LayerCustomizerTopComponent extends TopComponent {

    private final Lookup.Result<Layer> layerResult =
            Utilities.actionsGlobalContext().lookupResult(Layer.class);
    private final LL ll = new LL();

    public LayerCustomizerTopComponent() {
        layerResult.addLookupListener(ll);
        ll.resultChanged(null);
        setLayout(new BorderLayout());
    }

    public String preferredID() {
        return getClass().getSimpleName();
    }

    public void open() {
        //XXX fix the metadata
        Mode m = WindowManager.getDefault().findMode("layerCustomizerMode");
        if (m != null) {
            m.dockInto(this);
        }
        super.open();
    }

    private void setCustomizer(Customizer<?> c) {
        setDisplayName(c.getName());
        removeAll();
        add(c.getComponent(), BorderLayout.CENTER);
        if (!isOpened()) {
            open();
        }
        requestVisible();
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    protected void componentClosed() {
        removeAll();
        super.componentClosed();
    }

    private final class LL implements LookupListener {

        @Override
        public void resultChanged(LookupEvent le) {
            boolean found = false;
            for (Layer l : layerResult.allInstances()) {
                CustomizerProvider p = l.getLookup().lookup(CustomizerProvider.class);
                if (p != null) {
                    setCustomizerProvider(p);
                    found = true;
                    break;
                }
            }
            if (!found) {
                removeAll();
                close();
            }
        }
    }

    private void setCustomizerProvider(CustomizerProvider p) {
        Customizer<?> c = p.getCustomizer();
        setCustomizer(c);
    }
}
