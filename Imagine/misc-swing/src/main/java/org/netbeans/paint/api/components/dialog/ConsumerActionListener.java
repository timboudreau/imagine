/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
final class ConsumerActionListener implements ActionListener {

    private final Consumer<ButtonMeaning> consumer;

    public ConsumerActionListener(Consumer<ButtonMeaning> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (ButtonMeaning m : ButtonMeaning.values()) {
            if (m.name().equals(e.getActionCommand())) {
                consumer.accept(m);
                break;
            }
        }
    }

}
