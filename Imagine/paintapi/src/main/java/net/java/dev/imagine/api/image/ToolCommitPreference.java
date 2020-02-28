/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.image;

/**
 * Painting surfaces have different characteristics; if a tool is going to paint
 * a slew of shapes into a raster surface, it should just paint them. In the
 * case of a vector surface, it should collect all of the shapes, coalesce them
 * somehow (e.g. area) and commit one paint of one shape, to keep performance
 * reasonable.
 *
 * @author Tim Boudreau
 */
public enum ToolCommitPreference {

    JUST_PAINT,
    COLLECT_GEOMETRY
}
