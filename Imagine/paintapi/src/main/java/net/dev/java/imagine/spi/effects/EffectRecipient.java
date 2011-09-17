/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.spi.effects;

import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.image.BufferedImageOp;

/**
 *
 * @author Tim Boudreau
 */
public interface EffectRecipient {
    public boolean canApplyComposite();
    public boolean canApplyBufferedImageOp();
    public void applyComposite (Composite composite, Shape clip);
    public void applyBufferedImageOp (BufferedImageOp op, Shape clip);
    public Dimension getSize();
}
