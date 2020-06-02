/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.help.api.search;

import org.imagine.help.api.HelpItem;

/**
 *
 * @author Tim Boudreau
 */
public interface HelpSearchCallback {

    public boolean onMatch(String of, HelpItem item, String heading, String topic, float score, boolean isLast);

    default void onStart() {

    }

    default void onFinish() {
        
    }
}
