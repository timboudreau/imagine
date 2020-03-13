/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import org.imagine.editor.api.snap.SnapPoint;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author eppleton
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 20)
public class SnapStatusLineElementProvider implements StatusLineElementProvider {

    private JLabel statusLineLabel = new MinSizeLabel();

    private static SnapStatusLineElementProvider INSTANCE;

    public SnapStatusLineElementProvider() {
        statusLineLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        INSTANCE = this;
    }

    static void setSnapPoints(SnapPoint<ShapeSnapPointEntry> xp, SnapPoint<ShapeSnapPointEntry> yp) {
        if (INSTANCE != null) {
            INSTANCE._setSnapPoints(xp, yp);
        }
    }

    @Messages({
        "# {0} - snapKind",
        "# {1} - axes",
        "# {2} - targetInfo",
        "SNAP=Snap {0} to {1} from {2}",
        "# {0} - width",
        "# {1} - height",
        "SIZE={0}x{1}",
        "# {0} - width",
        "# {1} - height",
        "# {2} - origin",
        "SIZE_FROM={0}x{1} from {2}",
        "# {0} - width",
        "WIDTH=w{0}",
        "# {0} - width",
        "# {1} - origin",
        "WIDTH_FROM=width {0} from {1}",
        "# {0} - width",
        "# {1} - origin",
        "HEIGHT_FROM=hight {0} from {1}",
        "# {0} - height",
        "HEIGHT=h{0}",
        "# {0} - width",
        "# {1} - height",
        "# {2} - origin",
        "WIDTH_HEIGHT_FROM={0}x{1} from {2}",
        "# {0} - angle",
        "# {1} - origin",
        "ANGLE_FROM={0}\u00B0 from {1}",
        "# {0} - coordinate",
        "# {1} - origin",
        "COORD_FROM={0} from {1}",
        "# {0} - x_coordinate",
        "# {1} - origin",
        "X_COORD_FROM=x{0} from {1}",
        "# {0} - y_coordinate",
        "# {1} - origin",
        "Y_COORD_FROM=y{0} from {1}",
        "XY=x/y",
        "# {0} - x",
        "# {1} - y",
        "POINT={0},{1}",
        "# {0} - coordinate1",
        "# {1} - origin1",
        "# {2} - coordinate2",
        "# {3} - origin2",
        "COORDS_FROM={0} from {1}, {2} from {3}",
        "# {0} - coordinate1",
        "# {1} - width",
        "# {2} - coordinate2",
        "# {3} - height",
        "SIZES_FROM=w{0} from {1}, h{2} from {3}",
        "# {0} - x",
        "# {1} - y",
        "GRID=Grid {0}, {1}"
    })
    private void _setSnapPoints(SnapPoint<ShapeSnapPointEntry> xp, SnapPoint<ShapeSnapPointEntry> yp) {
        String snapKind = "";
        String axes = "";
        String targetInfo = "";
        if (xp != null && yp != null) {
            snapKind = xp.kind().toString();
            if (xp.kind() == yp.kind()) {
                axes = Bundle.XY();
                switch (xp.kind()) {
                    case ANGLE:
                        targetInfo = Bundle.ANGLE_FROM(xp.value().sizeOrAngle, xp.value().entry.getName());
                        break;
                    case MATCH:
                        if (xp.value().entry.equals(yp.value().entry)) {
                            targetInfo = Bundle.COORD_FROM(Bundle.POINT(
                                    xp.coordinate(), yp.coordinate()), xp.value().entry.getName());
                        } else {
                            targetInfo = Bundle.COORDS_FROM(xp.coordinate(),
                                    xp.value().entry.getName(),
                                    yp.coordinate(), yp.value().entry.getName());
                        }
                        break;
                    case DISTANCE:
                        if (xp.value().entry.equals(yp.value().entry)) {
                            targetInfo = Bundle.SIZE_FROM(xp.value().sizeOrAngle,
                                    yp.value().sizeOrAngle, xp.value().entry.getName());
                        } else {
                            targetInfo = Bundle.SIZES_FROM(
                                    xp.value().sizeOrAngle,
                                    xp.value().entry.getName(),
                                    yp.value().sizeOrAngle,
                                    yp.value().entry.getName());
                        }
                        break;
                    case GRID:
                        targetInfo = Bundle.POINT(xp.coordinate(), yp.coordinate());
                        break;
                    default:
                    case NONE:
                        // should not happen
                        setStatus("");
                        return;
                }
            } else {
                StringBuilder sb = new StringBuilder();
                switch (xp.kind()) {
                    case ANGLE:
                        targetInfo = Bundle.ANGLE_FROM(xp.value().sizeOrAngle, xp.value().entry.getName());
                        break;
                    case DISTANCE:
                        targetInfo = Bundle.WIDTH_FROM(FMT.format(xp.coordinate()),
                                xp.value().entry.getName());
                        break;
                    case GRID:
                        targetInfo = FMT.format(xp.coordinate());
                        break;
                    default:
                        targetInfo = Bundle.X_COORD_FROM(xp.coordinate(), xp.value().entry.getName());
                        break;
                }
                sb.append(targetInfo);
                switch (yp.kind()) {
                    case ANGLE:
                        targetInfo = Bundle.ANGLE_FROM(yp.value().sizeOrAngle, yp.value().entry.getName());
                        break;
                    case DISTANCE:
                        targetInfo = Bundle.HEIGHT_FROM(FMT.format(yp.coordinate()),
                                yp.value().entry.getName());
                        break;
                    case GRID:
                        targetInfo = FMT.format(yp.coordinate());
                        break;
                    default:
                        targetInfo = Bundle.Y_COORD_FROM(yp.coordinate(), yp.value().entry.getName());
                        break;
                }
                sb.append(targetInfo);
                targetInfo = sb.toString();
            }
        } else if (xp != null) {
            snapKind = xp.kind().toString();
            axes = xp.axis().toString();
            switch (xp.kind()) {
                case ANGLE:
                    targetInfo = Bundle.ANGLE_FROM(xp.value().sizeOrAngle, xp.value().entry.getName());
                    break;
                case DISTANCE:
                    targetInfo = Bundle.WIDTH_FROM(FMT.format(xp.coordinate()),
                            xp.value().entry.getName());
                    break;
                case GRID:
                    targetInfo = FMT.format(xp.coordinate());
                    break;
                default:
                    targetInfo = Bundle.X_COORD_FROM(xp.coordinate(), xp.value().entry.getName());
                    break;
            }
        } else if (yp != null) {
            snapKind = yp.kind().toString();
            axes = yp.axis().toString();
            switch (yp.kind()) {
                case ANGLE:
                    targetInfo = Bundle.ANGLE_FROM(yp.value().sizeOrAngle, yp.value().entry.getName());
                    break;
                case DISTANCE:
                    targetInfo = Bundle.HEIGHT_FROM(FMT.format(yp.coordinate()),
                            yp.value().entry.getName());
                    break;
                case GRID:
                    targetInfo = FMT.format(yp.coordinate());
                    break;
                default:
                    targetInfo = Bundle.Y_COORD_FROM(yp.coordinate(), yp.value().entry.getName());
                    break;
            }
        } else {
            setStatus("");
            return;
        }
        setStatus(Bundle.SNAP(snapKind, axes, targetInfo));
    }

    public Component getStatusLineElement() {
        return statusLineLabel;
    }

    public void setStatus(String statusMessage) {
        statusLineLabel.setText(statusMessage);
    }

    private static final DecimalFormat FMT = new DecimalFormat("###0.00#");

    static class MinSizeLabel extends JLabel {

        private boolean firstPaint = true;
        private int charWidth = -1;

        MinSizeLabel() {
            setFont(getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9)));
        }

        @Override
        public void paint(Graphics g) {
            if (firstPaint) {
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics(getFont());
                charWidth = fm.stringWidth("0");
                firstPaint = false;
            }
            super.paint(g);
        }

        @Override
        public void setFont(Font font) {
            firstPaint = true;
            charWidth = -1;
            super.setFont(font);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            Dimension result = super.getPreferredSize();
            int w = charWidth;
            if (w == -1) {
                w = 12;
            }
            result.width = Math.max((ins.left + ins.right) + (w * 10), result.width);
            result.height = Math.max(10, result.height);
            return result;
        }
    }
}
