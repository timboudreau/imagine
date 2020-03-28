/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.collections;

/**
 *
 * @author Tim Boudreau
 */
public interface CoordinateMapModifier<T> {

    public CoordinateMapModifier<T> add(double x, double y, T value);

    public CoordinateMapModifier<T> remove(double x, double y);

    public CoordinateMapModifier<T> move(double fromX, double fromY, double toX, double toY);

    public CoordinateMapModifier<T> set(double fromX, double fromY, T value);

    public void commit();
}
