/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import com.mastfrog.function.BooleanConsumer;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.openide.util.WeakSet;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
public class MainWindowActivation {

    private static MainWindowListener listener = new MainWindowListener();

    private MainWindowActivation() {
        throw new AssertionError();
    }

    public static boolean isMainWindowActive() {
        return listener.mainWindowIsActive();
    }

    public static boolean listen(BooleanConsumer consumer) {
        return listener.listen(consumer);
    }

    public static void unlisten(BooleanConsumer consumer) {
        listener.unlisten(consumer);
    }

    static class MainWindowListener extends WindowAdapter {

        private WeakSet<BooleanConsumer> listeners = new WeakSet<>();
        private boolean listeningToMainWindow;
        private boolean active;
        private static BooleanConsumer[] EMPTY = new BooleanConsumer[0];

        boolean mainWindowIsActive() {
            if (listeningToMainWindow) {
                return active;
            }
            return WindowManager.getDefault().getMainWindow().isActive();
        }

        boolean listen(BooleanConsumer consumer) {
            boolean result = ensureListeningOnMainWindow();
            listeners.add(consumer);
            return result;
        }

        void unlisten(BooleanConsumer consumer) {
            listeners.remove(consumer);
        }

        private boolean ensureListeningOnMainWindow() {
            if (!listeningToMainWindow) {
                listeningToMainWindow = true;
                Frame win = WindowManager.getDefault().getMainWindow();
                win.addWindowListener(this);
                active = win.isActive();
            }
            return active;
        }

        private void stopListeningOnMainWindow() {
            if (listeningToMainWindow) {
                listeningToMainWindow = false;
                WindowManager.getDefault().getMainWindow().removeWindowListener(this);
            }
        }

        private void dispatchToListeners(boolean val) {
            active = val;
            BooleanConsumer[] consumers = listeners.toArray(EMPTY);
            if (consumers.length == 0) {
                stopListeningOnMainWindow();
            }
            for (int i = 0; i < consumers.length; i++) {
                consumers[i].accept(val);
            }
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            dispatchToListeners(false);
        }

        @Override
        public void windowActivated(WindowEvent e) {
            dispatchToListeners(true);
        }

    }
}
