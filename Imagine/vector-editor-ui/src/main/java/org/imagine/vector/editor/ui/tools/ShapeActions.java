package org.imagine.vector.editor.ui.tools;

import com.mastfrog.util.collections.IntSet;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JLabel;
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

    static Runnable revalidateRelated(Widget widget, ShapeElement lookFor) {
        return () -> {
            descendRelated(widget.getScene(), lookFor, Widget::revalidate);
            widget.revalidate();
            widget.getScene().validate();
        };
    }

    static int descendRelated(Widget into, ShapeElement lookFor, Consumer<Widget> c) {
        int result = 0;
        if (into.getLookup().lookupAll(ShapeElement.class).contains(lookFor)) {
            c.accept(into);
            result++;
        }
        for (Widget child : into.getChildren()) {
            result += descendRelated(child, lookFor, c);
        }
        return result;
    }

    static JPopupMenu populatePopup(
            ControlPoint pt, ShapeElement entry,
            ShapesCollection shapes, Runnable refresh,
            Widget widget, Point2D sceneLocation) {
        JPopupMenu result = populatePopup(entry, shapes,
                refresh, widget, sceneLocation);

        result.add(new JSeparator(), 0);
        Runnable revalidateHook = revalidateRelated(widget, entry);

        if (!pt.availableControlPointKinds().isEmpty()) {
            JMenu changeType = new JMenu();
            Mnemonics.setLocalizedText(changeType, Bundle.changeType());
            for (ControlPointKind k : pt.availableControlPointKinds()) {
                new ChangePointTypeAction(pt, k, entry, shapes, refresh, revalidateHook)
                        .addToMenu(changeType);
            }
            result.add(changeType, 0);
        }

        DeleteControlPointAction deletePoint
                = new DeleteControlPointAction(pt, entry, shapes, refresh, revalidateHook);
        deletePoint.addToMenu(result, 0);

        return result;
    }

    static JPopupMenu populatePopup(ShapeElement entry,
            ShapesCollection shapes, Runnable refresh,
            Widget widget, Point2D sceneLocation) {
        JPopupMenu menu = new JPopupMenu();

        String nm = entry.getName();
        JLabel lbl = new JLabel(nm);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        menu.add(lbl);
        menu.add(new JSeparator());

        Runnable revalidateHook = revalidateRelated(widget, entry);

        new ToFrontAction(entry, shapes, refresh, widget, revalidateHook).addToMenu(menu);
        new ToBackAction(entry, shapes, refresh, widget, revalidateHook).addToMenu(menu);
        menu.add(new JSeparator());
        new DuplicateShapeAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        menu.add(new JSeparator());

        new TesselateTriangleAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        new SimplifyAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        new ConvertToPathAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        new ToCubicCurvesAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        new ToQuadCurvesAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        new ResetControlPointsAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);

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
            ShapeAction a = new RotateAction(d, entry, shapes, refresh, revalidateHook);
            a.addToMenu(rotation);
        });
//        if (rotation.getComponentCount() > 0) {
        menu.add(rotation);
        menu.add(new JSeparator());
