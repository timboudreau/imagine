package org.imagine.vector.editor.ui.tools.widget.actions;

import com.mastfrog.util.collections.IntSet;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import org.imagine.editor.api.grid.Grid;
import org.imagine.vector.editor.ui.palette.PaintPalettes;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.CSGOperation;
import org.imagine.vector.editor.ui.tools.widget.DesignerControl;
import org.imagine.vector.editor.ui.tools.widget.actions.generic.ActionsBuilder;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.components.dialog.DialogBuilder;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class ShapeActions {

    private final ActionsBuilder actions;

    private ThreadLocal<Point> currentPoint = ThreadLocal.withInitial(Point::new);
    private final DesignerControl ctrl;

    @Messages({
        "actionToFront=To &Front",
        "# {0} - shapeName",
        "opMoveToFront=Bring {0} to Front",
        "actionToBack=To &Back",
        "# {0} - shapeName",
        "opMoveToBack=Send {0} to Back",
        "actionTesselate=&Tesselate",
        "# {0} - shapeName",
        "opTesselate=Tesselate {0}",
        "actionDeleteControlPoint=D&elete Control Point",
        "# {0} - pointIndex",
        "# {1} - shapeName",
        "opDeleteControlPoint=Delete Control Point {0} of {1}",
        "actionDeleteShape=D&elete Shape",
        "# {0} - shapeName",
        "opDeleteShape=Delete {0}",
        "submenuControlPoints=Add Point",
        "submenuScale=Scale",
        "submenuRotate=Rotate",
        "submenuCSG=Combine",
        "submenuChangePointType=Point Type",
        "actionAddLinePoint=Add &Straight Line Point",
        "actionAddQuadraticPoint=Add &Quadratic Point",
        "actionAddCubicPoint=Add &Cubic Point",
        "# {0} - degrees",
        "actionRotateMultiple=Rotate Multiple Shapes by {0}\u00B0",
        "# {0} - shapeName",
        "# {1} - degrees",
        "actionRotate=Rotate {0} by {1}\u00B0",
        "scale=Scale",
        "# {0} - percentage",
        "percentage={0}%",
        "# {0} - degrees",
        "rotateDegrees=Rotate {0}\u00B0",
        "rotateSubmenu=&Rotate",
        "# {0} - shapeName",
        "opDuplicate=Duplicate {0}",
        "actionDuplicate=&Duplicate",
        "actionToPaths=To Pat&h",
        "# {0} - shapeName",
        "opToPaths=Convert {0} to Path",
        "actionCut=C&ut",
        "# {0} - shapeName",
        "opCut=Cut {0}",
        "actionCopy=C&opy",
        "actionPaste=&Paste",
        "opPaste=Paste",
        "actionAddShapeToPalette=Add Shape to Palette",
        "actionAddFillToPalette=Add Fill to Palette",
        "submenuChangeControlPointType=Change Point Type",
        "# {0} - pointType",
        "opPointType=Change Type to {0}",
        "actionConvertToCurves=Convert to Cur&ves",
        "opConvertToCurves=Convert to Curves",
        "actionSetName=Set &Name",
        "name=&Name",
        "opSetName=Set Shape Name",
        "actionSetText=Set Te&xt",
        "opSetText=Edit Text",
        "actionsSnapAllPointsToGrid=Snap All Points to Nearest &Grid",
        "opSnapAllPointsToGrid=Snap All Points"
    })
    public ShapeActions(DesignerControl ctrl) {
        this.ctrl = ctrl;
        actions = new ActionsBuilder();

        actions.action(Bundle.actionToFront())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0))
                .dontHideWhenDisabled()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .testingBothWith((shape, shapes) -> {
                    return shapes.indexOf(shape) != shapes.size() - 1;
                })
                .sensitiveTo(Widget.class).sensingPresence()
                .sensitiveTo(ShapeControlPoint.class).sensingAbsence()
                .finish((element, shapes, widget, shouldBeNull) -> {
                    assert shouldBeNull == null;
                    shapes.contentsEdit(Bundle.opMoveToFront(element.getName()), () -> {
                        if (shapes.toFront(element)) {
                            widget.bringToFront();
                            widget.revalidate();
                            widget.getScene().validate();
                            widget.getScene().repaint();
                        }
                    });
                });

        actions.action(Bundle.actionToBack())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0))
                .separatorAfter()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .testingBothWith((shape, shapes) -> {
                    return shapes.indexOf(shape) != 0;
                })
                .sensitiveTo(Widget.class).sensingPresence()
                .sensitiveTo(ShapeControlPoint.class).sensingAbsence()
                .finish((element, shapes, widget, shouldBeNull) -> {
                    assert shouldBeNull == null;
                    shapes.contentsEdit(Bundle.opMoveToBack(element.getName()), () -> {
                        if (shapes.toBack(element)) {
                            widget.bringToBack();
                            widget.revalidate();
                            widget.getScene().validate();
                        }
                    });
                });

        actions.action(Bundle.actionCut())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_X, Utilities.isMac() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK))
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_CUT, 0))
                .separatorBefore()
                // XXX need a clipboard type for multiple elements
                .sensitiveTo(ShapesCollection.class).sensingExactlyOne()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .finish((shapes, element) -> {
                    Transferable xfer = PaintPalettes.createTransferable(element);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(xfer, xfer instanceof ClipboardOwner ? (ClipboardOwner) xfer : new ClipboardOwner() {
                        @Override
                        public void lostOwnership(Clipboard clipboard, Transferable contents) {
                            // XXX need dummy impl
                        }
                    });
                    shapes.contentsEdit(Bundle.opCut(element.getName()), () -> {
                        shapes.deleteShape(element);
                    });
                });

        actions.action(Bundle.actionCopy())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, Utilities.isMac() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK))
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_COPY, 0))
                // XXX need a clipboard type for multiple elements
                .sensitiveTo(ShapesCollection.class).sensingExactlyOne()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .finish((shapes, element) -> {
                    Transferable xfer = PaintPalettes.createTransferable(element);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(xfer, xfer instanceof ClipboardOwner ? (ClipboardOwner) xfer : new ClipboardOwner() {
                        @Override
                        public void lostOwnership(Clipboard clipboard, Transferable contents) {
                            // XXX need dummy impl
                        }
                    });
                });

        actions.action(Bundle.actionPaste())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, Utilities.isMac() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK))
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PASTE, 0))
                .dontHideWhenDisabled()
                .separatorAfter()
                .sensitiveTo(ShapesCollection.class)
                .testingOne(ignored -> {
                    Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                    return PaintPalettes.containsShapeElement(xfer);
                })
                .finish(shapes -> {
                    try {
                        ShapeElement el = PaintPalettes.shapeElementFromTransferable(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this));
                        shapes.contentsEdit(Bundle.opPaste(), () -> {
                            ShapeElement actual = shapes.addForeign(el);
                            ctrl.shapeAdded(actual);
                        }).hook(ctrl::shapesMayBeDeleted);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });

        // XXX this should be paste fill?
