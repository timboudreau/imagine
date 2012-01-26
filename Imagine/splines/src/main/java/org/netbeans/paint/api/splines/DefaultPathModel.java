package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.paint.api.splines.Entry.Kind;

public final class DefaultPathModel<T extends Entry> implements PathModel<T> {

    private final List<T> data = new ArrayList<T>();

    public DefaultPathModel() {
    }

    private DefaultPathModel(List<T> l) {
        data.addAll(l);
        check();
    }

    public void transform(AffineTransform xform) {
        DefaultPathModel nue = (DefaultPathModel) DefaultPathModel.create(xform.createTransformedShape(this));
        data.clear();
        data.addAll(nue.data);
        check();
    }
    
    public void translate(double offsetX, double offsetY) {
        for (T t : data) {
            for (ControlPoint p : t.getControlPoints()) {
                p.translate(offsetX, offsetY);
            }
        }
    }

    void check() {
        //XXX do this right
        for (T t : data) {
            if (t instanceof Close) {
                ((Close) t).model = this;
            } else if (t instanceof LocationEntry) {
                ((LocationEntry) t).model = this;
            }
        }
    }

    @Override
    public DefaultPathModel clone() {
        List<T> l = new ArrayList<T>();
        for (T t : data) {
            T nue = (T) t.clone();
            l.add(nue);
        }
        return new DefaultPathModel(l);
    }
    private transient GeneralPath path;

    public GeneralPath getPath() {
        GeneralPath path = null;
        if (path == null) {
            path = new GeneralPath();
            for (T entry : data) {
                entry.perform(path);
            }
        }
        return path;
    }

    public void draw(Graphics2D g, Set<T> selection, Paint selColor) {
        g.draw(getPath());
        Paint p = g.getPaint();
        boolean sel;
        for (T entry : data) {
            sel = selection.contains(entry);
            if (sel) {
                g.setPaint(selColor);
            }
            entry.draw(g);
            if (sel) {
                g.setPaint(p);
            }
        }
    }

    public void setPoint(ControlPoint node, Point2D point) {
        if (!node.match(point)) {
            node.setLocation(point);
            fire();
        }
    }

    public static PathModel<Entry> newInstance() {
        return new DefaultPathModel<Entry>();
    }

    public static PathModel<Entry> copy(PathModel<Entry> mdl) {
        return new DefaultPathModel<Entry>(mdl);
    }

