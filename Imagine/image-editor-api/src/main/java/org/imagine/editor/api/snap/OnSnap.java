/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.editor.api.snap;

/**
 *
 * @author Tim Boudreau
 */
public interface OnSnap<T> {

    void onSnap(SnapPoint<T> x, SnapPoint<T> y);
}
