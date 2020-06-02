package org.imagine.help.annotation.processors;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Element;

/**
 *
 * @author Tim Boudreau
 */
class OneLocaleInfo implements Comparable<OneLocaleInfo> {

    final String language;
    final String country;
    final String text;
    final String topic;
    final List<String> keywords;
    final Element originatingElement;

    public OneLocaleInfo(Element originatingElement, String locale, String country, String text, String topic, List<String> keywords) {
        this.language = locale;
        this.country = country;
        this.text = text;
        this.topic = topic;
        this.keywords = keywords;
        this.originatingElement = originatingElement;
    }

    public String topic() {
        return topic;
    }

    public Set<String> keywords() {
        Set<String> result = new TreeSet<>();
        for (String kwd : keywords) {
            result.add(kwd.toLowerCase());
        }
        return result;
    }

    public String localeVariableName() {
        if (country == null || country.isEmpty()) {
            return language.toLowerCase();
        }
        return language.toLowerCase() + "_" + country;
    }

    public String languageTag() {
        if (country == null || country.isEmpty()) {
            return language;
        }
        return language + "-" + country;
    }

    @Override
    public int compareTo(OneLocaleInfo o) {
        return languageTag().compareTo(o.languageTag());
    }

}
