package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import com.mastfrog.geometry.Circle;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.api.visual.animator.AnimatorEvent;
import org.netbeans.api.visual.animator.AnimatorListener;
import org.netbeans.api.visual.animator.SceneAnimator;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class AnimationLayer extends LayerWidget {

    private static Color TRANSPARENT = new Color(255, 255, 255, 0);
    private static Color TARGET = Color.BLACK;

    private CircleWidget circle;

    public AnimationLayer(Scene scene) {
        super(scene);
        circle = new CircleWidget(scene);
        addChild(circle);
        circle.setVisible(false);
    }

    private static final int DIST = 100;

    public void setShape(Shape shape) {
        SceneAnimator anim = getScene().getSceneAnimator();
        circle.setVisible(true);
        Rectangle bds = shape.getBounds();
        Rectangle larger = new Rectangle(bds.x - DIST, bds.y - DIST,
                bds.width + (DIST * 2), bds.height + (DIST * 2));

        Rectangle sceneBounds = getScene().getBounds();
        if (larger.x < sceneBounds.x) {
            larger.width -= sceneBounds.x - larger.x;
            larger.x = sceneBounds.x;
        }
        if (larger.y < sceneBounds.y) {
            larger.height -= sceneBounds.y - larger.y;
            larger.y = sceneBounds.y;
        }
        if (larger.x + larger.width > sceneBounds.x + sceneBounds.width) {
            int off = (larger.x + larger.width) - (sceneBounds.x + sceneBounds.width);
            larger.width -= off;
        }
        if (larger.y + larger.height > sceneBounds.y + sceneBounds.height) {
            int off = (larger.y + larger.height) - (sceneBounds.y + sceneBounds.height);
            larger.height -= off;
        }

        circle.setPreferredBounds(larger);
//        circle.setPreferredBounds(sceneBounds);
        circle.setForeground(TRANSPARENT);
        anim.animateForegroundColor(circle, TARGET);
        anim.animatePreferredBounds(circle, bds);
        AL al = new AL();
        anim.getPreferredBoundsAnimator().addAnimatorListener(al);
        anim.getColorAnimator().addAnimatorListener(al);
        circle.setVisible(true);
        getScene().revalidate();
    }

    void abort() {
        circle.setVisible(false);
    }

    class AL implements AnimatorListener {

        private int countDown = 2;

        @Override
        public void animatorStarted(AnimatorEvent event) {
        }

        @Override
        public void animatorReset(AnimatorEvent event) {
        }

        @Override
        public void animatorFinished(AnimatorEvent event) {
            countDown--;
            event.getAnimator().removeAnimatorListener(this);
            if (countDown == 0 && circle.isVisible()) {
                circle.setVisible(false);
                getScene().revalidate(true);
            }
        }

        @Override
        public void animatorPreTick(AnimatorEvent event) {
        }

        @Override
        public void animatorPostTick(AnimatorEvent event) {
        }

    }

    static class CircleWidget extends Widget {

        private final Circle circle = new Circle(0, 0, 1);

        public CircleWidget(Scene scene) {
            super(scene);
        }

        @Override
        protected void paintWidget() {
            Graphics2D g = getGraphics();
            GraphicsUtils.setHighQualityRenderingHints(g);
            Rectangle r = getPreferredBounds();
            double rad = Math.max(r.getWidth(), r.getHeight()) / 2;
            circle.setCenterAndRadius(r.getCenterX(), r.getCenterY(), Math.max(1, rad * 0.5));
            g.setColor(getForeground());
            g.draw(circle);
            circle.setRadius(Math.max (3, circle.radius() - 2));
            g.draw(circle);
            circle.setRadius(Math.max (3, circle.radius() - 2));
            g.draw(circle);
            circle.setRadius(Math.max (3, circle.radius() - 2));
            g.draw(circle);
        }

    }
}
