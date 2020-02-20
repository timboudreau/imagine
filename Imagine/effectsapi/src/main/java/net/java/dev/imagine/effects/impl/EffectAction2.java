/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.effects.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import net.java.dev.imagine.effects.api.Effect;
import net.java.dev.imagine.effects.api.EffectReceiver;
import net.java.dev.imagine.effects.api.Preview;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 *
 * @author Tim Boudreau
 */
final class EffectAction2<In, Out> extends AbstractAction implements Lookup.Provider, LookupListener {

    private final Effect<In, Out> effect;
    private final Lookup target;
    private final Lookup.Result<EffectReceiver> result;

    EffectAction2(Effect<In, Out> effect, Lookup target) {
        this.effect = effect;
        this.target = target;
        putValue(NAME, effect.getDisplayName());
        putValue(SHORT_DESCRIPTION, effect.getDescription());
        result = target.lookupResult(EffectReceiver.class);
        result.addLookupListener(this);
        resultChanged(null);
    }

    private EffectReceiver<Out> receiver() {
        for (EffectReceiver<?> receiver : result.allInstances()) {
            if (effect.canApply(receiver)) {
                EffectReceiver<Out> result = receiver.as(effect.outputType());
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public void resultChanged(LookupEvent le) {
        setEnabled(receiver() != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        EffectReceiver<Out> receiver = receiver();
        if (receiver == null) {
            System.out.println("null receiver");
            return;
        }
        Preview<?, In, ?> preview = effect.createPreview(this);
        Customizer<In> c = Customizers.getCustomizer(effect.parameterType(), effect.getName());
        if (preview != null || c != null) {
            In param = effect.createInitialParam();
            Supplier<In> paramSupplier;
            if (c != null) {
                paramSupplier = c::get;
            } else {
                paramSupplier = () -> param;
            }
            JPanel panel = new JPanel(new BorderLayout());
            if (c != null) {
                panel.add(c.getComponent(), BorderLayout.CENTER);
            }
            if (preview != null) {
                PreviewComponent pc = new PreviewComponent(preview, paramSupplier);
                panel.add(pc, c == null ? BorderLayout.CENTER : BorderLayout.NORTH);
            }
            Object dlgResult = DialogDisplayer.getDefault().notify(new NotifyDescriptor(panel, effect.getDisplayName(), NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.PLAIN_MESSAGE, new Object[]{NotifyDescriptor.OK_OPTION, NotifyDescriptor.CANCEL_OPTION}, NotifyDescriptor.OK_OPTION));
            if (NotifyDescriptor.OK_OPTION.equals(dlgResult)) {
                receiver.apply(effect, paramSupplier.get());
            }
        }
    }

    static final class PreviewComponent<T> extends JComponent {

        private final Preview preview;
        private BufferedImage lastImg;
        private T lastValue;
        private final Supplier<T> supp;
        private Dimension lastSize = new Dimension();

        public PreviewComponent(Preview<?, T, ?> preview, Supplier<T> supp) {
            this.preview = preview;
            this.supp = supp;
        }

        public Dimension getPreferredSize() {
            return new Dimension(400, 300);
        }

        private void updateCached(T value, Dimension size) {
            if (!Objects.equals(value, lastValue) || !Objects.equals(lastSize, size)) {
                lastValue = value;
                lastSize = size;
                if (lastImg != null) {
                    lastImg.flush();
                }
                lastImg = preview.createPreview(size, value);
            }
        }

        @Override
        public void paint(Graphics gr) {
            Dimension sz = getSize();
            if (sz.width == 0 || sz.height == 0) {
                return;
            }
            Graphics2D g = (Graphics2D) gr;
            T val = supp.get();
            updateCached(val, sz);
            if (lastImg != null) {
                g.drawRenderedImage(lastImg, null);
            }
        }
    }

    @Override
    public Lookup getLookup() {
        return target;
    }
}
