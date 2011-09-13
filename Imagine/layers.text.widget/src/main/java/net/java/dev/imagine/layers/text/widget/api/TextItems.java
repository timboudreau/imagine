package net.java.dev.imagine.layers.text.widget.api;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Parameters;

/**
 * 
 * Object in the lookup of a text layer which can have text items added/removed.
 *
 * @author Tim Boudreau
 */
public interface TextItems extends Iterable<Text> {

    public void addChangeListener(ChangeListener cl);

    public void removeChangeListener(ChangeListener cl);

    public void add(Text text);

    public void remove(Text text);
    
    public boolean contains(Text text);
    
    public void edit(Text text);
    
    public Text hit (Point p);

    public final class TextItemsSupport implements TextItems {
        private final ChangeSupport supp = new ChangeSupport(this);
        private final List<Text> items = new ArrayList<Text>();
        private final Editor editor;

        public TextItemsSupport(Editor editor) {
            Parameters.notNull("editor", editor);
            this.editor = editor;
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }

        @Override
        public void add(Text text) {
            items.add(text);
            supp.fireChange();
        }

        @Override
        public void remove(Text text) {
            if (items.remove(text)) {
                supp.fireChange();
            }
        }

        @Override
        public Iterator<Text> iterator() {
            return Collections.unmodifiableList(items).iterator();
        }

        @Override
        public boolean contains(Text text) {
            return items.contains(text);
        }

        @Override
        public void edit(Text text) {
            editor.edit(text);
        }

        @Override
        public Text hit(Point p) {
            return editor.hit(p);
        }
        
        public interface Editor {
            public Text hit(Point p);
            public void edit(Text text);
        }
    }
}
