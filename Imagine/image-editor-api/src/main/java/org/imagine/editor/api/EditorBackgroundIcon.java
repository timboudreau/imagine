/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

/**
 *
 * @author Tim Boudreau
 */
final class EditorBackgroundIcon implements Icon {

    private final EditorBackground bg;
    private static final BasicStroke line = new BasicStroke(1);

    public EditorBackgroundIcon(EditorBackground bg) {
        this.bg = bg;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D gg = (Graphics2D) g;
        gg.setPaint(bg.getPaint());
        gg.fillRect(x, y, getIconWidth(), getIconHeight());
        gg.setStroke(line);
        gg.setColor(bg.midContrasting());
        gg.fillRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
    }

    @Override
    public int getIconWidth() {
        return bg.getPatternStride();
    }

    @Override
    public int getIconHeight() {
        return bg.getPatternStride();
    }

    static class Renderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            EditorBackground bg = (EditorBackground) value;
            setIcon(bg.icon());
            return result;
        }


    }

}