//        }
        if (sceneLocation != null) {
            JMenu addPoints = new JMenu();
            Mnemonics.setLocalizedText(addPoints, Bundle.add());
            for (ControlPointKind k : new ControlPointKind[]{ControlPointKind.LINE_TO_DESTINATION, ControlPointKind.CUBIC_CURVE_DESTINATION, ControlPointKind.QUADRATIC_CURVE_DESTINATION}) {
                new AddPointAction(k, sceneLocation, entry, shapes, refresh, revalidateHook).addToMenu(addPoints);
            }
            if (addPoints.getComponentCount() > 0) {
                menu.add(addPoints);
            }

            JMenu csg = new JMenu();
            Mnemonics.setLocalizedText(csg, Bundle.csg());
            for (CSGOperation op : CSGOperation.values()) {
                new CSGAction(op, sceneLocation, entry, shapes, refresh, revalidateHook).addToMenu(csg);
            }
            menu.add(csg);
        }

        menu.add(new JSeparator());

        new DeleteShapeAction(entry, shapes, refresh, revalidateHook).addToMenu(menu);
        return menu;

    }

    static abstract class ShapeAction extends AbstractAction {

        protected final ShapeElement entry;
        protected final ShapesCollection coll;
        private final Runnable refreshUi;
        private final EditKind kind;
        private final Runnable revalidateHook;

        ShapeAction(String name, ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            this(EditKind.SHAPE, name, entry, coll, refreshUi, revalidateHook);
        }

        ShapeAction(EditKind kind, String name, ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(name);
            this.kind = kind;
            this.entry = entry;
            this.coll = coll;
            this.refreshUi = refreshUi;
            this.revalidateHook = revalidateHook;
        }

        void addToMenu(JPopupMenu menu) {
            if (isEnabled()) {
                JMenuItem item = new JMenuItem(this);
                String name = (String) getValue(NAME);
                Mnemonics.setLocalizedText(item, name);
                menu.add(item);
            }
        }

        void addToMenu(JPopupMenu menu, int pos) {
            if (isEnabled()) {
                JMenuItem item = new JMenuItem(this);
                String name = (String) getValue(NAME);
                Mnemonics.setLocalizedText(item, name);
                menu.add(item, pos);
            }
        }

        void addToMenu(JMenu menu) {
            JMenuItem item = new JMenuItem(this);
            String name = (String) getValue(NAME);
            Mnemonics.setLocalizedText(item, name);
            menu.add(item);
        }

        /**
         * Return true if the scene should sync the set of widgets.
         *
         * @return True if the set of widgets may have changed and the scene
         * needs rebuilding
         */
        protected abstract boolean performAction();

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isEnabled()) {
                inEdit(this::performAction);
            }
        }

        private void inEdit(BooleanSupplier r) {
            String nm = entry.isNameSet() ? entry.getName() : ShapeNames.nameOf(entry.item());
            String undoName = getValue(NAME) + " " + nm;
            switch (kind) {
                case CONTENTS:
                    coll.contentsEdit(undoName, () -> {
                        if (r.getAsBoolean()) {
                            refreshUi.run();
                        }
                    }).hook(() -> {
                        revalidateHook.run();
                        refreshUi.run();
                    });
                    break;
                case GEOMETRY:
                    coll.geometryEdit(undoName, () -> {
                        if (r.getAsBoolean()) {
                            refreshUi.run();
                        }
                    }).hook(refreshUi);
                    break;
                case SHAPE:
                    coll.edit(undoName, entry, () -> {
                        if (r.getAsBoolean()) {
                            refreshUi.run();
                        }
                    }).hook(revalidateHook);
                    break;
                default:
                    throw new AssertionError(this);
            }
        }
    }

    static enum EditKind {
        CONTENTS,
        GEOMETRY,
        SHAPE
    }

    @Messages("deletePoint=Delete Point")
    static class DeleteControlPointAction extends ShapeAction {

        private final ControlPoint pt;

        public DeleteControlPointAction(ControlPoint pt, ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.deletePoint(), entry, coll, refreshUi, revalidateHook);
            this.pt = pt;
            setEnabled(pt.canDelete());
        }

        @Override
        protected boolean performAction() {
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
                ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(kind.toString(), entry, coll, refreshUi, revalidateHook);
            this.pt = pt;
            this.kind = kind;
            setEnabled(!pt.availableControlPointKinds().isEmpty());
        }

        @Override
        protected boolean performAction() {
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

        public DuplicateShapeAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(EditKind.CONTENTS, Bundle.duplicate(), entry, coll, refreshUi, revalidateHook);
        }

        @Override
        protected boolean performAction() {
            coll.duplicate(entry);
            return true;
        }

    }

    @NbBundle.Messages("delete=&Delete")
    static class DeleteShapeAction extends ShapeAction {

        public DeleteShapeAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(EditKind.CONTENTS, Bundle.delete(), entry, coll, refreshUi, revalidateHook);
        }

        @Override
        protected boolean performAction() {
            coll.deleteShape(entry);
            return true;
        }
    }

    @NbBundle.Messages("csg=Constructive Geometry")
    static class CSGAction extends ShapeAction {

        private final List<? extends ShapeElement> sh;
        private final CSGOperation op;

        public CSGAction(CSGOperation op, Point2D pt, ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(op.toString(), entry, coll, refreshUi, revalidateHook);
            this.op = op;
            sh = coll.shapesAtPoint(pt.getX(), pt.getY());
            sh.remove(entry);
            setEnabled(sh.size() >= 1);
        }

        @Override
        protected boolean performAction() {
            assert sh.size() >= 1;
            ShapeElement first = entry;
            coll.csg(op, first, sh, (removed, added) -> {
            });
            return true;
        }
    }

    @NbBundle.Messages("toFront=To &Front")
    static class ToFrontAction extends ShapeAction {

        private final Widget widget;

        public ToFrontAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Widget widget, Runnable revalidateHook) {
            super(EditKind.CONTENTS, Bundle.toFront(), entry, coll, refreshUi, revalidateHook);
            this.widget = widget;
            setEnabled(coll.indexOf(entry) != coll.size() - 1);
        }

        @Override
        protected boolean performAction() {
            coll.toFront(entry);
            widget.bringToFront();
            widget.revalidate();
            widget.getScene().validate();
            return true;
        }
    }

    @NbBundle.Messages("toBack=To &Back")
    static class ToBackAction extends ShapeAction {

        private final Widget widget;

        public ToBackAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Widget widget, Runnable revalidateHook) {
            super(EditKind.CONTENTS, Bundle.toBack(), entry, coll, refreshUi, revalidateHook);
            this.widget = widget;
            setEnabled(coll.indexOf(entry) != 0);
        }

        @Override
        protected boolean performAction() {
            coll.toBack(entry);
            widget.bringToBack();
            widget.revalidate();
            widget.getScene().validate();
            return false;
        }
    }

    @NbBundle.Messages("tesselate=&Tesselate")
    static class TesselateTriangleAction extends ShapeAction {

        public TesselateTriangleAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.tesselate(), entry, coll, refreshUi, revalidateHook);
            setEnabled(entry.item().is(TriangleWrapper.class));
        }

        @Override
        protected boolean performAction() {
            entry.item().as(TriangleWrapper.class, tri -> {
                TriangleWrapper[] two = tri.tesselate();
                coll.replaceShape(entry, two);
            });
            return true;
        }
    }

    @NbBundle.Messages("toPath=Convert To &Path")
    static final class ConvertToPathAction extends ShapeAction {

        public ConvertToPathAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.toPath(), entry, coll, refreshUi, revalidateHook);
            setEnabled(!entry.isPaths());
        }

        @Override
        protected boolean performAction() {
            entry.toPaths();
            return true;
        }
    }

    @NbBundle.Messages("toQuadCurves=To Quadratic Curves")
    static final class ToQuadCurvesAction extends ShapeAction {

        public ToQuadCurvesAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.toQuadCurves(), entry, coll, refreshUi, revalidateHook);
            setEnabled(entry.isPaths());
        }

        @Override
        protected boolean performAction() {
            entry.item().as(PathIteratorWrapper.class, piw -> {
                piw.toQuadSplines();
            });
            return true;
        }
    }

    @NbBundle.Messages("simplify=Simplify")
    static final class SimplifyAction extends ShapeAction {

        public SimplifyAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.simplify(), entry, coll, refreshUi, revalidateHook);
            setEnabled(entry.isPaths());
        }

        @Override
        protected boolean performAction() {
            entry.item().as(PathIteratorWrapper.class, piw -> {
                piw.simplify(5);
            });
            return true;
        }
    }

    @NbBundle.Messages("toCubicCurves=To Cubic Curves")
    static final class ToCubicCurvesAction extends ShapeAction {

        public ToCubicCurvesAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.toCubicCurves(), entry, coll, refreshUi, revalidateHook);
            setEnabled(entry.isPaths());
        }

        @Override
        protected boolean performAction() {
            entry.item().as(PathIteratorWrapper.class, piw -> {
                piw.toCubicSplines();
            });
            return true;
        }
    }

    @NbBundle.Messages("resetControlPoints=Reset Control Points")
    static final class ResetControlPointsAction extends ShapeAction {

        public ResetControlPointsAction(ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.resetControlPoints(), entry, coll, refreshUi, revalidateHook);
            setEnabled(entry.isPaths() && entry.item().as(PathIteratorWrapper.class).containsSplines());
        }

        @Override
        protected boolean performAction() {
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

        public AddPointAction(ControlPointKind kind, Point2D near, ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(EditKind.GEOMETRY, Bundle.addPoint(kind), entry, coll, refreshUi, revalidateHook);
            this.kind = kind;
            this.near = near;
            setEnabled(entry.item().is(PathIteratorWrapper.class));
        }

        @Override
        protected boolean performAction() {
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

        public RotateAction(int degrees, ShapeElement entry, ShapesCollection coll, Runnable refreshUi, Runnable revalidateHook) {
            super(Bundle.rotateDegrees(degrees), entry, coll, refreshUi, revalidateHook);
            double rad = Math.toRadians(degrees);
            Rectangle2D r = entry.shape().getBounds2D();
            xform = AffineTransform.getRotateInstance(rad, (float) r.getCenterX(), (float) r.getCenterY());
            setEnabled(entry.canApplyTransform(xform));
        }

        @Override
        protected boolean performAction() {
            entry.applyTransform(xform);
            return true;
        }
    }

    private ShapeActions() {
        throw new AssertionError();
    }
}
