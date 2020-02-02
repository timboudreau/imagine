/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.common;

import java.awt.Component;
import javax.swing.JLabel;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author eppleton
 */
@ServiceProvider(service = StatusLineElementProvider.class)
public class PositionStatusLineElementProvider implements StatusLineElementProvider {

    private JLabel statusLineLabel = new JLabel();

    public Component getStatusLineElement() {
        return statusLineLabel;
    }

    public void setStatus(String statusMessage) {
        statusLineLabel.setText(statusMessage);
    }
}