//        actions.action(Bundle.actionPaste())
//                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, Utilities.isMac() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK))
//                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_COPY, 0))
//                // XXX need a clipboard type for multiple elements
//                .sensitiveTo(ShapesCollection.class).testingOne(ignored -> {
//            Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
//            List<DataFlavor> flavors = Arrays.asList(xfer.getTransferDataFlavors());
//            Set<DataFlavor> supported = new HashSet<>(PaintPalettes.shapeMimeTypes());
//            supported.retainAll(flavors);
//            return !supported.isEmpty();
//        }).finish(shapes -> {
//            shapes.contentsEdit(Bundle.opPaste(), () -> {
//                try {
//                    ShapeElement el = PaintPalettes.shapeElementFromTransferable(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this));
//                    if (el != null) {
//                        el = shapes.addForeign(el);
//                    }
//                    ctrl.shapeAdded(el);
//                } catch (IOException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
//            }).hook(ctrl::shapesMayBeDeleted);
//        });
        actions.action(Bundle.actionsSnapAllPointsToGrid())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0))
                .sensitiveTo(ShapeElement.class)
                .testingEach(el -> {
                    // No grid, nothing to snap to
                    if (!Grid.getInstance().isEnabled()) {
                        return false;
                    }
                    Adjustable adj = el.item().as(Adjustable.class);
                    if (adj == null) {
                        return false;
                    }
                    if (!adj.hasReadOnlyControlPoints()) {
                        int ct = adj.getControlPointCount();
                        for (int i = 0; i < ct; i++) {
                            if (!adj.isControlPointReadOnly(i)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }).sensitiveTo(ShapesCollection.class).sensingPresence()
                .finishMultiple((items, colls) -> {
                    ShapesCollection c = colls.iterator().next();
                    Grid grid = Grid.getInstance();
                    Set<ShapeElement> allChanged = new HashSet<>();
                    c.geometryEdit(Bundle.opSnapAllPointsToGrid(), () -> {
                        boolean[] changed = new boolean[1];
                        for (ShapeElement entry : items) {
                            ShapeControlPoint[] cps = entry.controlPoints(0, cp -> {
                                changed[0] = true;
                                allChanged.add(entry);
                            });
                            for (int i = 0; i < cps.length; i++) {
                                ShapeControlPoint cp = cps[i];
                                if (cp.isEditable()) {
                                    Point2D pos = grid.nearestPointTo(cp.getX(), cp.getY());
                                    cp.set(pos.getX(), pos.getY());
                                }
                            }
                            if (changed[0]) {
                                ctrl.shapeGeometryChanged(entry);
                            }
                        }
                    }).hook(() -> {
                        for (ShapeElement el : allChanged) {
                            ctrl.shapeGeometryChanged(el);
                        }
                    });
                });

        actions.action(Bundle.actionTesselate())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0))
                .sensitiveTo(ShapeElement.class).testingOne(se -> se.item().is(TriangleWrapper.class))
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .finish((entry, shapes) -> {
                    entry.item().as(TriangleWrapper.class, tri -> {
                        shapes.geometryEdit(Bundle.opTesselate(entry.getName()), () -> {
                            TriangleWrapper[] two = tri.tesselate();
                            shapes.replaceShape(entry, two);
                            ctrl.shapeMayBeAdded();
                            ctrl.shapesMayBeDeleted();
                        }).hook(() -> {
                            ctrl.shapesMayBeDeleted();
                        });
                    });
                });

        actions.action(Bundle.actionSetText())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0))
                .sensitiveTo(ShapesCollection.class)
                .sensingPresence()
                .sensitiveTo(ShapeElement.class)
                .testingOne(se -> {
                    return se.item().is(Text.class);
                }).finish((shapes, text) -> {
            DialogBuilder.forName("shapeText")
                    .setTitle(Bundle.opSetText())
                    .modal().showMultiLineTextLineDialog(text.item().as(Text.class).getText(), 1, 768, newText -> {
                        shapes.edit(Bundle.opSetText(), text, () -> {
                            text.item().as(Text.class).setText(newText);
                            text.changed();
                            ctrl.pointCountMayBeChanged(text);
                            ctrl.shapeGeometryChanged(text);
                        }).hook(() -> {
                            text.changed();
                            ctrl.pointCountMayBeChanged(text);
                            ctrl.shapeGeometryChanged(text);
                        });
                    });
        });
        ;

        actions.action(Bundle.actionConvertToCurves())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0))
                .sensitiveTo(ShapeElement.class).testingOne((ShapeElement se) -> {
            if (se.isPaths()) {
                PathIteratorWrapper it = se.item().as(PathIteratorWrapper.class);
                if (it == null) {
                    // won't be now, but could
                    return false;
                }
                return !it.containsSplines();
            }
            return false;
        }).sensitiveTo(ShapesCollection.class).sensingExactlyOne()
                .finish((shape, shapes) -> {
                    PathIteratorWrapper w = shape.item().as(PathIteratorWrapper.class);
                    assert w != null;
                    shapes.edit(Bundle.opConvertToCurves(), shape, () -> {
                        w.toCubicSplines();
                        ctrl.shapeGeometryChanged(shape);
                        ctrl.pointCountMayBeChanged(shape);
                    }).hook(() -> {
                        ctrl.shapeGeometryChanged(shape);
                        ctrl.pointCountMayBeChanged(shape);
                    });
                });

        actions.submenu(Bundle.submenuChangeControlPointType(), a -> {
            for (ControlPointKind k : ControlPointKind.values()) {
                if (k.isPathComponent()) {
                    a.action(k.toString())
                            .sensitiveTo(ShapesCollection.class).sensingExactlyOne()
                            .sensitiveTo(ShapeElement.class).testingEach(ShapeElement::isPaths)
                            .sensitiveTo(ShapeControlPoint.class).testingEach(pt -> {
                        return pt.availableControlPointKinds().contains(k);
                    }).finish((shapes, element, cp) -> {
                        shapes.edit(Bundle.opPointType(k), element, () -> {
                            boolean result = k.changeType(element.item().as(PathIteratorWrapper.class), cp.index());
                            if (result) {
                                ctrl.shapeGeometryChanged(element);
                                ctrl.pointCountMayBeChanged(element);
                            }
                        });
                    });
                }
            }
        });

        actions.submenu(Bundle.submenuControlPoints(), a -> {
            for (ControlPointKind k : new ControlPointKind[]{ControlPointKind.LINE_TO_DESTINATION, ControlPointKind.CUBIC_CURVE_DESTINATION, ControlPointKind.QUADRATIC_CURVE_DESTINATION}) {
                a.action(k.toString())
                        .sensitiveTo(ShapeElement.class)
                        .testingOne(element -> {
                            return element.isPaths()
                                    && element.item().is(PathIteratorWrapper.class);
                        }).finish(element -> {
                    if (k.addDefaultPoint(element.item().as(PathIteratorWrapper.class), lastPopupLocation)) {
                        ctrl.pointCountMayBeChanged(element);
                        ctrl.shapeGeometryChanged(element);
                    }
                });
            }
        });

        actions.submenu(Bundle.scale(), a -> {
            for (float by : new float[]{0.25F, 0.5F, 0.75F, 0.9F, 1.25F, 1.5F, 2, 2.5F, 3}) {
                AffineTransform xform = AffineTransform.getScaleInstance(by, by);
                String name = Bundle.percentage((int) Math.round(by * 100F));
                a.action(name).sensitiveTo(ShapeElement.class)
                        .testingAll(shapes -> {
                            for (ShapeElement el : shapes) {
                                if (!el.canApplyTransform(xform)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .sensitiveTo(ShapesCollection.class)
                        .sensingPresence()
                        .dontHideWhenDisabled()
                        .finishMultiple((allShapes, collections) -> {
                            Set<ShapesCollection> all = new HashSet<>(collections);
                            assert all.size() == 1;
                            ShapesCollection shapes = all.iterator().next();
                            if (allShapes.size() == 1) {
                                ShapeElement entry = allShapes.iterator().next();
                                shapes.edit("Scale " + entry.getName(), entry, () -> {
                                    Rectangle2D.Double oldBounds = new Rectangle2D.Double();
                                    entry.addToBounds(oldBounds);
                                    entry.applyTransform(xform);
                                    Rectangle2D.Double newBounds = new Rectangle2D.Double();
                                    entry.addToBounds(newBounds);
                                    double dx = oldBounds.getCenterX() - newBounds.getCenterX();
                                    double dy = oldBounds.getCenterY() - newBounds.getCenterY();
                                    AffineTransform xlate = AffineTransform.getTranslateInstance(dx, dy);
                                    entry.applyTransform(xlate);

                                    ctrl.shapeGeometryChanged(entry);
                                });
                            } else {
                                shapes.geometryEdit("Scale Multiple", () -> {
                                    for (ShapeElement entry : allShapes) {
                                        Rectangle2D.Double oldBounds = new Rectangle2D.Double();
                                        entry.addToBounds(oldBounds);
                                        entry.applyTransform(xform);
                                        Rectangle2D.Double newBounds = new Rectangle2D.Double();
                                        entry.addToBounds(newBounds);
                                        double dx = oldBounds.getCenterX() - newBounds.getCenterX();
                                        double dy = oldBounds.getCenterY() - newBounds.getCenterY();
                                        AffineTransform xlate = AffineTransform.getTranslateInstance(dx, dy);
                                        entry.applyTransform(xlate);

                                        ctrl.shapeGeometryChanged(entry);
                                    }
                                });
                            }
                        });
            }
        });

        actions.submenu(Bundle.submenuRotate(), a -> {
            IntSet ints = IntSet.create(12);
            for (int i = 45; i < 360; i += 45) {
                ints.add(i);
            }
            for (int i = 30; i < 360; i += 30) {
                ints.add(i);
            }
            ints.forEach((IntConsumer) degrees -> {
                String name = Bundle.rotateDegrees(degrees);
                double rad = Math.toRadians(degrees);
                // We need the raw (no center point) transform to call
                // Vector.canApplyTransform() to
                AffineTransform rawRotate = AffineTransform.getRotateInstance(rad, rad);
                a.action(name).dontHideWhenDisabled()
                        .sensitiveTo(ShapeElement.class).testingEach(el -> {
                    return el.canApplyTransform(rawRotate);
                }).sensitiveTo(ShapesCollection.class).sensingPresence()
                        .finishMultiple((allShapes, coll) -> {
                            Set<ShapesCollection> s = new HashSet<>(coll);
                            assert s.size() == 1;
                            ShapesCollection shapes = s.iterator().next();
                            Rectangle2D.Double aggregateBounds = new Rectangle2D.Double();
                            if (allShapes.size() == 1) {
                                ShapeElement only = allShapes.iterator().next();
                                only.addToBounds(aggregateBounds);
                                AffineTransform realXform = AffineTransform.getRotateInstance(degrees, aggregateBounds.getCenterX(), aggregateBounds.getCenterY());
                                shapes.edit(Bundle.actionRotate(only.getName(), degrees), only, () -> {
                                    only.applyTransform(realXform);
                                    ctrl.shapeGeometryChanged(only);
                                });
                            } else {
                                // find the aggregate center point
                                for (ShapeElement el : allShapes) {
                                    el.addToBounds(aggregateBounds);
                                }
                                AffineTransform realXform = AffineTransform.getRotateInstance(degrees, aggregateBounds.getCenterX(), aggregateBounds.getCenterY());
                                shapes.geometryEdit(Bundle.actionRotateMultiple(degrees), () -> {
                                    for (ShapeElement el : allShapes) {
                                        el.applyTransform(realXform);
                                        ctrl.shapeGeometryChanged(el);
                                    }
                                });
                            }
                        });
            });
        });

        actions.submenu(Bundle.submenuCSG(), a -> {
            for (CSGOperation op : CSGOperation.values()) {
                a.action(op.toString()).dontHideWhenDisabled()
                        .sensitiveTo(ShapeElement.class).sensingPresence()
                        .sensitiveTo(ShapesCollection.class).sensingPresence()
                        .testingBothWith((element, shapes) -> {
                            List<? extends ShapeElement> atPoint = shapes.shapesAtPoint(lastPopupLocation.x, lastPopupLocation.y);
                            return atPoint.size() > 1;
                        }).finish((element, shapes) -> {
                    List<? extends ShapeElement> atPoint = shapes.shapesAtPoint(lastPopupLocation.x, lastPopupLocation.y);
                    atPoint.remove(element);
                    Set<ShapeElement> allRemoved = new HashSet<>(atPoint.size() + 1);
                    Set<ShapeElement> allAdded = new HashSet<>(1);
                    shapes.contentsEdit(op.toString(), () -> {
                        shapes.csg(op, element, atPoint, (Set<? extends ShapeElement> removed, ShapeElement added) -> {
                            if (added != null) {
                                allAdded.add(added);
                            }
                            allRemoved.addAll(removed);
                        });
                    });
                    if (!allAdded.isEmpty()) {
                        for (ShapeElement el : allAdded) {
                            ctrl.shapeAdded(el);
                        }
                    }
                    if (!allRemoved.isEmpty()) {
                        for (ShapeElement el : allRemoved) {
                            ctrl.shapeDeleted(el);
                        }
                    }
                    if (shapes.indexOf(element) >= 0) {
                        ctrl.shapeGeometryChanged(element);
                    }
                });
            }
        });

        actions.action(Bundle.actionToPaths())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0))
                .sensitiveTo(ShapeElement.class)
                .testingEach(el -> {
                    return !el.isPaths();
                }).sensitiveTo(ShapesCollection.class).sensingPresence()
                .finishMultiple((elements, allShapes) -> {
                    Set<ShapesCollection> s = new HashSet<>(allShapes);
                    Set<ShapeElement> all = new HashSet<>(elements);
                    assert s.size() == 1;
                    ShapesCollection shapes = s.iterator().next();
                    if (all.size() == 1) {
                        ShapeElement only = elements.iterator().next();
                        shapes.edit(Bundle.opToPaths(only.getName()), only, () -> {
                            only.toPaths();
                        });
                        ctrl.shapeGeometryChanged(only);
                        ctrl.pointCountMayBeChanged(only);
                    } else {
                        shapes.geometryEdit(Bundle.opToPaths(""), () -> {
                            for (ShapeElement current : all) {
                                current.toPaths();
                                ctrl.shapeGeometryChanged(current);
                                ctrl.pointCountMayBeChanged(current);
                            }
                        });
                    }
                });

        actions.action(Bundle.actionDuplicate())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0))
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .sensitiveTo(ShapeElement.class).sensingExactlyOne()
                .finish((shapes, element) -> {
                    shapes.contentsEdit(Bundle.opDuplicate(element.getName()), () -> {
                        ShapeElement nue = shapes.duplicate(element);
                        ctrl.shapeAdded(nue);
                        ctrl.updateSelection(nue);
                    });
                });

        actions.action(Bundle.actionAddShapeToPalette())
                .separatorBefore()
                .sensitiveTo(ShapeElement.class).sensingExactlyOne()
                .finish((element) -> {
                    ShapeElement dup = element.copy();
                    dup.changed();
                    Rectangle2D.Double rect = new Rectangle2D.Double();
                    dup.addToBounds(rect);
                    AffineTransform xf = AffineTransform.getTranslateInstance(-rect.x, -rect.y);
                    dup.applyTransform(xf);
                    dup.changed();
                    PaintPalettes.addToShapePalette(dup);
                });

        actions.action(Bundle.actionAddFillToPalette())
                .separatorAfter()
                .sensitiveTo(ShapeElement.class)
                .testingOne(el -> {
                    return el.isFill() && el.getFillKey() != null;
                })
                .finish((element) -> {
                    PaintPalettes.addToPaintPalette(element.getFillKey());
                });

        actions.action(Bundle.actionSetName())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0))
                .separatorAfter()
                .sensitiveTo(ShapeElement.class)
                .sensingExactlyOne()
                .finish(el -> {
                    DialogBuilder.forName("shapeName")
                            .setTitle(Bundle.opSetName())
                            .modal()
                            .showTextLineDialog(el.getName(), 1, 60, el::setName);
                });

        actions.action(Bundle.actionDeleteControlPoint())
                .separatorBefore()
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .sensitiveTo(ShapeControlPoint.class).testingEach(ShapeControlPoint::canDelete)
                .finish(this::doDeleteControlPoint);

        actions.action(Bundle.actionDeleteShape())
                .separatorBefore()
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .finish(this::doDeleteShape);
    }

    private void doDeleteShape(ShapeElement element, ShapesCollection shapes) {
        shapes.contentsEdit(Bundle.opDeleteShape(element.getName()), () -> {
            if (shapes.deleteShape(element)) {
                boolean res = ctrl.shapeDeleted(element);
            }
        });
    }

    private void doDeleteControlPoint(ShapeElement element, ShapesCollection shapes, ShapeControlPoint controlPoint) {
        if (controlPoint == null) {
            doDeleteShape(element, shapes);
            return;
        }
        shapes.edit(Bundle.opDeleteControlPoint(controlPoint.index(), element.getName()), element, () -> {
            if (controlPoint.canDelete() && controlPoint.delete()) {
                ctrl.controlPointDeleted(controlPoint);
            }
        });
    }

    public void applyKeyboardActions(Widget widget) {
        actions.applyToWidget(widget);
    }

    public WidgetAction popupMenuAction() {
        return ActionFactory.createPopupMenuAction(this::menu);
    }

    private Point lastPopupLocation = new Point();

    public JPopupMenu menu(Widget widget, Point localLocation) {
        if (localLocation != null) {
            lastPopupLocation.setLocation(widget.convertLocalToScene(localLocation));
        }
        JPopupMenu menu = new JPopupMenu();
        currentPoint.set(localLocation);
        try {
            actions.applyToPopup(menu, widget.getLookup());
        } finally {
            currentPoint.remove();
        }
        return menu;
    }

}
