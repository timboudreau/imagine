package org.imagine.vector.editor.ui.tools;

import com.mastfrog.util.collections.IntSet;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.api.visual.widget.Widget;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeActions {

    static JPopupMenu populatePopup(
            ControlPoint pt, ShapeElement entry,
            ShapesCollection shapes, Runnable refresh,
            Widget widget, Point2D sceneLocation) {
        JPopupMenu result = populatePopup(entry, shapes,
                refresh, widget, sceneLocation);

        result.add(new JSeparator(), 0);

        if (!pt.availableControlPointKinds().isEmpty()) {
            JMenu changeType = new JMenu();
            Mnemonics.setLocalizedText(changeType, Bundle.changeType());
            for (ControlPointKind k : pt.availableControlPointKinds()) {
                new ChangePointTypeAction(pt, k, entry, shapes, refresh)
                        .addToMenu(changeType);
            }
            result.add(changeType, 0);
        }

        DeleteControlPointAction deletePoint
                = new DeleteControlPointAction(pt, entry, shapes, refresh);
        deletePoint.addToMenu(result, 0);

        return result;
    }

    static JPopupMenu populatePopup(ShapeElement entry,
            ShapesCollection shapes, Runnable refresh,
            Widget widget, Point2D sceneLocation) {
        JPopupMenu menu = new JPopupMenu();
        new ToFrontAction(entry, shapes, refresh).addToMenu(menu);
        new ToBackAction(entry, shapes, refresh).addToMenu(menu);
        menu.add(new JSeparator());

        new TesselateTriangleAction(entry, shapes, refresh).addToMenu(menu);
        new ConvertToPathAction(entry, shapes, refresh).addToMenu(menu);
        new ToCubicCurvesAction(entry, shapes, refresh).addToMenu(menu);
        new ToQuadCurvesAction(entry, shapes, refresh).addToMenu(menu);
        new ResetControlPointsAction(entry, shapes, refresh).addToMenu(menu);
        menu.add(new JSeparator());
        JMenu rotation = new JMenu();
        Mnemonics.setLocalizedText(rotation, Bundle.rotateSubmenu());
        IntSet ints = IntSet.create(12);
        for (int i = 45; i < 360; i += 45) {
            ints.add(i);
        }
        for (int i = 30; i < 360; i += 30) {
            ints.add(i);
        }
        ints.forEach((IntConsumer) d -> {
            ShapeAction a = new RotateAction(d, entry, shapes, refresh);
            a.addToMenu(rotation);
        });
        menu.add(rotation);
        menu.add(new JSeparator());
        JMenu addPoints = new JMenu();
        Mnemonics.setLocalizedText(addPoints, Bundle.add());
        for (ControlPointKind k : new ControlPointKind[]{ControlPointKind.LINE_TO_DESTINATION, ControlPointKind.CUBIC_CURVE_DESTINATION, ControlPointKind.QUADRATIC_CURVE_DESTINATION}) {
            new AddPointAction(k, sceneLocation, entry, shapes, refresh).addToMenu(addPoints);
        }
        menu.add(addPoints);
        JMenu csg = new JMenu();
        Mnemonics.setLocalizedText(csg, Bundle.csg());
        for (CSGOperation op : CSGOperation.values()) {
            new CSGAction(op, sceneLocation, entry, shapes, refresh).addToMenu(csg);
        }
        menu.add(csg);

        menu.add(new JSeparator());
        new DeleteShapeAction(entry, shapes, refresh).addToMenu(menu);
        return menu;

    }

    static abstract class ShapeAction extends AbstractAction {

        protected final ShapeElement entry;
        protected final ShapesCollection coll;
        private final Runnable refreshUi;

        ShapeAction(String name, ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(name);
            this.entry = entry;
            this.coll = coll;
            this.refreshUi = refreshUi;
        }

        void addToMenu(JPopupMenu menu) {
            JMenuItem item = new JMenuItem(this);
            String name = (String) getValue(NAME);
            Mnemonics.setLocalizedText(item, name);
            menu.add(item);
        }

        void addToMenu(JPopupMenu menu, int pos) {
            JMenuItem item = new JMenuItem(this);
            String name = (String) getValue(NAME);
            Mnemonics.setLocalizedText(item, name);
            menu.add(item, pos);
        }

        void addToMenu(JMenu menu) {
            JMenuItem item = new JMenuItem(this);
            String name = (String) getValue(NAME);
            Mnemonics.setLocalizedText(item, name);
            menu.add(item);
        }

        protected abstract boolean run();

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isEnabled()) {
                inEdit(this::run);
            }
        }

        private void inEdit(BooleanSupplier r) {
            coll.edit(getValue(NAME) + " " + ShapeNames.nameOf(entry.item()), entry, () -> {
                if (r.getAsBoolean()) {
                    refreshUi.run();
                }
            });
        }
    }

    @Messages("deletePoint=Delete Point")
    static class DeleteControlPointAction extends ShapeAction {

        private final ControlPoint pt;

        public DeleteControlPointAction(ControlPoint pt, ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.deletePoint(), entry, coll, refreshUi);
            this.pt = pt;
            setEnabled(pt.canDelete());
        }

        @Override
        protected boolean run() {
            return pt.delete();
        }
    }

    @Messages({
        "changeType=&Change Type"
    })
    static final class ChangePointTypeAction extends ShapeAction {

        private final ControlPoint pt;
        private final ControlPointKind kind;

        public ChangePointTypeAction(ControlPoint pt,
                ControlPointKind kind, ShapeElement entry,
                ShapesCollection coll, Runnable refreshUi) {
            super(kind.toString(), entry, coll, refreshUi);
            this.pt = pt;
            this.kind = kind;
            setEnabled(!pt.availableControlPointKinds().isEmpty());
        }

        @Override
        protected boolean run() {
            PathIteratorWrapper path = entry.item()
                    .as(PathIteratorWrapper.class);
            boolean result = kind.changeType(path, pt.index());
            if (!result) {
                StatusDisplayer.getDefault().setStatusText("Could not change type");
                System.out.println("did not change type from " + pt.kind() + " to " + kind);
            }
            return result;
        }
    }

    @NbBundle.Messages("duplicate=&Duplicate")
    static class DuplicateShapeAction extends ShapeAction {

        public DuplicateShapeAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.duplicate(), entry, coll, refreshUi);
        }

        @Override
        protected boolean run() {
            coll.duplicate(entry);
            return true;
        }

    }

    @NbBundle.Messages("delete=&Delete")
    static class DeleteShapeAction extends ShapeAction {

        public DeleteShapeAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.delete(), entry, coll, refreshUi);
        }

        @Override
        protected boolean run() {
            coll.deleteShape(entry);
            return true;
        }
    }

    @NbBundle.Messages("csg=Constructive Geometry")
    static class CSGAction extends ShapeAction {

        private final List<? extends ShapeElement> sh;
        private final CSGOperation op;

        public CSGAction(CSGOperation op, Point2D pt, ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(op.toString(), entry, coll, refreshUi);
            this.op = op;
            sh = coll.shapesAtPoint(pt.getX(), pt.getY());
            sh.remove(entry);
            setEnabled(sh.size() >= 1);
        }

        @Override
        protected boolean run() {
            assert sh.size() >= 1;
            ShapeElement first = entry;
            coll.csg(op, first, sh, (removed, added) -> {
                System.out.println("CSG COMPLETE " + removed + " added " + added);
            });
            return true;
        }
    }

    @NbBundle.Messages("toFront=To &Front")
    static class ToFrontAction extends ShapeAction {

        public ToFrontAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.toFront(), entry, coll, refreshUi);
        }

        @Override
        protected boolean run() {
            coll.toFront(entry);
            return true;
        }
    }

    @NbBundle.Messages("toBack=To &Back")
    static class ToBackAction extends ShapeAction {

        public ToBackAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.toBack(), entry, coll, refreshUi);
        }

        @Override
        protected boolean run() {
            coll.toFront(entry);
            return true;
        }
    }

    @NbBundle.Messages("tesselate=&Tesselate")
    static class TesselateTriangleAction extends ShapeAction {

        public TesselateTriangleAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.tesselate(), entry, coll, refreshUi);
            setEnabled(entry.item().as(TriangleWrapper.class) != null);
        }

        @Override
        protected boolean run() {
            entry.item().as(TriangleWrapper.class, tri -> {
                TriangleWrapper[] two = tri.tesselate();
                coll.replaceShape(entry, two);
            });
            return true;
        }
    }

    @NbBundle.Messages("toPath=Convert To &Path")
    static final class ConvertToPathAction extends ShapeAction {

        public ConvertToPathAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.toPath(), entry, coll, refreshUi);
            setEnabled(!entry.isPaths());
        }

        @Override
        protected boolean run() {
            entry.toPaths();
            return true;
        }
    }

    @NbBundle.Messages("toQuadCurves=To Quadratic Curves")
    static final class ToQuadCurvesAction extends ShapeAction {

        public ToQuadCurvesAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.toQuadCurves(), entry, coll, refreshUi);
            setEnabled(entry.isPaths());
        }

        @Override
        protected boolean run() {
            entry.item().as(PathIteratorWrapper.class, piw -> {
                piw.toQuadSplines();
            });
            return true;
        }
    }

    @NbBundle.Messages("toCubicCurves=To Cubic Curves")
    static final class ToCubicCurvesAction extends ShapeAction {

        public ToCubicCurvesAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.toCubicCurves(), entry, coll, refreshUi);
            setEnabled(entry.isPaths());
        }

        @Override
        protected boolean run() {
            entry.item().as(PathIteratorWrapper.class, piw -> {
                piw.toCubicSplines();
            });
            return true;
        }
    }

    @NbBundle.Messages("resetControlPoints=Reset Control Points")
    static final class ResetControlPointsAction extends ShapeAction {

        public ResetControlPointsAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.resetControlPoints(), entry, coll, refreshUi);
            setEnabled(entry.isPaths() && entry.item().as(PathIteratorWrapper.class).containsSplines());
        }

        @Override
        protected boolean run() {
            entry.item().as(PathIteratorWrapper.class, piw -> {
                piw.resetControlPoints();
            });
            return true;
        }
    }

    @NbBundle.Messages({
        "# {0} - pointKind",
        "addPoint=Add {0} Point",
        "add=Add Points"
    })
    static final class AddPointAction extends ShapeAction {

        private final ControlPointKind kind;
        private final Point2D near;

        public AddPointAction(ControlPointKind kind, Point2D near, ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.addPoint(kind), entry, coll, refreshUi);
            this.kind = kind;
            this.near = near;
            setEnabled(entry.item().as(PathIteratorWrapper.class) != null);
        }

        @Override
        protected boolean run() {
            return kind.addDefaultPoint(entry.item().as(PathIteratorWrapper.class), near);
        }

    }

    @NbBundle.Messages({
        "# {0} - degrees",
        "rotateDegrees=Rotate {0}\u00B0",
        "rotateSubmenu=&Rotate"
    })
    static final class RotateAction extends ShapeAction {

        private final AffineTransform xform;

        public RotateAction(int degrees, ShapeElement entry, ShapesCollection coll, Runnable refreshUi) {
            super(Bundle.rotateDegrees(degrees), entry, coll, refreshUi);
            double rad = Math.toRadians(degrees);
            Rectangle2D r = entry.shape().getBounds2D();
            xform = AffineTransform.getRotateInstance(rad, (float) r.getCenterX(), (float) r.getCenterY());
            setEnabled(entry.canApplyTransform(xform));
        }

        @Override
        protected boolean run() {
            entry.applyTransform(xform);
            return true;
        }
    }

    private ShapeActions() {
        throw new AssertionError();
    }
}
