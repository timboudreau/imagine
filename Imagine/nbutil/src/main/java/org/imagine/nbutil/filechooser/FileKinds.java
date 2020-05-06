/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.nbutil.filechooser;

import javax.swing.JFileChooser;

/**
 *
 * @author Tim Boudreau
 */
public enum FileKinds {

    DIRECTORIES_ONLY,
    FILES_ONLY,
    DIRECTORIES_AND_FILES;

    public boolean isDirectories() {
        return this != FILES_ONLY;
    }

    public boolean isFiles() {
        return this != DIRECTORIES_ONLY;
    }

    void configure(JFileChooser chooser) {
        switch(this) {
            case DIRECTORIES_ONLY :
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            case FILES_ONLY :
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            default :
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                break;
        }
    }
}
