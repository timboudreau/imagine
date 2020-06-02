/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.helpimpl;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.imagine.help.api.annotations.Help;

/**
 *
 * @author Tim Boudreau
 */
@Help(id = "Slider", content = @Help.HelpText(
        value="This is a slider.\n\nYou can *_slide_* it!\n\nHow 'bout that?",
        topic = "Doing Things That Slide", keywords = {"slide", "shimmy", "shake"}))
public class Demo {

    @Help(id="Text", content=@Help.HelpText(value="A Text Component\n\nYou can type text here, like\n\n * Things you *dream* of\n"
            + " * Things you ~~_don't_ dream of~~\n * Things it would be _nice_ to dream of if you could, but can't\n\n"
            + "-------\nAnd other stuff like that, ya know, things with antidisestablishmentarianism and stuff?\n\nThat's all.",
            topic = "Writing Things", keywords = {"text", "stuff"}))
    private static final String Foo = "";

//    @Help(id = "Tree", content = @Help.HelpText("A Tree Of Stuff\n\nThis is a _tree_.\n\n-----\nHow about *that*?\n\n> Cool, huh? I think so.\n\n"))
    @Help(id = "Tree", content = @Help.HelpText("A single sentence with some longish words that could wrap but should not wrap terribly tightly I expect"))
    public static void main(String[] args) {
        Font f = UIManager.getFont("Label.font");
        f = f.deriveFont(AffineTransform.getScaleInstance(3, 3));
        UIManager.put("Label.font", f);
        UIManager.put("Tree.font", f);
        UIManager.put("Slider.font", f);
        UIManager.put("Window.font", f);
        UIManager.put("TextArea.font", f);
        UIManager.put("controlFont", f);

        JFrame frm = new JFrame("Demo");
        frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frm.setLayout(new BorderLayout());

        JTree tree = new JTree();
        frm.add(new JScrollPane(tree), BorderLayout.CENTER);

        JPanel pnl = new JPanel();
        JLabel sl1 = new JLabel("Slider");
        JSlider slid = new JSlider();
        pnl.add(sl1);
        pnl.add(slid);

        JTextArea area = new JTextArea("Some text here");
        JScrollPane pn = new JScrollPane(area);
        frm.add(pnl, BorderLayout.EAST);
        frm.add(pn, BorderLayout.WEST);

        HelpItems.Slider.attach(slid);
        HelpItems.Tree.attach(tree);
        HelpItems.Text.attach(area);



        frm.pack();
        frm.setVisible(true);
    }
}
