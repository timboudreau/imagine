/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi;

import java.awt.Shape;

/**
 *
 * @author Tim Boudreau
 */
interface Link {

    String destination();

    Shape bounds();

}
