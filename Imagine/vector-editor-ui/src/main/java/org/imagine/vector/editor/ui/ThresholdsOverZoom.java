package org.imagine.vector.editor.ui;

import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.grid.Grid;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.Thresholds;
import static org.imagine.geometry.CornerAngle.ENCODING_MULTIPLIER;

/**
 *
 * @author Tim Boudreau
 */
public class ThresholdsOverZoom implements Thresholds {

    private final Zoom zoom;

    public ThresholdsOverZoom(Zoom zoom) {
        this.zoom = zoom;
    }

    @Override
    public double threshold(SnapKind kind) {
        switch (kind) {
            case CORNER:
                // CornerAngles encoded as doubles are multiplied by
                // 1 million, so we need a ten degree range here
                // (make this smaller once it's working)
                return 10 * ENCODING_MULTIPLIER;
            case ANGLE:
                // How close an angle should we snap to?
                return 5;
            case DISTANCE:
                return 5;
            case EXTENT:
                return 10;
            case LENGTH :
                return 10;
            case GRID:
                return zoom.inverseScale(Grid.getInstance().size() / 4);
            default:
                return pointThreshold();
        }
    }

    @Override
    public double pointThreshold() {
        return zoom.inverseScale(10);
    }

}
