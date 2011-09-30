package net.java.dev.imagine.layers.text.widget.api;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.netbeans.paint.api.components.Fonts;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 * A text item visible in a text layer
 *
 * @author Tim Boudreau
 */
public interface Text {

    public static final String PROP_FONT = "font";
    public static final String PROP_TEXT = "text";
    public static final String PROP_LEADING = "leading";
    public static final String PROP_LOCATION = "location";
    public static final String PROP_TRANSFORM = "transform";
    public static final String PROP_BACKGROUND = "background";

    void setText(String text);

    void setLocation(Point location);

    void setFont(Font font);

    void setLeading(double leading);

    void setTransform(AffineTransform xform);

    String getText();

    Point getLocation();

    Font getFont();

    double getLeading();

    Paint getPaint();

    void setPaint(Paint paint);

    AffineTransform getTransform();

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    public final class TextImpl implements Text {

        private static int ct = 0;
        private AffineTransform transform = AffineTransform.getTranslateInstance(0, 0);
        private String text = NbBundle.getMessage(TextImpl.class, "TEXT");
        private double leading = 1.0D;
        private final PropertyChangeSupport supp = new PropertyChangeSupport(this);
        private Font font = Fonts.getDefault().get();
        private final Point location = new Point();
        private Paint paint = Color.BLACK;

        public TextImpl() {
            this(NbBundle.getMessage(TextImpl.class, "TEXT") + ' ' + ++ct);
        }

        public TextImpl(String text) {
            Parameters.notNull("text", text);
            this.text = text;
        }

        public TextImpl(String text, Point point) {
            Parameters.notNull("text", text);
            Parameters.notNull("point", point);
            this.text = text;
            this.location.setLocation(point);
        }

        public TextImpl(Text text) {
            setText(text.getText());
            setLeading(text.getLeading());
            setFont(text.getFont());
            setLocation(text.getLocation());
            setTransform(text.getTransform());
        }

        public final Font getFont() {
            return font;
        }

        public final void setFont(Font font) {
            Parameters.notNull("font", font);
            if (!font.equals(this.font)) {
                Font old = this.font;
                this.font = font;
                fire(PROP_FONT, old, font);
            }
        }

        public final double getLeading() {
            return leading;
        }

        public final void setLeading(double leading) {
            if (leading != this.leading) {
                double old = this.leading;
                this.leading = leading;
                fire(PROP_LEADING, old, leading);
            }
        }

        public final Point getLocation() {
            return new Point(location);
        }

        public final void setLocation(Point location) {
            Parameters.notNull("location", location);
            if (!location.equals(this.location)) {
                Point old = new Point(this.location);
                this.location.setLocation(location);
                fire(PROP_LOCATION, old, location);
            }
        }

        public final String getText() {
            return text;
        }

        public final void setText(String text) {
            Parameters.notNull("text", text);
            if (!text.equals(this.text)) {
                String old = this.text;
                this.text = text;
                fire(PROP_TEXT, old, text);
            }
        }

        public final AffineTransform getTransform() {
            return transform;
        }

        public final void setTransform(AffineTransform transform) {
            Parameters.notNull("transform", transform);
            if (!this.transform.equals(transform)) {
                AffineTransform old = this.transform;
                this.transform = transform;
                fire(PROP_TRANSFORM, old, transform);
            }
        }

        public final synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
            supp.removePropertyChangeListener(listener);
        }

        protected final void fire(String propertyName, Object oldValue, Object newValue) {
            supp.firePropertyChange(propertyName, oldValue, newValue);
        }

        public final synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
            supp.addPropertyChangeListener(listener);
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public final boolean equals(Object o) {
            //should always be an identity check
            return super.equals(o);
        }

        public final int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return "TextImpl{" + "text=" + text + ", font=" + font + ", location=" + location + '}';
        }

        @Override
        public Paint getPaint() {
            return paint;
        }

        @Override
        public void setPaint(Paint paint) {
            Parameters.notNull("paint", paint);
            this.paint = paint;
        }
    }
}
