package org.imagine.helpimpl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.imagine.geometry.Circle;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.search.HelpIndex;
import org.imagine.help.api.search.HelpSearchCallback;
import org.imagine.help.api.search.HelpSearchConstraint;
import org.imagine.help.api.search.SearchControl;
import org.imagine.markdown.uiapi.Markdown;
import org.imagine.markdown.uiapi.MarkdownComponent;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "searchTab=Search Help",
    "search=&Search",
    "searchFor=S&earch For",
    "keywords=&Keywords",
    "go=&Go",
    "exact=E&xact Match",
    "noResults=No Results",
    "searching=Searching...",
    "index=Help Index",
    "topics=&Topic",
    "selectSomething=Select an item",
    "fullText=&Full Text",
    "cancel=&Cancel"
})
public class HelpWindowComponent extends JPanel {

    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel searchPanel = new JPanel(new GridBagLayout());
    private final JPanel searchOuterPanel = new JPanel(new BorderLayout());
    private final JTextField searchTextField = new JTextField();
    private final JRadioButton keywords = new JRadioButton();
    private final JRadioButton topics = new JRadioButton();
    private final JRadioButton fullText = new JRadioButton();
    private final ButtonGroup grp = new ButtonGroup();
    private final JCheckBox exactMatch = new JCheckBox();
    private final JLabel searchLabel = new JLabel();
    private final JButton goButton = new JButton();
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final HelpTopicsPanel searchResultsPanel = new HelpTopicsPanel();
    private final HelpTopicsPanel indexContentsPanel = new HelpTopicsPanel();
    private final CancelAction cancelAction = new CancelAction();
    private final GoAction goAction = new GoAction();
    private int searches = 0;
    private SearchControl currentSearch;
    private static final Border EMPTY = BorderFactory.createEmptyBorder();

