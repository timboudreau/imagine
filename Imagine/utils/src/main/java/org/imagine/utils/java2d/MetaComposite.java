package org.imagine.utils.java2d;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Merges multiple Composites
 *
 * @author Tim Boudreau
 */
final class MetaComposite implements Composite {

    private final Composite[] composites;

    public MetaComposite(Composite... composites) {
        this.composites = composites;
    }

    static MetaComposite combine(Composite a, Composite b) {
        if (!(a instanceof MetaComposite) && !(b instanceof MetaComposite)) {
            return new MetaComposite(a, b);
        }
        List<Composite> all = new ArrayList<>();
        for (Composite c : new Composite[]{a, b}) {
            if (c instanceof MetaComposite) {
                MetaComposite mc = (MetaComposite) c;
                all.addAll(Arrays.asList(mc.composites));
            } else {
                all.add(c);
            }
        }
        return new MetaComposite(all.toArray(new Composite[all.size()]));
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
