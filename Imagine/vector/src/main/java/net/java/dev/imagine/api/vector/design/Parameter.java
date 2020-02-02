/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 *
 * @author Tim Boudreau
 */
public interface Parameter {

    double get();

    void set(double val);

    String name();

    static Parameter from(String name, DoubleSupplier getter, DoubleConsumer setter) {
        return new Parameter() {
            @Override
            public double get() {
                return getter.getAsDouble();
            }

            @Override
            public void set(double val) {
                setter.accept(val);
            }

            @Override
            public String name() {
                return name;
            }

            public String toString() {
                return name();
            }
        };
    }
}
