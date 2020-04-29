/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.paint.effectsui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.spi.effects.Effect;
import net.dev.java.imagine.spi.effects.Effect.BufferedImageOpApplicator;
import net.dev.java.imagine.spi.effects.EffectRecipient;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.Utilities;

/**
 *
 * @author Timothy Boudreau
 */
public class EffectsMenu extends JMenu {
    private static final RequestProcessor RP = new RequestProcessor("Effect Preview", 1);
    
    /** Creates a new instance of EffectsMenu */
    public EffectsMenu() {
	setText (NbBundle.getMessage (EffectsMenu.class,
		"MENU_Effects")); //NOI18N
    }
    
    public void addNotify() {
	super.addNotify();
	initialize();
    }
    
    public void removeNotify() {
	super.removeNotify();
	removeAll();
    }
    
    Lookup.Result fxLookup;
    private void initialize() {
	fxLookup = Lookup.getDefault().lookup (
		new Lookup.Template(Effect.class));
	
	for (Iterator i=fxLookup.allInstances().iterator(); i.hasNext();) {
	    EffectAction effa = new EffectAction ((Effect) i.next());
	    JMenuItem item = new JMenuItem(effa);
	    effa.putValue(KEY_MENUITEM, item);
	    add (item);
	}
    }
    
    private static final String KEY_MENUITEM = "menuitem";
    private class EffectAction extends GenericContextSensitiveAction <EffectRecipient> {
        private final Effect effect;
        public EffectAction (Effect effect) {
            super (EffectRecipient.class);
            this.effect = effect;
            setDisplayName (effect.getName());
        }

        @Override
        protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
            boolean result = false;
            if (EffectRecipient.class == clazz) {
                for (T obj : coll) {
                    EffectRecipient r = (EffectRecipient) obj;
                    boolean can = r.canApplyEffects();
                    switch (effect.type()) {
                        case COMPOSITE:
                            result |= can && r.canApplyComposite();
                            break;
                        case BUFFERED_IMAGE_OP:
                            result |= can && r.canApplyBufferedImageOp();
                            break;
                        default:
                            throw new AssertionError(effect.type());
                    }
                }
            }
            return result;
        }

