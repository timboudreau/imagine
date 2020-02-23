/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.key;

/**
 * The "type" of a paint key for purposes of describing to the user
 * (may be the underlying type of a paint key wrapped in a texture).
 *
 * @see StandardPaintKeyKinds
 * @author Tim Boudreau
 */
public interface PaintKeyKind {

    /**
     * The kind name
     *
     * @return
     */
    String kindName();

    /**
     * Programmatic name
     * @return The name
     */
    String name();

    default String description() {
        return "";
    }
    
    default boolean is(PaintKeyKind kind) {
        return name().equals(kind.name());
    }

    default PaintKeyKind withDescription(String desc) {
        return new PaintKeyKind() {
            @Override
            public String kindName() {
                return PaintKeyKind.this.kindName();
            }

            @Override
            public String name() {
                return PaintKeyKind.this.name();
            }

            @Override
            public String description() {
                return desc;
            }
        };
    }
}
