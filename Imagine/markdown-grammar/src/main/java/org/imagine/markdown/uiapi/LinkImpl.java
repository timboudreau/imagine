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
class LinkImpl implements Link {

    final Shape bounds;
    final String url;

    public LinkImpl(Shape bounds, String url) {
        this.bounds = bounds;
        this.url = url;
    }

    public String destination() {
        return url;
    }

    public Shape bounds() {
        return bounds;
    }

}