        @Override
        protected void performAction(EffectRecipient recipient) {
	    Effect.Applicator applicator = effect.getApplicator();
            assert ((applicator instanceof BufferedImageOpApplicator) == (effect.type() == Effect.Type.BUFFERED_IMAGE_OP)) : 
                    "Mismatch: " + effect.type() + " " + applicator;
	    Layer applyTo = 
                    Utilities.actionsGlobalContext().lookup (Layer.class);
            
            assert applyTo != null;
	    JComponent customizer = applicator.canPreview() && recipient instanceof Surface ? 
		new PreviewContainer (applyTo, applicator) : 
		applicator.getCustomizer();
	    
	    if (customizer != null) {
		String title = NbBundle.getMessage (EffectsMenu.class,
			"TTL_EffectDlg", effect.getName()); //NOI18N
		DialogDescriptor dlg = new DialogDescriptor (customizer,
			title, true, DialogDescriptor.OK_CANCEL_OPTION,
			DialogDescriptor.OK_OPTION, null);
		
		CL cl = new CL(dlg);
		applicator.addChangeListener (cl);
		
		Object okCancel = DialogDisplayer.getDefault().notify(dlg);
		applicator.removeChangeListener(cl);
		
		if ( okCancel != DialogDescriptor.OK_OPTION) {
		    return;
		}
	    }
	    assert applicator.canApply();
            if (applicator instanceof Effect.BufferedImageOpApplicator) {
                Selection sel = applyTo.getLookup().lookup(Selection.class);
                Shape clip = null;
                if (sel != null) {
                    clip = sel.asShape();
                }
                BufferedImageOp op = ((Effect.BufferedImageOpApplicator) applicator).getOp(clip == null ? recipient.getSize() : clip.getBounds().getSize());
                recipient.applyBufferedImageOp(op, clip);
            } else {
                Composite composite = applicator.getComposite();
                Selection sel = applyTo.getLookup().lookup(Selection.class);
                Shape clip = null;
                if (sel != null) {
                    clip = sel.asShape();
                }
                recipient.applyComposite(composite, clip);
            }
	}
    }
    /*
    private class EffectAction extends AppContextSensitiveAction {
	private final Effect effect;
	EffectAction (Effect effect) {
	    super (Picture.class);
	    this.effect = effect;
	    setDisplayName (effect.getName());
	}
	
	protected boolean shouldEnable (Object target) {
	    Picture l = (Picture) target;
	    return l != null && l.getActiveLayer() != null;
	}
	
	protected void performAction(Object o) {
	    Effect.Applicator applicator = effect.getApplicator();
	    Picture currentPicture = (Picture) o;
	    Layer applyTo = currentPicture.getActiveLayer();
	    
	    JComponent customizer = applicator.canPreview() ? 
		new PreviewContainer (applyTo, applicator) : 
		applicator.getCustomizer();
	    
	    if (customizer != null) {
		String title = NbBundle.getMessage (EffectsMenu.class,
			"TTL_EffectDlg", effect.getName()); //NOI18N
		DialogDescriptor dlg = new DialogDescriptor (customizer,
			title, true, DialogDescriptor.OK_CANCEL_OPTION,
			DialogDescriptor.OK_OPTION, null);
		
		CL cl = new CL(dlg);
		applicator.addChangeListener (cl);
		
		Object okCancel = DialogDisplayer.getDefault().notify(dlg);
		applicator.removeChangeListener(cl);
		
		if ( okCancel != DialogDescriptor.OK_OPTION) {
		    return;
		}
	    }
	    assert applicator.canApply();
	    
	    Composite composite = applicator.getComposite();
	    applyTo.getSurface().applyComposite(composite, null);
	}
	
	private JMenuItem getItem() {
	    return (JMenuItem) getValue (KEY_MENUITEM);
	}
    }
     */ 
    private static class CL implements ChangeListener {
	DialogDescriptor dlg;
	CL (DialogDescriptor dlg) {
	    this.dlg = dlg;
	}

	public void stateChanged(ChangeEvent e) {
	    Effect.Applicator appl = (Effect.Applicator) e.getSource();
	    boolean enableOK = appl.canApply();
	    if (enableOK) {
		dlg.setClosingOptions(new Object[] {
		    DialogDescriptor.OK_OPTION,
		    DialogDescriptor.CANCEL_OPTION,
		});
	    } else {
		dlg.setClosingOptions( new Object[] { 
		    DialogDescriptor.CANCEL_OPTION,
		});
	    }
	}
    }
    
    private static final class PreviewContainer extends JPanel {
	public PreviewContainer(Layer layer, Effect.Applicator applicator) {
	    setLayout (new BorderLayout());
	    JPanel lower = new JPanel();
	    lower.setLayout (new FlowLayout());
	    lower.add (new PreviewComponent (layer, applicator));
	    add (lower, BorderLayout.SOUTH);
	    add (applicator.getCustomizer(), BorderLayout.CENTER);
	}
    }
    
    private static final class PreviewComponent extends JComponent implements ChangeListener, Runnable {
	private Layer layer;
	private Effect.Applicator applicator;
	public PreviewComponent (Layer layer, Effect.Applicator applicator) {
	    setBorder (BorderFactory.createLoweredBevelBorder());
	    this.layer = layer;
	    this.applicator = applicator;
	}
	
	public void addNotify() {
	    super.addNotify();
	    applicator.addChangeListener (this);
	    stateChanged(null);
	}
	
	public void removeNotify() {
	    super.removeNotify();
	    applicator.removeChangeListener(this);
            t.cancel();
	}
	
	
	public Dimension getPreferredSize() {
	    Insets ins = getInsets();
	    return new Dimension (320 + ins.left + ins.right, 200 + ins.top + 
		    ins.bottom);
	}
	
	private BufferedImage backingImage = null;
	public void paint(Graphics g) {
	    g.setColor (Color.WHITE);
	    g.fillRect (0, 0, getWidth(), getHeight());
	    BufferedImage img;
	    synchronized (this) {
		img = backingImage;
	    }
	    if (img == null) {
		return;
	    }
	    Graphics2D g2d = (Graphics2D) g;
	    Insets ins = getInsets();
	    g2d.drawRenderedImage(img, 
		    AffineTransform.getTranslateInstance(ins.left, ins.top));
	}
	
	private void buildBackingImage() {
	    //Cannot simultaneously scale and apply a composite, it will
	    //throw an InternalError.  So do it in two steps:
	    //Make a temporary image
	    BufferedImage temp = new BufferedImage (320, 200, 
		    GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);

	    Graphics2D imageGr = (Graphics2D) temp.createGraphics();
	    //Paint a thumbnail into that
	    layer.paint (RenderingGoal.THUMBNAIL, imageGr, new Rectangle (0, 0, 320, 200), false, true, Zoom.ONE_TO_ONE,
                        AspectRatio.create(new Dimension(temp.getWidth(), temp.getHeight())));
	    //Now recreate our back buffer
	    BufferedImage back = new BufferedImage (320, 200, 
		    GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);

	    imageGr = (Graphics2D) back.createGraphics();
            if (applicator instanceof Effect.BufferedImageOpApplicator) {
                BufferedImageOp op = ((Effect.BufferedImageOpApplicator) applicator).getOp(new Dimension(temp.getWidth(), temp.getHeight()));
                temp = op.filter(temp, null);
                imageGr.drawRenderedImage(temp, 
                        AffineTransform.getTranslateInstance(0,0));
            } else {
                //Set the composite
                Composite comp = imageGr.getComposite();
                imageGr.setComposite (applicator.getComposite());
                //And copy the temp image into it, applying our effect
                imageGr.drawRenderedImage(temp, 
                        AffineTransform.getTranslateInstance(0,0));
                imageGr.setComposite(comp);
            }
	    synchronized (this) {
		this.backingImage = back;
	    }
	}
	
	public void run() {
	    buildBackingImage();
	    repaint();
	}
	
	Task t = RP.create(this);
	public void stateChanged(ChangeEvent e) {
            t.setPriority(Thread.MAX_PRIORITY);
	    t.schedule(30);
	}
    }
}
