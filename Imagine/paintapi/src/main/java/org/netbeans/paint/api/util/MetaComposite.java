package org.netbeans.paint.api.util;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Merges multiple Composites
 *
 * @author Tim Boudreau
 */
public final class MetaComposite implements Composite {
    private final Composite[] composites;
    
    public MetaComposite(Composite... composites) {
        this.composites = composites;
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new CC(srcColorModel, dstColorModel, hints);
    }
    
    class CC implements CompositeContext {
        private CompositeContext[] contexts;
        private final ColorModel srcColorModel;
        private final ColorModel dstColorModel;
        private final RenderingHints hints;
        private CC(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            this.srcColorModel = srcColorModel;
            this.dstColorModel = dstColorModel;
            this.hints = hints;
        }

        @Override
        public void dispose() {
            if (contexts != null) {
                for (CompositeContext c : contexts) {
                    c.dispose();
                }
            }
        }

        @Override
        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            contexts = new CompositeContext[composites.length];
            for (int i = 0; i < contexts.length; i++) {
                //XXX testme
                CompositeContext c;
                c = contexts[i] = composites[i].createContext(srcColorModel, dstColorModel, hints);
                c.compose(src, dstIn, dstOut);
                src = dstOut;
            }
        }
    }
    
}
