package org.imagine.markdown.uiapi;

import java.awt.Shape;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
class LinkImpl implements RegionOfInterest {

    private final Shape bounds;
    private final String url;
    private final Kind kind;

    public LinkImpl(Shape bounds, String url, Kind kind) {
        this.bounds = bounds;
        this.url = url;
        this.kind = kind;
    }

    public String content() {
        return url;
    }

    public Shape region() {
        return bounds;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    public String toString() {
        return kind().name() + "(" + url + ":" + region() + ")";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.bounds);
        hash = 83 * hash + Objects.hashCode(this.url);
        hash = 83 * hash + Objects.hashCode(this.kind);
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
        final LinkImpl other = (LinkImpl) obj;
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        if (!Objects.equals(this.bounds, other.bounds)) {
            return false;
        }
        if (this.kind != other.kind) {
            return false;
        }
        return true;
    }
}
