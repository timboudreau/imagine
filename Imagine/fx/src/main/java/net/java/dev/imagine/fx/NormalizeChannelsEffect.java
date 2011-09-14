/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.fx;

import java.awt.image.BufferedImageOp;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Effect.class)
public class NormalizeChannelsEffect extends AbstractOpEffect {
    public NormalizeChannelsEffect() {
        super ("NORMALIZE_CHANNELS");
    }

    @Override
    protected BufferedImageOp getOp() {
        return new RemoveGrayFilter();
    }
    
}
