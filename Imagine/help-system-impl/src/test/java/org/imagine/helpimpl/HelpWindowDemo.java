package org.imagine.helpimpl;

import java.awt.Font;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.imagine.help.impl.HelpComponentManagerTrampoline;

/**
 *
 * @author Tim Boudreau
 */
public class HelpWindowDemo {

    public static void main(String[] args) {
        int sz = 18;
        System.setProperty("uiFontSize", Integer.toString(sz));
        System.setProperty("awt.useSystemAAFontSettings", "lcd_hrgb");
        Font f = new Font("Arimo", Font.PLAIN, sz);
        UIManager.put("controlFont", f);
        UIManager.put("Label.font", f);
        UIManager.put("List.font", f);
        UIManager.put("Window.font", f);
        UIManager.put("Frame.font", f);
        UIManager.put("Button.font", f);
        UIManager.put("CheckBox.font", f);
        UIManager.put("RadioButton.font", f);
        UIManager.put("TextField.font", f);
        UIManager.put("TabbedPane.font", f);
        UIManager.put("TextArea.font", f);

        HelpComponentManagerTrampoline.setIndices(() -> {
            return Arrays.asList(new org.imagine.help.api.HIndex(), new org.imagine.helpimpl.HIndex(),
                    new org.imagine.help.api.demo.HIndex());
        });

        HelpWindowComponent win = new HelpWindowComponent();
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(win);
        jf.pack();
        jf.setBounds(200, 200, 800, 600);
//        jf.pack();
        jf.setVisible(true);
    }
}
