package org.imagine.editor.api.snap;

/**
 *
 * @author Tim Boudreau
 */
public interface Thresholds {

    public double threshold(SnapKind kind);

    public double pointThreshold();
}
