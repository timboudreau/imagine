/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api;

import java.awt.Dimension;

/**
 *
 * @author Tim Boudreau
 */
public interface ImageEditor {

    Dimension getAvailableSize();

    AspectRatio getPictureAspectRatio();

    Zoom getZoom();
}
