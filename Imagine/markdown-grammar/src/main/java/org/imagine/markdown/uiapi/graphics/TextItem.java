/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi.graphics;

/**
 *
 * @author Tim Boudreau
 */
public interface TextItem extends PaintItem {

    String text();

    boolean contains(float x, float y);

}
