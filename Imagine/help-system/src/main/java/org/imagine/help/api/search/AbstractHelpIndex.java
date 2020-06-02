/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.help.api.search;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.imagine.help.api.HelpItem;
import org.imagine.help.impl.LevenshteinDistance;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractHelpIndex<H extends Enum<H> & HelpItem> extends HelpIndex {

    private final Class<H> type;
    private final Map<Locale, InternalIndex> indices = new HashMap<>();
    private volatile boolean initialized;
    private final H[] hs;
    protected final Logger log = Logger.getLogger(getClass().getName());

    {
        log.setLevel(Level.ALL);
    }

    public AbstractHelpIndex(Class<H> type) {
        this.type = type;
        hs = type.getEnumConstants();
    }

    protected abstract void init();

    @Override
    public String toString() {
        return getClass().getName() + "(" + type.getName() + ")";
    }

    protected String heading(H id, Locale locale) {
        return id.heading(locale);
    }

    protected String fullText(H id, Locale locale) {
        return id.asPlainText(locale);
    }

    protected void ensureInitialized() {
        if (!initialized) {
            synchronized (indices) {
                if (!initialized) {
                    init();
                }
            }
        }
    }

    private InternalIndex indexFor(Locale locale) {
        ensureInitialized();
        InternalIndex ix = indices.get(locale);
        if (ix == null) {
            List<InternalIndex> matches = new ArrayList<>(5);
            for (Map.Entry<Locale, InternalIndex> e : indices.entrySet()) {
                Locale loc = e.getKey();
                if (locale.getLanguage().equals(loc.getLanguage())) {
                    matches.add(e.getValue());
                }
            }
            if (matches.isEmpty()) {
                ix = new InternalIndex(locale);
            } else {
                Locale loc = Locale.forLanguageTag(locale.getLanguage());
                ix = new InternalIndex(loc);
                for (InternalIndex sub : matches) {
                    ix.coalesce(sub);
                }
            }
        }
        return ix;
    }

    protected void add(Locale locale, String topic, String constantName, String... keywords) {
        assert Thread.currentThread().holdsLock(indices);
        log.log(Level.FINEST, "Init-add to {0} for {1}: {2}", new Object[]{this,
            locale, constantName});
        InternalIndex ix = indices.get(locale);
        if (ix == null) {
            ix = new InternalIndex(locale);
            indices.put(locale, ix);
        }
        ix.add(topic, constantName, keywords);
    }

    @Override
    protected Set<H> allItems() {
        return EnumSet.allOf(type);
    }

    private H find(String helpId) {
        for (H h : hs) {
            if (h.name().equals(helpId)) {
                return h;
            }
        }
        return null;
    }

    protected String topic(Locale locale, H id) {
        return indexFor(locale).topicFor(id.name());
    }

    protected void fullTextSearch(Locale locale, boolean exact, String searchTerm,
            Set<HelpSearchConstraint> constraints, int maxResults, HelpSearchCallback callback) {
        log.log(Level.FINEST, "{0} perform full text in {1} for ''{2}''", new Object[]{
            this, searchTerm
        });
        boolean[] cancelled = new boolean[1];
        for (H h : hs) {
            String fullText = fullText(h, locale);
            collate(fullText, (word) -> {
                float score = exact ? word.equals(searchTerm)
                        ? 0F
                        : 1F
                        : LevenshteinDistance.score(searchTerm, word, false);
                if (score < THRESHOLD_SCORE) {
                    boolean keepGoing = callback.onMatch(searchTerm, h, heading(h, locale),
                            indexFor(locale).topicFor(h.name()), score, false);
                    if (!keepGoing) {
//                        System.out.println("cancelled");
                        cancelled[0] = true;
                    }
                    return false;
                }
                return true;
            });
            if (cancelled[0]) {
                break;
            }
        }
    }

    protected void keywordSearch(Locale locale, String searchTerm, Set<HelpSearchConstraint> constraints,
            int maxResults, HelpSearchCallback callback) {
        InternalIndex ix = indexFor(locale);
        Set<H> seen = EnumSet.noneOf(type);
        log.log(Level.FINEST, "{0} perform keyword search in {1} for ''{2}''", new Object[]{
            this, ix, searchTerm
        });
        ix.keywordMatches(searchTerm, constraints.contains(HelpSearchConstraint.EXACT),
                (helpId, topic, score) -> {
//                    System.out.println("ts " + helpId + " score " + score);
                    H id = find(helpId);
                    if (id != null && !seen.contains(id)) {
                        if (seen.size() >= maxResults) {
//                            System.out.println("already at max results, abort");
                            return false;
                        }
                        seen.add(id);
                        return callback.onMatch(searchTerm, id, heading(id, locale), topic,
                                score, initialized);
                    } else {
                        System.err.println("null constant on " + type.getName() + " for " + helpId);
                    }
                    return true;
                });
    }

    protected void topicSearch(Locale locale, String searchTerm, Set<HelpSearchConstraint> constraints,
            int maxResults, HelpSearchCallback callback) {
        InternalIndex ix = indexFor(locale);
        log.log(Level.FINEST, "{0} perform topic search in {1} for ''{2}''", new Object[]{
            this, ix, searchTerm
        });
        Set<H> seen = EnumSet.noneOf(type);
        ix.topicMatches(searchTerm, constraints.contains(HelpSearchConstraint.EXACT), (helpId, topic, score) -> {
//            System.out.println("ts " + helpId + " score " + score);
            H id = find(helpId);
            if (id != null && !seen.contains(id)) {
                seen.add(id);
                if (seen.size() >= maxResults) {
                    System.out.println("at max results, abort");
                    return false;
                }
                return callback.onMatch(searchTerm, id, heading(id, locale),
                        topic, score, initialized);
            } else {
                System.err.println("null constant on " + type.getName()
                        + " for " + helpId);
            }
            return true;
        });
    }

    @Override
    protected void runSearch(Locale locale, String searchTerm, Set<HelpSearchConstraint> constraints,
            int maxResults, HelpSearchCallback callback) {
        if (constraints.contains(HelpSearchConstraint.KEYWORD)) {
            keywordSearch(locale, searchTerm, constraints, maxResults, callback);
        } else if (constraints.contains(HelpSearchConstraint.TOPIC)) {
            topicSearch(locale, searchTerm, constraints, maxResults, callback);
        } else {
            fullTextSearch(locale, constraints.contains(HelpSearchConstraint.EXACT),
                    searchTerm, constraints, maxResults, callback);
        }
    }

    protected interface IndexSearcher {

        boolean match(String constant, String topic, float score);
    }

    protected boolean isStopWord(String word) {
        return word.length() <= 3;
    }

    protected void collate(String text, Predicate<String> consumer) {
        String[] words = text.split("\\s+");
        StringBuilder curr = new StringBuilder();
        outer:
        for (String word : words) {
            if (isStopWord(word)) {
                continue;
            }
            curr.setLength(0);
            for (int j = 0; j < word.length(); j++) {
                char c = word.charAt(j);
                boolean alphabetic = Character.isAlphabetic(c);
                if (alphabetic) {
                    curr.append(c);
                }
                if ((!alphabetic || j == word.length() - 1) && curr.length() > 0) {
                    String currentWord = curr.toString();
                    curr.setLength(0);
                    if (!consumer.test(currentWord)) {
                        break outer;
                    }
                }
            }
        }
    }

    private static final float THRESHOLD_SCORE = 0.45F;
    private static final float THRESHOLD_SCORE_WITH_EXACT_MATCHES = 0.2F;

    private class InternalIndex {

        private final Locale locale;
        private final Map<String, Set<String>> constantsForTopic
                = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, String> topicForConstant = new HashMap<>();
        private final Map<String, Set<String>> constantsForKeyword
                = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public InternalIndex(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String toString() {
            return "InternalIndex(" + locale + " " + constantsForTopic + ", "
                    + " and " + constantsForKeyword + ")";
        }

        void topicMatches(String searchTerm, boolean exact, IndexSearcher searcher) {
            for (Map.Entry<String, Set<String>> e : constantsForTopic.entrySet()) {
                String topic = e.getKey();
                collate(topic, (word) -> {
                    float score = LevenshteinDistance.score(searchTerm, word, false);
//                    System.out.println("topic match '" + searchTerm + "' to '" + word + "' - " + score);
                    if (exact ? score == 0F : score < THRESHOLD_SCORE) {
                        for (String id : e.getValue()) {
                            boolean notCancelled = searcher.match(id, topic, score);
                            if (!notCancelled) {
                                return false;
                            }
                        }
                        return false;
                    }
                    return true;
                });
            }
        }

        private String topicFor(String constant) {
            return topicForConstant.getOrDefault(constant, "General");
        }

        void keywordMatches(String searchTerm, boolean exact, IndexSearcher searcher) {
            Set<String> consts = constantsForKeyword.get(searchTerm);
            if (consts != null && !consts.isEmpty()) {
                for (String s : consts) {
                    searcher.match(s, topicFor(s), 0F);
                }
            }
            if (!exact) {
                float targetScore = consts != null && !consts.isEmpty()
                        ? THRESHOLD_SCORE_WITH_EXACT_MATCHES
                        : THRESHOLD_SCORE;
                Set<String> usedIds = consts == null ? new HashSet<>() : new HashSet<>(consts);
                constantsForKeyword.forEach((keyword, helpIds) -> {
                    if (consts != null && consts.contains(keyword)) {
                        return;
                    }
                    float score = LevenshteinDistance.score(searchTerm, keyword, false);
                    if (score < targetScore) {
                        for (String id : helpIds) {
                            if (!usedIds.contains(id)) {
                                searcher.match(id, topicFor(id), score);
                            }
                        }
                    }
                });
            }
        }

        void add(String topic, String constantName, String[] keywords) {
            topicForConstant.put(constantName, topic);
            Set<String> consts = constantsForTopic.get(topic);
            if (consts == null) {
                consts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                constantsForTopic.put(topic, consts);
            }
            consts.add(constantName);
            for (String kwd : keywords) {
                Set<String> forKeyword = constantsForKeyword.get(kwd);
                if (forKeyword == null) {
                    forKeyword = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    constantsForKeyword.put(kwd, forKeyword);
                }
                forKeyword.add(constantName);
            }
        }

        void coalesce(InternalIndex languageMatch) {
            for (Map.Entry<String, Set<String>> e : languageMatch.constantsForKeyword.entrySet()) {
                Set<String> current = constantsForKeyword.get(e.getKey());
                if (current == null) {
                    current = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                }
                current.addAll(e.getValue());
            }
            for (Map.Entry<String, Set<String>> e : languageMatch.constantsForTopic.entrySet()) {
                Set<String> current = constantsForTopic.get(e.getKey());
                if (current == null) {
                    current = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                }
                current.addAll(e.getValue());
            }
        }
    }
}
