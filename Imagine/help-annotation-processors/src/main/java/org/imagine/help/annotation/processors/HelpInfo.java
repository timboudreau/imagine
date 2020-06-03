package org.imagine.help.annotation.processors;

import com.mastfrog.util.collections.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import javax.lang.model.element.Element;

// Make these sortable, so generation order is consistent for repeatable builds

class HelpInfo implements Comparable<HelpInfo>, Iterable<OneLocaleInfo> {

    final String id;
    final List<OneLocaleInfo> locales = new ArrayList<>();
    final Map<String, Set<OneLocaleInfo>> infosByLanguage = CollectionUtils.supplierMap(TreeSet::new);
    final Set<Element> elements = new HashSet<>();
    private final boolean noIndex;
    final Set<String> related;
    private final boolean publyc;

    public HelpInfo(String id, boolean noIndex, List<String> related, boolean publyc) {
        this.id = id;
        this.noIndex = noIndex;
        this.related = new LinkedHashSet<>(related);
        this.publyc = publyc;
    }

    public boolean generatePublicClass() {
        return publyc;
    }

    public boolean isIndexable() {
        return !noIndex;
    }

    public Iterator<OneLocaleInfo> iterator() {
        Collections.sort(locales);
        return locales.iterator();
    }

    Element[] originatingElements() {
        return elements.toArray(new Element[elements.size()]);
    }

    void visitLanguagesAndInfos(BiConsumer<String, Set<OneLocaleInfo>> c) {
        infosByLanguage.forEach((lang, locs) -> {
            if (!locs.isEmpty()) {
                // can happen with SupplierMap in theory
                c.accept(lang, locs);
            }
        });
    }

    public OneLocaleInfo fallback() {
        for (OneLocaleInfo ifo : locales) {
            if ("en".equals(ifo.language) && "US".equals(ifo.country)) {
                return ifo;
            }
        }
        for (OneLocaleInfo ifo : locales) {
            if ("".equals(ifo.country)) {
                return ifo;
            }
        }
        for (OneLocaleInfo ifo : locales) {
            if ("en".equals(ifo.language)) {
                return ifo;
            }
        }
        return null;
    }

    OneLocaleInfo add(Element element, String locale, String variant, String text, String topic, List<String> keywords) {
        elements.add(element);
        for (OneLocaleInfo info : locales) {
            if (info.language.equals(locale) && info.country.equals(variant)) {
                return null;
            }
        }
        OneLocaleInfo result = new OneLocaleInfo(element, locale, variant, text, topic, keywords);
        locales.add(result);
        infosByLanguage.get(locale).add(result);
        return result;
    }

    @Override
    public int compareTo(HelpInfo o) {
        return id.compareTo(o.id);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.id);
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
        final HelpInfo other = (HelpInfo) obj;
        return Objects.equals(this.id, other.id);
    }
}
