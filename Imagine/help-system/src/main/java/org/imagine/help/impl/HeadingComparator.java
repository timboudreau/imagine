/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.help.impl;

import java.util.Comparator;
import java.util.Locale;
import org.imagine.help.api.HelpItem;

/**
 *
 * @author Tim Boudreau
 */
public final class HeadingComparator implements Comparator<HelpItem> {

    private final Locale locale;

    public HeadingComparator(Locale locale) {
        this.locale = locale;
    }

    @Override
    public int compare(HelpItem o1, HelpItem o2) {
        return o1.heading(locale).compareToIgnoreCase(o2.heading(locale));
    }

}
