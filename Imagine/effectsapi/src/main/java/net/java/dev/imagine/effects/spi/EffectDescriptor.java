package net.java.dev.imagine.effects.spi;

import org.openide.util.HelpCtx;

/**
 *
 * @author Tim Boudreau
 */
public interface EffectDescriptor {

    public String name();

    public String displayName();

    public String description();

    public HelpCtx helpCtx();

    public boolean canPreview();
}
