/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.nbutil;

import java.util.Set;
import org.imagine.editor.api.util.ActivationOrder;
import org.openide.util.WeakSet;
import org.openide.windows.TopComponent;

/**
 *
 * @author Tim Boudreau
 */
public abstract class ActivationOrderTopComponent extends TopComponent {

    private final Set<ActivationOrder<String>> activationRegistries
            = new WeakSet<>();
    private boolean dontUpdate;

    protected ActivationOrderTopComponent() {

    }

    protected void onComponentActivated() {

    }

    protected void onComponentClosed() {

    }

    public void register(ActivationOrder<String> order) {
        activationRegistries.add(order);
    }

    @Override
    protected final void componentActivated() {
        if (!dontUpdate) {
            for (ActivationOrder<String> ord : activationRegistries) {
                ord.activated(preferredID());
            }
        }
        onComponentActivated();
    }

    @Override
    protected final void componentClosed() {
        if (!dontUpdate) {
            for (ActivationOrder<String> ord : activationRegistries) {
                ord.prejudice(preferredID());
            }
        }
        onComponentClosed();
    }

    public void closeWithoutUpdateOrder() {
        withoutUpdate(this::close);
    }

    public void ensureOpenWithoutUpdateOrder(boolean requestVisible) {
        withoutUpdate(() -> {
            if (!isOpened()) {
                open();
            }
            if (requestVisible) {
                requestVisible();
            }
        });
    }

    protected void withoutUpdate(Runnable r) {
        dontUpdate = true;
        try {
            r.run();
        } finally {
            dontUpdate = false;
        }
    }

}
