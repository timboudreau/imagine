/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Paint;
import java.util.function.Supplier;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Attribute;

/**
 *
 * @author Tim Boudreau
 */
interface HibernatablePaintReference<T extends Paint> extends Supplier<T>, Hibernator, Attribute<T> {}
