/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import java.text.DecimalFormat;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Tim Boudreau
 */
public final class ZoomSubmenuAction extends GenericContextSensitiveAction<Zoom> implements Presenter.Menu/*, Presenter.Toolbar */ {

    public ZoomSubmenuAction() {
        super("ACT_Zoom", Zoom.class);
        putValue("hideWhenDisabled", true);
    }

    @Override
    protected void performAction(Zoom t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenu menu = new JMenu();
        Mnemonics.setLocalizedText(menu, (String) getValue(NAME));
        float[] fracs = ZoomInAction.FRACTIONS;
        for (float frac : fracs) {
            menu.add(new ZoomOneAction(frac, lookup));
        }
        return menu;
    }

//    @Override
//    public Component getToolbarPresenter() {
//        JPanel pnl = new JPanel();
//        pnl.add(new JLabel((String) getValue(NAME)));
//        JComboBox box = new JComboBox();
//
//    }
//
    private static final class ZoomOneAction extends GenericContextSensitiveAction<Zoom> {

        private final float zoom;

        ZoomOneAction(float zoom, Lookup lookup) {
            super(lookup, Zoom.class);
            this.zoom = zoom;
            init();
        }

        ZoomOneAction(float zoom) {
            super(Zoom.class);
            this.zoom = zoom;
            init();
        }

        private void init() {
            int intValue = (int) (zoom * 10);
            DecimalFormat fmt = new DecimalFormat(NbBundle.getMessage(ZoomOneAction.class, "FMT_Zoom"));
            putValue(NAME, fmt.format(intValue));
        }

        @Override
        public Action createContextAwareInstance(Lookup lookup) {
            return new ZoomOneAction(zoom, lookup);
        }

        @Override
        protected void performAction(Zoom t) {
            t.setZoom(zoom);
        }
    }
}
