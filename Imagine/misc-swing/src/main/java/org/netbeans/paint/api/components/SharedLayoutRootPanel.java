/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.geom.AffineTransform;
import javax.swing.JPanel;

/**
 *
 * @author Tim Boudreau
 */
public class SharedLayoutRootPanel extends JPanel implements SharedLayoutData {

    private final SharedLayoutData data = new DefaultSharedLayoutData();

    public SharedLayoutRootPanel() {
        super(new VerticalFlowLayout());
        setBorder(new FlexEmptyBorder());
    }

    public SharedLayoutRootPanel(double scaleFonts) {
        System.out.println("scale fonts " + scaleFonts);
        setUI(new FontManagingPanelUI(
                AffineTransform.getScaleInstance(scaleFonts, scaleFonts)));
        setLayout(new VerticalFlowLayout());
        setBorder(new FlexEmptyBorder());
    }

    @Override
    public int xPosForColumn(int column) {
        return data.xPosForColumn(column);
    }

    @Override
    public void register(LayoutDataProvider p) {
        data.register(p);
    }

    @Override
    public void unregister(LayoutDataProvider p) {
        data.unregister(p);
    }

    @Override
    public void expanded(LayoutDataProvider p, boolean state) {
        data.expanded(p, state);
    }
}
