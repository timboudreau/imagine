package org.imagine.help.annotation.processors;

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
class OneTopicInfo implements Comparable<OneTopicInfo> {

    final String language;
    final String country;
    final String topic;

    public OneTopicInfo(String language, String country, String topic) {
        this.language = language;
        this.country = country;
        this.topic = topic;
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
    public int compareTo(OneTopicInfo o) {
        return languageTag().compareTo(o.languageTag());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.language);
        hash = 79 * hash + Objects.hashCode(this.country);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OneTopicInfo other = (OneTopicInfo) obj;
        if (!Objects.equals(this.language, other.language)) {
            return false;
        }
        if (!Objects.equals(this.country, other.country)) {
            return false;
        }
        return true;
    }

}
