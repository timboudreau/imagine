/*
 * Proxy.java
 *
 * Created on November 6, 2006, 1:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.vector;

/**
 * A Primitive which proxies another primitive.  In practice useful mainly for
 * visual primitives (Vectors, Volumes, Strokables).
 *
 * @author Tim Boudreau
 */
public interface Proxy {
    Primitive getProxiedPrimitive();

    default Primitive unwind() {
        Proxy p = this;
        Primitive result = null;
        while (p instanceof Proxy) {
            result = p.getProxiedPrimitive();
            if (result instanceof Proxy) {
                p = (Proxy) result;
            } else {
                break;
            }
        }
        return result;
    }
}