    HelpWindowComponent() {
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        tabs.add(Bundle.index(), indexContentsPanel);
        tabs.add(Bundle.searchTab(), searchOuterPanel);
        tabs.setSelectedIndex(0);
        Mnemonics.setLocalizedText(searchLabel, Bundle.searchFor());
        Mnemonics.setLocalizedText(keywords, Bundle.keywords());
        Mnemonics.setLocalizedText(exactMatch, Bundle.exact());
        Mnemonics.setLocalizedText(topics, Bundle.topics());
        Mnemonics.setLocalizedText(fullText, Bundle.fullText());
        searchLabel.setLabelFor(searchTextField);
        searchOuterPanel.add(searchPanel, BorderLayout.NORTH);
        searchOuterPanel.add(searchResultsPanel, BorderLayout.CENTER);

        keywords.setSelected(true);
        grp.add(keywords);
        grp.add(topics);
        grp.add(fullText);

        FontMetrics fm = keywords.getFontMetrics(keywords.getFont());

        int cw = fm.charWidth('O');
        JLabel fill = new JLabel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridx = gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(cw, cw, cw, cw);
        searchPanel.add(searchLabel, gbc);
        gbc.insets = new Insets(cw, 0, cw, cw);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth = 4;
        searchPanel.add(searchTextField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx += 4;
        gbc.fill = GridBagConstraints.NONE;
        searchPanel.add(goButton, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, cw, cw, cw);
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        searchPanel.add(fill, gbc);
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc.insets = new Insets(0, 0, cw, cw);
        gbc.gridx++;
        searchPanel.add(keywords, gbc);
        gbc.gridx++;
        searchPanel.add(topics, gbc);
        gbc.gridx++;
        searchPanel.add(fullText, gbc);
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        gbc.fill = GridBagConstraints.VERTICAL;
        exactMatch.setHorizontalAlignment(SwingConstants.TRAILING);
        searchPanel.add(exactMatch, gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx++;
        searchPanel.add(progress, gbc);
        progress.setVisible(false);
        progress.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (1 == e.getClickCount() && !e.isPopupTrigger()) {
                    if (currentSearch != null) {
                        currentSearch.cancel();
                        e.consume();
                    }
                }
            }

        });
        goAction.setEnabled(false);

//        indexContentsPanel.model.setContents(HelpIndex.allItemsByTopic());
        searchTextField.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (goAction.isEnabled()) {
                    goButton.doClick();
                }
            }
        });
        searchTextField.setColumns(64);
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                goAction.setEnabled(!searchTextField.getText().trim().isEmpty());
            }
        });
        searchTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                searchTextField.selectAll();
            }
        });
        setGoButtonAction(goAction);
        InputMap in = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap act = getActionMap();
        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelSearch");
        act.put("cancelSearch", cancelAction);
    }

    @Override
    public void addNotify() {
        super.addNotify();
//                indexContentsPanel.model.setContents(HelpIndex.allItemsByTopic());
        if (indexContentsPanel.model.getSize() == 0) {
            ForkJoinPool.commonPool().submit(() -> {
                long then = System.currentTimeMillis();
//                        indexContentsPanel.model.setContents(HelpIndex.allItemsByTopic());
                Map<String, List<HelpItem>> allItems = HelpIndex.allItemsByTopic();
                System.out.println("time to load all help items: " + (System.currentTimeMillis() - then + "ms"));
                EventQueue.invokeLater(() -> {
                    indexContentsPanel.model.setContents(allItems);
                });
            });
        }
    }

    public boolean isSearchSelected() {
        return tabs.getSelectedIndex() == 1;
    }

    public void setSearchSelected(boolean val) {
        if (val) {
            tabs.setSelectedIndex(1);
        } else {
            tabs.setSelectedIndex(0);
        }
    }

    private void setGoButtonAction(Action action) {
        String name = (String) action.getValue(Action.NAME);
        goButton.setAction(action);
        Mnemonics.setLocalizedText(goButton, name);
    }

    public void open(HelpItem item) {
        ItemHelpItem ihi = new ItemHelpItem(item, 0);
        int ix = indexContentsPanel.model.indexOf(ihi);
        if (ix >= 0) {
            indexContentsPanel.items.setSelectedIndex(ix);
            indexContentsPanel.items.scrollRectToVisible(indexContentsPanel.items.getCellBounds(ix, ix));
        } else {
            Markdown md = item.getContent(Markdown.class);
            indexContentsPanel.detail.setMarkdown(md);
        }
        tabs.setSelectedIndex(0);
    }

    class GoAction extends AbstractAction {

        GoAction() {
            super(Bundle.search());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean exact = exactMatch.isSelected();
            boolean kwds = keywords.isSelected();
            EnumSet<HelpSearchConstraint> constraints = EnumSet.noneOf(HelpSearchConstraint.class);
            if (kwds) {
                constraints.add(HelpSearchConstraint.KEYWORD);
            } else if (topics.isSelected()) {
                constraints.add(HelpSearchConstraint.TOPIC);
            }
            if (exact) {
                constraints.add(HelpSearchConstraint.EXACT);
            }
            performSearch(searchTextField.getText().trim(), constraints);
        }
    }

    class CancelAction extends AbstractAction {

        CancelAction() {
            super(Bundle.cancel());
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentSearch != null) {
                currentSearch.cancel();
            }
        }

    }

    class HSC implements HelpSearchCallback {

        private final int searchId;

        HSC() {
            searchId = ++searches;
        }

        @Override
        public boolean onMatch(String of, HelpItem item, String heading, String topic, float score, boolean isLast) {
            searchResultsPanel.incrementalAdd(topic, item, score);
            return true;
        }

        @Override
        public void onFinish() {
            if (searches == searchId) {
                cancelAction.setEnabled(false);
                setGoButtonAction(goAction);
                progress.setVisible(false);
                progress.setIndeterminate(false);
                currentSearch = null;
            }
        }

        @Override
        public void onStart() {
            if (searches == searchId) {
                cancelAction.setEnabled(true);
                setGoButtonAction(cancelAction);
                progress.setVisible(false);
                progress.setIndeterminate(false);
            }
        }
    }

    private void performSearch(String searchText, Set<HelpSearchConstraint> constraints) {
        if (currentSearch != null) {
            currentSearch.cancel();
        }
        searchResultsPanel.model.clear();
        currentSearch = HelpIndex.search(searchText, 50, new HSC(), constraints.toArray(new HelpSearchConstraint[0]));
    }

    private static JScrollPane cleanupScrollPane(JScrollPane pn) {
        pn.setBorder(EMPTY);
        pn.setViewportBorder(EMPTY);
        return pn;
    }

    static final class HelpTopicsPanel extends JPanel implements ListSelectionListener {

        private final HelpItemsModel model = new HelpItemsModel();
        private final FastList<HelpModelItem> items = new FastList<HelpModelItem>(model);
        private final MarkdownComponent detail = new MarkdownComponent(new Markdown(Bundle.noResults()));
        private final JScrollPane itemsScroll = cleanupScrollPane(new JScrollPane(items));
        private final JScrollPane detailScroll = cleanupScrollPane(new JScrollPane(detail));
        private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, itemsScroll, detailScroll);
        private HelpItem selectedHelpItem;

        HelpTopicsPanel() {
            super(new BorderLayout());
            add(split, BorderLayout.CENTER);
            items.setCellRenderer(new Ren());
            items.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            items.setSelectionModel(new HelpItemsSelectionModel(model));
            items.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        }

        @Override
        public void addNotify() {
            items.addListSelectionListener(this);
            super.addNotify();
            FontMetrics fm = getFontMetrics(items.getFont());
            detail.setMargin(fm.charWidth('O'));
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            items.removeListSelectionListener(this);
        }

        public void clear() {
            model.clear();
        }

        public void incrementalAdd(String topic, HelpItem item, float score) {
            preservingSelection(() -> {
                model.incrementalAdd(topic, item, score);
            });
        }

        public void setContents(Map<String, List<HelpItem>> newContents) {
            preservingSelection(() -> {
                model.setContents(newContents);
            });
        }

        private void preservingSelection(Runnable r) {
            int ix = items.getSelectedIndex();
            if (ix < 0 || model.getSize() == 0) {
                r.run();
                return;
            }
            if (ix >= model.getSize()) {
                ix = model.getSize() - 1;
            }
            HelpModelItem oldSelection = model.getElementAt(ix);
            r.run();
            int newIx = model.indexOf(oldSelection);
            if (newIx >= 0) {
                if (ix != newIx) {
                    items.setSelectedIndex(newIx);
                }
            } else {
                if (model.getSize() > 0) {
                    newIx = Math.min(model.getSize() - 1, ix);
                    HelpModelItem item = model.getElementAt(newIx);
                    selectedHelpItem = item.item();
                    items.setSelectedIndex(newIx);
                } else {
                    selectedHelpItem = null;
                }
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int ix = e.getFirstIndex();
            if (ix < 0) {
                return;
            }
            HelpModelItem item = model.getElementAt(ix);
            HelpItem itemItem = item.item();
            if (item != selectedHelpItem) {
                selectedHelpItem = itemItem;
                Markdown md = itemItem.getContent(Markdown.class);
                detail.setMarkdown(md);
            }
        }
    }

    static final class Ren implements ListCellRenderer<HelpModelItem> {

        private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();
        private Border indentBorder;
        private Border nonIndentBorder;
        private Font boldFont;
        private Font plainFont;

        private Font plainFont() {
            if (plainFont == null) {
                plainFont = delegate.getFont().deriveFont(Font.PLAIN);
            }
            return plainFont;
        }

        private Font boldFont() {
            if (boldFont == null) {
                boldFont = delegate.getFont().deriveFont(Font.BOLD);
            }
            return boldFont;
        }

        private void computeBorders(JList<?> list) {
            if (indentBorder == null) {
                FontMetrics fm = list.getFontMetrics(list.getFont());
                int width = fm.charWidth('O');
                indentBorder = BorderFactory.createEmptyBorder(0, width * 3, 0, 0);
                nonIndentBorder = BorderFactory.createEmptyBorder(0, width, 0, 0);
            }
        }

        private Border indentBorder(JList<?> list) {
            computeBorders(list);
            return indentBorder;
        }

        private Border emptyBorder(JList<?> list) {
            computeBorders(list);
            return nonIndentBorder;
        }
        private int itemSize = -1;

        @Override
        public Component getListCellRendererComponent(JList<? extends HelpModelItem> list, HelpModelItem value, int index, boolean isSelected, boolean cellHasFocus) {
            String val = value.title();
            Component result = delegate.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
            if (itemSize == -1) {
                itemSize = result.getPreferredSize().height - 1;
            }
            if (value.isTopLevelItem()) {
                delegate.setBorder(emptyBorder(list));
            } else {
                delegate.setBorder(indentBorder(list));
            }
            if (value instanceof SearchHelpItem) {
                float score = ((SearchHelpItem) value).score();
                delegate.setIcon(new ScoreIcon(itemSize, score));
            } else {
                delegate.setIcon(null);
            }
            if (value instanceof TopicHelpItem) {
                result.setFont(boldFont());
            } else {
                result.setFont(plainFont());
            }
            Rectangle r = ((FastList<?>) list).visRect;
            if (r == null) {
                r = list.getVisibleRect();
            }
            Dimension d = delegate.getPreferredSize();
            if (d.width > list.getVisibleRect().width) {
                FontMetrics fm = result.getFontMetrics(result.getFont());
                int w = fm.charWidth('W');
                int inChars = r.width / w;
                val = val.substring(0, inChars) + "\u2026";
                delegate.setText(val);
            }
            return result;
        }
    }

    static final class FastList<T> extends JList<T> {

        private boolean firstPaint = true;
        Rectangle visRect;

        public FastList(ListModel<T> dataModel) {
            super(dataModel);
            this.visRect = visRect;
        }

        @Override
        public void setFont(Font font) {
            if (!Objects.equals(getFont(), font)) {
                firstPaint = true;
                super.setFont(font);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            visRect = getVisibleRect();
            if (firstPaint) {
                FontMetrics fm = g.getFontMetrics(getFont());
                int h = fm.getMaxAscent() + fm.getMaxDescent();
                setFixedCellHeight(h);
            }
            super.paintComponent(g);
        }
    }

    static final class HelpItemsSelectionModel implements SingleSelectionModel, ListSelectionModel {

        private final HelpItemsModel contents;
        private int selected = -1;
        private final List<ChangeListener> listeners = new ArrayList<>();
        private final ChangeEvent evt = new ChangeEvent(this);

        public HelpItemsSelectionModel(HelpItemsModel contents) {
            this.contents = contents;
        }

        @Override
        public int getSelectedIndex() {
            return selected;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (index != selected) {
                int old = selected;
                if (index >= 0 && index < contents.getSize()) {
                    HelpModelItem item = contents.getElementAt(index);
                    if (item instanceof TopicHelpItem) {
                        index++;
                    }
                }
                if (index != selected) {
                    selected = index;
                    fire(old, index);
                }
            }
        }

        @Override
        public void clearSelection() {
            if (selected != -1) {
                int old = selected;
                selected = -1;
                fire(old, -1);
            }
        }

        private void fire(int old, int nue) {
            old = Math.min(old, contents.getSize() - 1);
            nue = Math.min(nue, contents.getSize() - 1);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
            if (!listListeners.isEmpty()) {
                for (ListSelectionListener lis : listListeners) {
                    ListSelectionEvent lse = new ListSelectionEvent(this,
                            Math.min(old, nue), Math.max(old, nue), adjusting);
                    lis.valueChanged(lse);
                }
            }
        }

        @Override
        public boolean isSelected() {
            return selected >= 0 && selected < contents.getSize();
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            setSelectedIndex(index1);
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            setSelectedIndex(index1);
        }

        @Override
        public void removeSelectionInterval(int index0, int index1) {
            if (selected >= Math.min(index0, index1)
                    && selected <= Math.max(index0, index1)) {
                clearSelection();
            }
        }

        @Override
        public int getMinSelectionIndex() {
            return selected;
        }

        @Override
        public int getMaxSelectionIndex() {
            return selected;
        }

        @Override
        public boolean isSelectedIndex(int index) {
            return selected == index;
        }

        @Override
        public int getAnchorSelectionIndex() {
            return selected;
        }

        @Override
        public void setAnchorSelectionIndex(int index) {
            setSelectedIndex(index);
        }

        @Override
        public int getLeadSelectionIndex() {
            return selected;
        }

        @Override
        public void setLeadSelectionIndex(int index) {
            setSelectedIndex(index);
        }

        @Override
        public boolean isSelectionEmpty() {
            return selected < 0;
        }

        @Override
        public void insertIndexInterval(int index, int length, boolean before) {
            setSelectedIndex(index + length);
        }

        @Override
        public void removeIndexInterval(int index0, int index1) {
            if (selected >= Math.min(index0, index1) && selected <= Math.max(index0, index1)) {
                clearSelection();
            }
        }

        private boolean adjusting;

        @Override
        public void setValueIsAdjusting(boolean valueIsAdjusting) {
            adjusting = valueIsAdjusting;
        }

        @Override
        public boolean getValueIsAdjusting() {
            return adjusting;
        }

        @Override
        public void setSelectionMode(int selectionMode) {
            if (selectionMode != ListSelectionModel.SINGLE_SELECTION) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int getSelectionMode() {
            return ListSelectionModel.SINGLE_SELECTION;
        }

        private final List<ListSelectionListener> listListeners = new ArrayList<>();

        @Override
        public void addListSelectionListener(ListSelectionListener x) {
            listListeners.add(x);
        }

        @Override
        public void removeListSelectionListener(ListSelectionListener x) {
            listListeners.remove(x);
        }

    }

    static final class HelpItemsModel implements ListModel<HelpModelItem> {

        private List<HelpModelItem> items = new ArrayList<>();
        private final List<ListDataListener> listeners = new ArrayList<>(3);
        private int ords = 0;

        public int indexOf(HelpModelItem item) {
            return items.indexOf(item);
        }

        public void clear() {
            int oldSize = items.size();
            if (oldSize > 0) {
                items.clear();
            }
            if (!listeners.isEmpty()) {
                ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED,
                        0, oldSize);
                for (ListDataListener l : listeners) {
                    l.contentsChanged(evt);
                }
            }
            ords = 0;
        }

        public void incrementalAdd(String topic, HelpItem item, float score) {
            SearchHelpItem shi = new SearchHelpItem(topic, item, score, ords++);
            items.add(shi);
            Collections.sort(items);
            int ix = items.indexOf(shi);
            if (!listeners.isEmpty()) {
                ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, ix, ix);
                for (ListDataListener l : listeners) {
                    l.intervalAdded(evt);
                }
            }
        }

        public void setContents(Map<String, List<HelpItem>> newContents) {
            List<HelpModelItem> old = new ArrayList<>(items);
            items.clear();
            int ord = 0;
            for (Map.Entry<String, List<HelpItem>> e : newContents.entrySet()) {
                Iterator<HelpItem> it = e.getValue().iterator();
                if (it.hasNext()) {
                    HelpItem first = it.next();
                    TopicHelpItem thi = new TopicHelpItem(e.getKey(), first, ord++);
                    items.add(thi);
                    ItemHelpItem firstsHeading = new ItemHelpItem(first, ord++);
                    items.add(firstsHeading);
                    while (it.hasNext()) {
                        ItemHelpItem next = new ItemHelpItem(it.next(), ord++);
                        items.add(next);
                    }
                }
            }
            if (!listeners.isEmpty()) {
                ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED,
                        0, Math.max(old.size(), items.size()));
                for (ListDataListener l : listeners) {
                    l.contentsChanged(evt);
                }
            }
            ords = 0;
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public HelpModelItem getElementAt(int index) {
            return items.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }
    }

    static interface HelpModelItem extends Comparable<HelpModelItem> {

        String title();

        HelpItem item();

        boolean isTopLevelItem();

        int ordinal();

        @Override
        public default int compareTo(HelpModelItem o) {
            return Integer.compare(ordinal(), o.ordinal());
        }

    }

    static final class SearchHelpItem implements HelpModelItem {

        private final String topic;

        private final HelpItem item;
        private final float score;
        private final int arrivalOrder;
        private String title;

        public SearchHelpItem(String topic, HelpItem item, float score, int arrivalOrder) {
            this.topic = topic;
            this.item = item;
            this.score = score;
            this.arrivalOrder = arrivalOrder;
        }

        float score() {
            return score;
        }

        @Override
        public String title() {
            return title == null ? title = item.heading() : title;
        }

        @Override
        public HelpItem item() {
            return item;
        }

        @Override
        public boolean isTopLevelItem() {
            return true;
        }

        @Override
        public int ordinal() {
            int result = (int) (100000 * score) + arrivalOrder;
            return result;
        }
    }

    private static final class TopicHelpItem implements HelpModelItem {

        private final String topic;
        private final HelpItem first;
        private final int ord;

        public TopicHelpItem(String topic, HelpItem first, int ord) {
            this.topic = topic;
            this.first = first;
            this.ord = ord;
        }

        public int ordinal() {
            return ord;
        }

        @Override
        public String title() {
            return topic;
        }

        @Override
        public HelpItem item() {
            return first;
        }

        @Override
        public boolean isTopLevelItem() {
            return true;
        }

        public String toString() {
            return title();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 19 * hash + Objects.hashCode(this.topic);
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
            final TopicHelpItem other = (TopicHelpItem) obj;
            if (!Objects.equals(this.topic, other.topic)) {
                return false;
            }
            return true;
        }
    }

    private static final class ItemHelpItem implements HelpModelItem {

        private final HelpItem item;
        private String heading;
        private final int ord;

        public ItemHelpItem(HelpItem item, int ord) {
            this.item = item;
            this.ord = ord;
        }

        @Override
        public int ordinal() {
            return ord;
        }

        @Override
        public String title() {
            return heading == null ? heading = item.heading() : heading;
        }

        @Override
        public HelpItem item() {
            return item;
        }

        @Override
        public boolean isTopLevelItem() {
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.item);
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
            final ItemHelpItem other = (ItemHelpItem) obj;
            if (!Objects.equals(this.item, other.item)) {
                return false;
            }
            return true;
        }

        public String toString() {
            return title();
        }
    }

    private static final Circle circ = new Circle();
    private static final Color ICON_COLOR = new Color(40, 220, 40);

    private static class ScoreIcon implements Icon {

        private final int size;
        private final float score;

        public ScoreIcon(int size, float score) {
            this.size = size;
            this.score = score;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            float halfSize = size / 2F;
            circ.setRadius(halfSize);
            circ.setCenter(x + halfSize, y + halfSize);
            Graphics2D gg = (Graphics2D) g;
            gg.setColor(ICON_COLOR);
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.draw(circ);
            if (score < 0.45F) {
                Rectangle2D r = circ.getBounds2D();
                float normScore = score / 0.45F;
                float h = (float) r.getHeight() * normScore;
                r.setFrame(r.getX(), r.getY() + h, r.getWidth(), r.getHeight() - h);
                Shape old = gg.getClip();
                gg.setClip(r);
                gg.fill(circ);
                gg.setClip(old);
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
