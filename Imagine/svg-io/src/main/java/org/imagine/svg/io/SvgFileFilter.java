/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.svg.io;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Tim Boudreau
 */
final class SvgFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isHidden()) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        if (!f.canWrite()) {
            return false;
        }
        if (f.getName().toLowerCase().endsWith(".svg")) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return Bundle.SVG_FILES();
    }

}
