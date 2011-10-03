/*
 * CustomizerProvider.java
 *
 * Created on September 28, 2006, 4:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.dev.java.imagine.api.tool.aspects;

/**
 *
 * @author Tim Boudreau
 */
public interface CustomizerProvider {
    public Customizer<?> getCustomizer();
}