    public static PathModel<Entry> create(Shape shape) {
        double[] d = new double[6];
        List<Entry> entries = new LinkedList<Entry>();
        PathIterator iter = shape.getPathIterator(AffineTransform.getTranslateInstance(0, 0));
        while (!iter.isDone()) {
            int op = iter.currentSegment(d);
            switch (op) {
                case PathIterator.SEG_MOVETO:
                    entries.add(new MoveTo(d[0], d[1]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    entries.add(new CurveTo(d[0], d[1], d[2], d[3], d[4], d[5]));
                    break;
                case PathIterator.SEG_LINETO:
                    entries.add(new LineTo(d[0], d[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    entries.add(new QuadTo(d[0], d[1], d[2], d[3]));
                    break;
                case PathIterator.SEG_CLOSE:
//                    if (!entries.isEmpty() && entries.get(0) instanceof MoveTo) {
//                        MoveTo mt = (MoveTo) entries.get(0);
//                        entries.add (new LineTo (mt.x, mt.y));
//                    }
                    entries.add(new Close());
                    break;
                default:
                    throw new AssertionError("Not a PathIterator segment type: " + op);
            }
            iter.next();
        }
        return new DefaultPathModel<Entry>(entries);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return data.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> wrap = data.iterator();
        if (wrap == null) {
            throw new NullPointerException();
        }
        return new Iter<T>(wrap);
    }

    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return data.toArray(a);
    }

    @Override
    public boolean add(T e) {
        boolean result;
        if (result = data.add(e)) {
            fire();
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result;
        if (result = data.remove(o)) {
            fire();
        }
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return data.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean result;
        if (result = data.addAll(c)) {
            fire();
        }
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean result;
        if (result = data.addAll(index, c)) {
            fire();
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result;
        if (result = data.removeAll(c)) {
            fire();
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result;
        if (result = data.retainAll(c)) {
            fire();
        }
        return result;
    }

    @Override
    public void clear() {
        boolean fire = isEmpty();
        data.clear();
        if (fire) {
            fire();
        }
    }

    @Override
    public T get(int index) {
        return data.get(index);
    }

    @Override
    public T set(int index, T element) {
        T result = data.set(index, element);
        if (result != element) {
            fire();
        }
        return result;
    }

    @Override
    public void add(int index, T element) {
        data.add(index, element);
        fire();
    }

    @Override
    public T remove(int index) {
        T result = data.remove(index);
        fire();
        return result;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            if (get(i) == o) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = size() - 1; i >= 0; i--) {
            if (get(i) == o) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return new LI<T>(data.listIterator());
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new LI<T>(data.listIterator(index));
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new DefaultPathModel<T>(data.subList(fromIndex, toIndex));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("    GeneralPath gp = new GeneralPath();\n");
        for (T elem : data) {
            sb.append("    ");
            sb.append(elem);
        }
        return sb.toString();
    }
    private transient ArrayList<ChangeListener> changeListenerList;

    @Override
    public synchronized void addChangeListener(ChangeListener listener) {
        if (changeListenerList == null) {
            changeListenerList = new java.util.ArrayList<ChangeListener>();
        }
        changeListenerList.add(listener);
    }

    @Override
    public synchronized void removeChangeListener(ChangeListener listener) {
        if (changeListenerList != null) {
            changeListenerList.remove(listener);
        }
    }

    private void fire() {
        path = null;
        check();
        java.util.ArrayList list;
        javax.swing.event.ChangeEvent e = new ChangeEvent(this);
        synchronized (this) {
            if (changeListenerList == null) {
                return;
            }
            list = (ArrayList) changeListenerList.clone();
        }
        for (int i = 0; i < list.size(); i++) {
            ((javax.swing.event.ChangeListener) list.get(i)).stateChanged(e);
        }
    }

    //Impl of java.awt.Shape
    @Override
    public Rectangle getBounds() {
        return getPath().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return getPath().getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return getPath().contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return getPath().contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return getPath().intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return getPath().intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return getPath().contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return getPath().contains(r);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return getPath().getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPath().getPathIterator(at, flatness);
    }

    @Override
    public T add(Kind kind, double x, double y) {
        T result;
        switch (kind) {
            case Close:
                this.add(result = (T) new Close());
                break;
            case CurveTo:
                CurveTo c = new CurveTo(x - 12, y - 12, x + 12, y + 12, x, y);
                add(result = (T) c);
                break;
            case LineTo:
                add(result = (T) new LineTo(x, y));
                break;
            case MoveTo:
                add(result = (T) new MoveTo(x, y));
                break;
            case QuadTo:
                add(result = (T) new QuadTo(x + 12, y + 12, x, y));
                break;
            default:
                throw new AssertionError(kind);
        }
        return result;
    }

    @Override
    public T add(int index, Kind kind, double x, double y) {
        T result;
        switch (kind) {
            case Close:
                this.add(index, result = (T) new Close());
                break;
            case CurveTo:
                CurveTo c = new CurveTo(x - 12, y - 12, x + 12, y + 12, x, y);
                add(index, result = (T) c);
                break;
            case LineTo:
                add(index, result = (T) new LineTo(x, y));
                break;
            case MoveTo:
                add(index, result = (T) new MoveTo(x, y));
                break;
            case QuadTo:
                add(index, result = (T) new QuadTo(x + 12, y + 12, x, y));
                break;
            default:
                throw new AssertionError(kind);

        }
        return result;
    }

    @Override
    public Edge[] getPathEdges() {
        List<Edge> result = new ArrayList<Edge>();
        final int sz = size();
        ControlPoint prev = null;
        for (int i = 0; i < sz; i++) {
            Entry e = get(i);
            ControlPoint[] nn = e.getControlPoints();
            if (nn.length > 0) {
                ControlPoint curr = nn[0];
                if (prev != null) {
                    result.add(new EdgeImpl(prev, curr, true));
                }
                prev = curr;
            } else {
                prev = null;
            }
        }
        return result.toArray(new Edge[result.size()]);
    }

    @Override
    public Set<ControlPoint> allNodes() {
        Set<ControlPoint> s = new HashSet<ControlPoint>();
        for (Entry e : this) {
            s.addAll(Arrays.asList(e.getControlPoints()));
        }
        return s;
    }

    @Override
    public Set<Edge> allEdges() {
        Set<Edge> result = new HashSet<Edge>(Arrays.asList(getPathEdges()));
        for (Entry e : this) {
            ControlPoint[] nn = e.getControlPoints();
            for (ControlPoint n : nn) {
                result.addAll(Arrays.asList(n.getEdges()));
            }
        }
        return result;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Shape) {
            return ShapeUtils.shapesEqual(this, (Shape) o, false);
        }
        return false;
    }

    private class Iter<T> implements Iterator<T> {

        private Iterator<T> it;

        private Iter(Iterator<T> other) {
            this.it = other;
            if (it == null) {
                throw new NullPointerException();
            }
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public void remove() {
            it.remove();
            fire();
        }
    }

    private class LI<T> implements ListIterator<T> {

        private final ListIterator<T> it;

        public LI(ListIterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public boolean hasPrevious() {
            return it.hasPrevious();
        }

        @Override
        public T previous() {
            return it.previous();
        }

        @Override
        public int nextIndex() {
            return it.nextIndex();
        }

        @Override
        public int previousIndex() {
            return it.previousIndex();
        }

        @Override
        public void remove() {
            it.remove();
            fire();
        }

        @Override
        public void set(T e) {
            it.set(e);
            fire();
        }

        @Override
        public void add(T e) {
            it.add(e);
            fire();
        }
    }
}
