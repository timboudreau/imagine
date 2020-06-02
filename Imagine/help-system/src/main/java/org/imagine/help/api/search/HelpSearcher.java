/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.help.api.search;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.imagine.help.api.HelpItem;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
class HelpSearcher implements SearchControl {

    private static final int CONCURRENT_SEARCH_THREADS;

    static {
        String prop = System.getProperty("help-search-threads", "5");
        int value = 5;
        try {
            value = Integer.parseInt(prop);
            if (value <= 0) {
                throw new IllegalArgumentException("Invalid help search threads: " + value);
            }
        } catch (Exception nfe) {
            Logger.getLogger(HelpSearcher.class.getName()).log(Level.INFO, "Bad value for system property "
                    + "help-search-threads: '" + prop + "'", nfe);
        }
        CONCURRENT_SEARCH_THREADS = value;
    }
    private static final RequestProcessor SEARCH_POOL = new RequestProcessor("help-search",
            CONCURRENT_SEARCH_THREADS, false);
    private final List<HelpIndex> indices;
    private final HelpSearchCallback callback;
    private final Set<HelpSearchConstraint> constraints;
    private final int maxItems;
    private final AtomicInteger itemCount = new AtomicInteger();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Set<String> returnedIds = new ConcurrentSkipListSet<>();
    private final Locale locale;
    private final String searchTerm;
    private final AtomicInteger runningCount = new AtomicInteger();
    private static final Logger LOG = Logger.getLogger(HelpSearcher.class.getName());

    static {
        LOG.setLevel(Level.ALL);
    }

    HelpSearcher(Locale locale, String searchTerm, Collection<? extends HelpIndex> indices, HelpSearchCallback callback, Set<HelpSearchConstraint> constraints, int maxItems) {
        this.locale = locale;
        this.searchTerm = searchTerm;
        this.indices = new CopyOnWriteArrayList<>(indices);
        this.callback = callback;
        this.constraints = constraints;
        this.maxItems = maxItems;
    }

    public String toString() {
        return "HelpSearcher('" + searchTerm + "' max " + maxItems + " over " + indices + ")";
    }

    public boolean isRunning() {
        return runningCount.get() > 0;
    }

    public boolean start() {
//        System.out.println("start " + this);
        int count = Math.min(CONCURRENT_SEARCH_THREADS, indices.size());
        LOG.log(Level.FINE, "Start {0} with up to {1} threads and {2} indices: {3}",
                new Object[]{this, count, indices.size(), indices});
        if (count > 0) {
            if (runningCount.compareAndSet(0, 1)) {
                if (indices.size() <= count) {
                    LOG.log(Level.FINEST, "Not enough indices - use one thread");
                    SEARCH_POOL.submit(new SearchRunner(0, new ArrayList<>(indices)));
                    EventQueue.invokeLater(callback::onStart);
                    return true;
                }
                int batchSize = indices.size() / count;
                List<HelpIndex> copy = new ArrayList<>(indices);
                List<SearchRunner> runners = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    List<HelpIndex> curr = new ArrayList<>(copy.size());
                    int start = i * batchSize;
                    for (int j = start; j < start + batchSize; j++) {
                        if (j >= copy.size()) {
                            break;
                        }
                        curr.add(copy.get(j));
                    }
                    if (!curr.isEmpty()) {
                        runners.add(new SearchRunner(i, curr));
                    }
                }
//                System.out.println("Start " + runners.size() + " search threads for '" + searchTerm
//                    + "' over " + indices + " with " + runners);
                LOG.log(Level.INFO, "Start {0} search threads for {1} over {2} indices: {3}", new Object[]{
                    runners.size(), searchTerm, indices, runners
                });
                for (SearchRunner r : runners) {
                    SEARCH_POOL.submit(r);
                }
                if (!runners.isEmpty()) {
                    EventQueue.invokeLater(callback::onStart);
                }
                return !runners.isEmpty();
            }
        } else {
//            System.out.println("nothing to do " + count + " / " + indices.size());
            LOG.log(Level.INFO, "Nothing to do for {0} with {1} threads for {2} indices",
                    new Object[]{this, count, indices.size()});
        }
        return false;
    }

    void replanCallback(HelpItem item, String heading, HelpIndex index, String topic, float score, boolean isLast) {
        String id = item.identifier();
        if (returnedIds.add(id)) {
            EventQueue.invokeLater(() -> {
                boolean stopped = isStop();
//                System.out.println("replan have " + item + " stopped? " + stopped + " score " + score);
                if (!stopped) {
                    boolean callbackResult = callback.onMatch(searchTerm, item, heading, topic, score, false);
                    if (!callbackResult) {
                        cancelled.set(true);
                    } else {
                        itemCount.incrementAndGet();
                        if (isLast) {
                            indices.remove(index);
                        }
                    }
                } else {
                    returnedIds.remove(id);
                }
            });
        } else {
            System.out.println("already did " + id);
        }
    }

    boolean isStop() {
        return itemCount.get() == maxItems || cancelled.get();
    }

    class SearchRunner implements Runnable {

        private final List<HelpIndex> toSearch;
        private final int index;

        public SearchRunner(int index, List<HelpIndex> indices) {
            this.index = index;
            this.toSearch = indices;
        }

        public String toString() {
            return "SearchRunner-" + index + "(" + toSearch + ")";
        }

        @Override
        public void run() {
            Thread.currentThread().setName(toString());
            runningCount.getAndIncrement();
            try {
                for (HelpIndex index : toSearch) {
                    int currentMax = maxItems - itemCount.get();
                    LOG.log(Level.FINE, "{0} begin search {1} for max {2} of {3} currently {4}", new Object[]{this, index,
                        currentMax, maxItems, itemCount});
                    if (itemCount.get() >= maxItems) {
                        System.out.println("have max items " + maxItems + " abort");
                        LOG.log(Level.FINEST, "{0} stop looking with {1} of {2} items", new Object[]{this, itemCount, maxItems});
                        break;
                    }
                    if (isStop()) {
                        LOG.log(Level.FINEST, "{0} cancelled or completed with {1} of {2} items", new Object[]{this, itemCount, maxItems});
                        break;
                    }
                    LOG.log(Level.FINER, "{0} enter {1} looking for {2} items", new Object[]{this, index, currentMax});
                    index.runSearch(locale, searchTerm, constraints, currentMax, (of, item, heading, topic, score, isLast) -> {
                        boolean stopped = isStop();
                        if (!stopped) {
                            LOG.log(Level.FINEST, "{0} forward item {1} topic ''{2}'' score {3}",
                                    new Object[]{this, item, topic, score});
                            replanCallback(item, heading, index, topic, score, isLast);
                        }
                        // May have changed, so re-check
                        return !isStop();
                    });
                }
            } finally {
                int remaining = runningCount.decrementAndGet();
                if (remaining == 1) {
                    EventQueue.invokeLater(callback::onFinish);
                    runningCount.decrementAndGet();
                }
            }
        }
    }

    @Override
    public boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    @Override
    public boolean resume() {
        if (cancelled.compareAndSet(true, false)) {
            if (!indices.isEmpty()) {
                itemCount.set(0);
                start();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return !indices.isEmpty();
    }
}
