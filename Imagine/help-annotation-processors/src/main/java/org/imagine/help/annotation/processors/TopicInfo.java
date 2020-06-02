/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.help.annotation.processors;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.imagine.help.annotation.processors.OneTopicInfo;

/**
 *
 * @author Tim Boudreau
 */
class TopicInfo implements Comparable<TopicInfo>, Iterable<OneTopicInfo> {

    final Element target;
    private final Set<OneTopicInfo> entries = new TreeSet<>();

    public TopicInfo(Element target) {
        this.target = target;
    }

    public String toString() {
        switch (target.getKind()) {
            case PACKAGE:
                PackageElement pe = (PackageElement) target;
                return pe.getQualifiedName().toString();
            case CLASS:
                TypeElement te = (TypeElement) target;
                return te.getQualifiedName().toString();
            default:
                throw new AssertionError("Unsupported kind " + target.getKind());
        }
    }

    @Override
    public int compareTo(TopicInfo o) {
        return toString().compareTo(o.toString());
    }

    public boolean add(String language, String country, String topic) {
        return entries.add(new OneTopicInfo(language, country, topic));
    }

    public OneTopicInfo forLanguage(String language, String country) {
        for (OneTopicInfo info : this) {
            if (language.equals(info.language) && country.equals(info.country)) {
                return info;
            }
        }
        for (OneTopicInfo info : this) {
            if (language.equals(info.language)) {
                return info;
            }
        }
        return null;
    }

    @Override
    public Iterator<OneTopicInfo> iterator() {
        return entries.iterator();
    }


}
