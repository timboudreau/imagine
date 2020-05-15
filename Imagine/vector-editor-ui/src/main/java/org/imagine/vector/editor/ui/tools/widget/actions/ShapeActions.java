package org.imagine.vector.editor.ui.tools.widget.actions;

import com.mastfrog.util.collections.IntSet;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Centered;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Textual;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.editor.api.grid.Grid;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.PooledTransform;
import org.imagine.vector.editor.ui.palette.PaintPalettes;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.spi.ZSync;
import org.imagine.vector.editor.ui.tools.CSGOperation;
import org.imagine.vector.editor.ui.tools.widget.DesignerControl;
import org.imagine.vector.editor.ui.tools.widget.actions.generic.ActionsBuilder;
import org.imagine.vector.editor.ui.tools.widget.util.ViewL;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.components.dialog.DialogBuilder;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
public class ShapeActions {

    private final ActionsBuilder actions;

    private ThreadLocal<Point> currentPoint = ThreadLocal.withInitial(Point::new);
    private ThreadLocal<EqPointDouble> currentPoint2D = ThreadLocal.withInitial(EqPointDouble::new);
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
        "opSnapAllPointsToGrid=Snap All Points",
        "actionFlipHorizontal=Flip &Horizontal",
        "opFlipHorizontal=Flip Horizontal",
        "actionFlipVertical=&Flip Vertical",
        "opFlipVertical=Flip Vertical",
        "actionConvertToPathText=Convert to Text Path",
        "opConvertToPathText=Convert to Text Path",
        "actionCenterOnCanvas=Center on Canvas",
        "# {0} - name",
        "opCenterOnCanvas=Center {0}",
        "actionTransformFill=Transform Fill",
        "# {0} - name",
        "opTransformFill=Transform Fill on {0}",
        "actionMoveUp=Move Up",
        "# {0} - name",
        "opMoveUp=Move {0} Up",
        "actionMoveDown=Move Down",
        "# {0} - name",
        "opMoveDown=Move {0} Down",
        "actionArbitraryScale=Arbt&rary Scale",
        "opArbitraryScale=Change Scale on {0}",
        "opArbitraryScaleMultiple=Change Scale on Multiple Shapes",
        "dlgAdjustScale=Scale & Transform",})
    public ShapeActions(DesignerControl ctrl) {
        this.ctrl = ctrl;
        actions = new ActionsBuilder();

        actions.action(Bundle.actionToFront())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0))
                .dontHideWhenDisabled()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .testingBothWith((shape, shapes) -> {
                    return shapes.canMoveToFront(shape);
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
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .testingBothWith((shape, shapes) -> {
                    return shapes.canMoveToBack(shape);
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

        actions.action(Bundle.actionMoveUp())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0))
                .separatorAfter()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .testingBothWith((shape, shapes) -> {
                    return shapes.canMoveUp(shape);
                })
                .sensitiveTo(Widget.class).sensingPresence()
                .sensitiveTo(ShapeControlPoint.class).sensingAbsence()
                .finish((element, shapes, widget, shouldBeNull) -> {
                    assert shouldBeNull == null;
                    shapes.contentsEdit(Bundle.opMoveUp(element.getName()), () -> {
                        if (shapes.moveForward(element)) {
//                            widget.bringToBack();
                            ZSync sync = widget.getLookup().lookup(ZSync.class);
                            if (sync != null) {
                                sync.syncZOrder();
                            }
                            widget.revalidate();
                            widget.getScene().validate();
                        }
                    });
                });

        actions.action(Bundle.actionMoveDown())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0))
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .testingBothWith((shape, shapes) -> {
                    return shapes.canMoveBack(shape);
                })
                .sensitiveTo(Widget.class).sensingPresence()
                .sensitiveTo(ShapeControlPoint.class).sensingAbsence()
                .finish((element, shapes, widget, shouldBeNull) -> {
                    assert shouldBeNull == null;
                    shapes.contentsEdit(Bundle.opMoveDown(element.getName()), () -> {
                        if (shapes.moveBack(element)) {
                            ZSync sync = widget.getLookup().lookup(ZSync.class);
                            if (sync != null) {
                                sync.syncZOrder();
                            }
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
                .sensitiveTo(ShapesCollection.class).sensingPresence()
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
                .sensitiveTo(ShapesCollection.class).sensingPresence()
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

        final AffineTransform testTransform = AffineTransform.getTranslateInstance(1, 1);
        actions.action(Bundle.actionCenterOnCanvas())
                .dontHideWhenDisabled()
                .sensitiveTo(ShapeElement.class).testingOne(shape -> {
            return shape.item().is(Centered.class)
                    && shape.item().is(Transformable.class)
                    && shape.canApplyTransform(testTransform);
        }).sensitiveTo(Picture.class).sensingPresence()
                .sensitiveTo(Layer.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .finish((entry, picture, layer, coll) -> {
                    Dimension d = picture.getSize();
                    Rectangle r = layer.getBounds();
                    if (r.width < d.width) {
                        r.width = d.width;
                    }
                    if (r.height < d.height) {
                        r.height = d.height;
                    }
                    double cx = r.getCenterX();
                    double cy = r.getCenterY();
                    EqPointDouble center = entry.item().as(Centered.class).center();
                    double offX = center.x - cx;
                    double offY = center.y - cy;
                    if (offX != 0 || offY != 0) {
                        coll.edit(Bundle.opCenterOnCanvas(entry.getName()), entry, () -> {
                            PooledTransform.withTranslateInstance(-offX, -offY, xf -> {
                                entry.applyTransform(xf);
                            });
//                            entry.applyTransform(AffineTransform.getTranslateInstance(-offX, -offY));
                        });
                    }
                });

        actions.action(Bundle.actionSetText())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0))
                .sensitiveTo(ShapesCollection.class)
                .sensingPresence()
                .sensitiveTo(ShapeElement.class)
                .testingOne(se -> {
                    return se.item().is(Textual.class);
                }).finish((shapes, text) -> {
            DialogBuilder.forName("shapeText")
                    .setTitle(Bundle.opSetText())
                    .modal().showMultiLineTextLineDialog(text.item().as(Textual.class).getText(), 1, 768, newText -> {
                        shapes.edit(Bundle.opSetText(), text, (abortable) -> {
                            Textual txt = text.item().as(Textual.class);
                            if (!Objects.equals(txt.getText(), text)) {
                                txt.setText(newText);
                                text.changed();
                                ctrl.pointCountMayBeChanged(text);
                                ctrl.shapeGeometryChanged(text);
                            } else {
                                abortable.abort();
                            }
                        }).hook(() -> {
                            text.changed();
                            ctrl.pointCountMayBeChanged(text);
                            ctrl.shapeGeometryChanged(text);
                        });
                    });
        });

        AffineTransform testScale = AffineTransform.getScaleInstance(-1, 1);
        actions.action(Bundle.actionFlipHorizontal())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0))
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .sensitiveTo(ShapeElement.class)
                .testingEach(el -> {
                    return el.canApplyTransform(testScale);
                }).finish((shapes, el) -> {
            shapes.edit(Bundle.opFlipHorizontal(), el, () -> {
                Rectangle2D.Double origBounds = new Rectangle2D.Double();
                el.addToBounds(origBounds);
                PooledTransform.withScaleInstance(-1, 1, xf -> {
                    if (el.item().is(Text.class) && el.item().as(Text.class).transform() == null) {
                        // This works only for text, which internally diddles
                        // with transforms
                        el.applyTransform(xf);
                        el.changed();
                        PooledTransform.withTranslateInstance((-origBounds.getX() * 2) - origBounds.getWidth(), 0, xl -> {
                            el.applyTransform(xl);
                        });
                        el.applyTransform(AffineTransform.getTranslateInstance((-origBounds.getX() * 2) - origBounds.getWidth(), 0));
                        el.changed();
                    } else {
                        // XXX if a PathIteratorWrapper, maybe center
                        // on the non-virtual points?
                        el.applyTransform(xf);
                        el.changed();
                        el.translate((origBounds.x * 2) + origBounds.width, 0);
                    }
                });
//                AffineTransform xf = AffineTransform.getScaleInstance(-1, 1);
//                if (el.item().is(Text.class) && el.item().as(Text.class).transform() == null) {
//                    // This works only for text, which internally diddles
//                    // with transforms
//                    el.applyTransform(xf);
//                    el.changed();
//                    el.applyTransform(AffineTransform.getTranslateInstance((-origBounds.getX() * 2) - origBounds.getWidth(), 0));
//                    el.changed();
//                } else {
//                    // XXX if a PathIteratorWrapper, maybe center
//                    // on the non-virtual points?
//                    el.applyTransform(xf);
//                    el.changed();
//                    el.translate((origBounds.x * 2) + origBounds.width, 0);
//                }
                ctrl.shapeGeometryChanged(el);
            }).hook(() -> {
                ctrl.shapeGeometryChanged(el);
            });
        });

        actions.action(Bundle.actionTransformFill())
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .sensitiveTo(ShapeElement.class).testingEach((el) -> {
            if (!el.isFill()) {
                return false;
            }
            PaintKey<?> fk = el.getFillKey();
            if (fk == null || !fk.isTransformable()) {
                return false;
            }
            return true;
        }).finish((coll, el) -> {
            Customizer<AffineTransform> c = Customizers.getCustomizer(AffineTransform.class, "transformFill");
            if (c == null) {
                throw new IllegalStateException("Affine transform customizer is missing");
            }
            JComponent comp = c.getComponent();
            AffineTransform xform = DialogBuilder.forName("transformFill").okCancel()
                    .setTitle(Bundle.actionTransformFill())
                    .forContent(comp).openDialog(cp -> {
                return c.get();
            });
            if (xform != null && !xform.isIdentity()) {
                coll.edit(Bundle.opTransformFill(el.getName()), el, () -> {
                    PaintKey<?> fill = el.getFillKey();
                    if (fill instanceof TexturedPaintWrapperKey<?, ?>) {
                        fill = ((TexturedPaintWrapperKey<?, ?>) fill).delegate();
                    }
                    fill = fill.createTransformedCopy(xform);
                    el.setFill(fill.toPaint());
                });
            }
        });

        actions.action(Bundle.actionFlipVertical())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0))
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .sensitiveTo(ShapeElement.class)
                .testingEach(el -> {
                    return el.canApplyTransform(testScale);
                }).finish((shapes, el) -> {
            shapes.edit(Bundle.opFlipVertical(), el, () -> {
                Rectangle2D.Double origBounds = new Rectangle2D.Double();
                el.addToBounds(origBounds);
                PooledTransform.withScaleInstance(1, -1, xf -> {
                    if (el.item().is(Text.class) && el.item().as(Text.class).transform() == null) {
                        // This works only for text, which internally diddles
                        // with transforms
                        el.applyTransform(xf);
                        el.changed();
                        PooledTransform.withTranslateInstance(0, (-origBounds.getY() * 2) - origBounds.getHeight(), xlate -> {
                            el.applyTransform(xlate);
                        });
//                        el.applyTransform(AffineTransform.getTranslateInstance(0, (-origBounds.getY() * 2) - origBounds.getHeight()));
                        el.changed();
                    } else {
                        // XXX if a PathIteratorWrapper, maybe center
                        // on the non-virtual points?
                        el.applyTransform(xf);
                        el.changed();
                        el.translate(0, (origBounds.y * 2) + origBounds.height);
                    }
                });
//                AffineTransform xf = AffineTransform.getScaleInstance(1, -1);
//                if (el.item().is(Text.class) && el.item().as(Text.class).transform() == null) {
//                    // This works only for text, which internally diddles
//                    // with transforms
//                    el.applyTransform(xf);
//                    el.changed();
//                    el.applyTransform(AffineTransform.getTranslateInstance(0, (-origBounds.getY() * 2) - origBounds.getHeight()));
//                    el.changed();
//                } else {
//                    // XXX if a PathIteratorWrapper, maybe center
//                    // on the non-virtual points?
//                    el.applyTransform(xf);
//                    el.changed();
//                    el.translate(0, (origBounds.y * 2) + origBounds.height);
//
//                }
                ctrl.shapeGeometryChanged(el);
            }).hook(() -> {
                ctrl.shapeGeometryChanged(el);
            });
        });

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
                    int oldHash = w.hashCode();
                    assert w != null;
                    shapes.edit(Bundle.opConvertToCurves(), shape, (abortable) -> {
                        boolean result = w.toCubicSplines();
                        // if nothing changed, don't add an undoable edit
                        if (!result || w.hashCode() == oldHash) {
                            abortable.abort();
                        } else {
                            ctrl.shapeGeometryChanged(shape);
                            ctrl.pointCountMayBeChanged(shape);
                        }
                    }).hook(() -> {
                        ctrl.shapeGeometryChanged(shape);
                        ctrl.pointCountMayBeChanged(shape);
                    });
                });

        actions.action(Bundle.actionConvertToPathText())
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .finish((shape, shapes) -> {
                    DialogBuilder.forName("convertToPathText")
                            .okCancel()
                            .showMultiLineTextLineDialog("This text will wrap around the shape", 2, 512, txt -> {
                                Customizer<Font> cus = Customizers.getCustomizer(Font.class, "font");
                                DialogBuilder.<Font>forName("font").closeOnly()
                                        .forContent(cus.getComponent())
                                        .openDialog(comp -> {
                                            return cus.get();
                                        }, fnt -> {
                                            shapes.edit(Bundle.opConvertToPathText(), shape, () -> {
                                                PathText pt = new PathText(shape.item(), new StringWrapper(txt, 0, 0), FontWrapper.create(fnt));
                                                shape.setShape(pt);
                                                shape.setPaintingStyle(PaintingStyle.FILL);
                                                ctrl.shapeGeometryChanged(shape);
                                            }).hook(() -> {
                                                ctrl.shapeGeometryChanged(shape);
                                            });

                                        });
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
                        shapes.edit(Bundle.opPointType(k), element, (abortable) -> {
                            boolean result = k.changeType(element.item().as(PathIteratorWrapper.class), cp.index());
                            if (result) {
                                ctrl.shapeGeometryChanged(element);
                                ctrl.pointCountMayBeChanged(element);
                            } else {
                                abortable.abort();
                            }
                        });
                    });
                }
            }
        });

        actions.submenu(Bundle.submenuControlPoints(), a -> {
            for (ControlPointKind k : new ControlPointKind[]{ControlPointKind.LINE_TO_DESTINATION, ControlPointKind.CUBIC_CURVE_DESTINATION, ControlPointKind.QUADRATIC_CURVE_DESTINATION}) {
                a.action(k.toString())
                        .sensitiveTo(ShapesCollection.class).sensingPresence()
                        .sensitiveTo(ShapeElement.class)
                        .testingOne(element -> {
                            return element.isPaths()
                                    && element.item().is(PathIteratorWrapper.class);
                        }).finish((coll, element) -> {
                    coll.edit(Bundle.opPointType(element.getName()), element, abortable -> {
                        if (k.addDefaultPoint(element.item().as(PathIteratorWrapper.class), lastPopupLocation)) {
                            ctrl.pointCountMayBeChanged(element);
                            ctrl.shapeGeometryChanged(element);
                        } else {
                            abortable.abort();
                        }
                    });
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
                                if (!el.canApplyTransform(testScale)) {
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
            a.action(Bundle.actionArbitraryScale()).sensitiveTo(ShapesCollection.class)
                    .sensingPresence().sensitiveTo(ShapeElement.class)
                    .testingEach(item -> {
                        return item.canApplyTransform(testScale);
                    }).finishMultiple((colls, shapes) -> {
                Customizer<AffineTransform> ac = Customizers.getCustomizer(AffineTransform.class, "scaleShape");
                AffineTransform xf = DialogBuilder.forName("scaleShape").modal()
                        .okCancel().ownedBy(WindowManager.getDefault().getMainWindow())
                        .forContent(ac.getComponent())
                        .openDialog(comp -> {
                            return ac.get();
                        });
                if (xf == null || xf.isIdentity()) {
                    return;
                }
                Rectangle2D.Double oldCenter = new Rectangle2D.Double();
                Rectangle2D.Double newCenter = new Rectangle2D.Double();
                if (shapes.size() == 1) {
                    // XXX need to capture the original bounds and re-center
                    ShapeElement el = shapes.iterator().next();
                    colls.iterator().next().edit(Bundle.opArbitraryScale(el.getName()), el, abortable -> {
                        oldCenter.width = oldCenter.height = newCenter.width = newCenter.height = 0;
                        el.addToBounds(oldCenter);
                        el.applyTransform(xf);
                        el.addToBounds(newCenter);
                        double diffX = newCenter.getCenterX() - oldCenter.getCenterX();
                        double diffY = newCenter.getCenterY() - oldCenter.getCenterY();
                        el.translate(-diffX, -diffY);
                        ctrl.shapeGeometryChanged(el);
                    });
                } else {
                    colls.iterator().next().geometryEdit(Bundle.opArbitraryScaleMultiple(), () -> {
                        for (ShapeElement el : shapes) {
                            oldCenter.width = oldCenter.height = newCenter.width = newCenter.height = 0;
                            el.addToBounds(oldCenter);
                            el.applyTransform(xf);
                            el.addToBounds(newCenter);
                            double diffX = newCenter.getCenterX() - oldCenter.getCenterX();
                            double diffY = newCenter.getCenterY() - oldCenter.getCenterY();
                            el.translate(-diffX, -diffY);
                            ctrl.shapeGeometryChanged(el);
                        }
                    });
                }
            });
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
                                AffineTransform realXform = AffineTransform.getRotateInstance(rad, aggregateBounds.getCenterX(), aggregateBounds.getCenterY());
                                shapes.edit(Bundle.actionRotate(only.getName(), degrees), only, () -> {
                                    only.applyTransform(realXform);
                                    ctrl.shapeGeometryChanged(only);
                                });
                            } else {
                                // find the aggregate center point
                                for (ShapeElement el : allShapes) {
                                    el.addToBounds(aggregateBounds);
                                }
                                AffineTransform realXform = AffineTransform.getRotateInstance(rad, aggregateBounds.getCenterX(), aggregateBounds.getCenterY());
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
                            EqPointDouble pt = currentPoint2D.get();
                            List<? extends ShapeElement> atPoint = shapes.shapesAtPoint(pt.x, pt.y);
                            return atPoint.size() > 1;
                        }).finish((element, shapes) -> {
                    EqPointDouble pt = currentPoint2D.get();
                    List<? extends ShapeElement> atPoint = shapes.shapesAtPoint(pt.x, pt.y);
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

        actions.action(Bundle.actionDuplicate())
                .withKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0))
                .sensitiveTo(ShapesCollection.class).sensingPresence()
                .sensitiveTo(ShapeElement.class).sensingPresence()
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

        actions.action("Debug Shape Info")
                .separatorBefore()
                .sensitiveTo(ShapeElement.class).sensingPresence()
                .finish(this::shapeInfo);
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

    public JPopupMenu popup(Lookup lkp) {
        JPopupMenu menu = new JPopupMenu();
        actions.applyToPopup(menu, lkp);
        return menu;
    }

    public JPopupMenu menu(Widget widget, Point localLocation) {
        if (localLocation != null) {
            lastPopupLocation.setLocation(widget.convertLocalToScene(localLocation));
        }
        JPopupMenu menu = new JPopupMenu();
        currentPoint.set(localLocation);
        currentPoint2D.set(ViewL.lastPoint2D(widget));
        try {
            actions.applyToPopup(menu, new ProxyLookup(widget.getLookup(), Utilities.actionsGlobalContext()));
        } finally {
            currentPoint.remove();
        }
        return menu;
    }

    public void shapeInfo(ShapeElement el) {
        Shape shape = el.shape();
        Shaped vect = el.item();
        StringBuilder sb = new StringBuilder(ShapeNames.nameOf(vect) + "\n" + ShapeNames.infoString(vect));
        Rectangle2D bds = shape.getBounds2D();
        sb.append("\nBounds: ").append(bds.getX()).append(", ").append(bds.getY()).append(", ").append(bds.getWidth()).append(" x ").append(bds.getWidth()).append('\n');
        double[] data = new double[6];
        PathIterator pi = shape.getPathIterator(null);
        int firstType = pi.currentSegment(data);
        pi.next();
        double[] firstPoint = new double[]{data[0], data[1]};
        int secondType = pi.currentSegment(data);
        double[] secondPoint = new double[]{data[0], data[1]};

        Circle circ = new Circle(firstPoint[0], firstPoint[1],
                Point2D.distance(firstPoint[0], firstPoint[1], secondPoint[0],
                        secondPoint[1]));

        sb.append('\n').append("Point 1 to 2 dist / radius: " + circ.radius()).append('\n');
        sb.append('\n').append("Points:\n");
        int ix = 2;
        while (!pi.isDone()) {
            sb.append(ix).append(". ");
            int type = pi.currentSegment(data);
            int pointCount;
            switch (type) {
                case SEG_CUBICTO:
                    sb.append("Cubic ");
                    pointCount = 3;
                    break;
                case SEG_CLOSE:
                    sb.append("Close ");
                    pointCount = 0;
                    break;
                case SEG_QUADTO:
                    sb.append("Quad ");
                    pointCount = 2;
                    break;
                case SEG_MOVETO:
                    sb.append("Move ");
                    pointCount = 1;
                    break;
                case SEG_LINETO:
                    sb.append("Line ");
                    pointCount = 1;
                    break;
                default:
                    throw new AssertionError("Bogus point type " + type);
            }
            EqPointDouble lastMaster = new EqPointDouble(secondPoint[0], secondPoint[1]);
            if (pointCount > 0) {
                List<EqPointDouble> controlPoints = new ArrayList<>();
                EqPointDouble master = new EqPointDouble();
                for (int i = 0; i < pointCount; i++) {
                    int offset = i * 2;
                    if (i == pointCount - 1) {
                        master.setLocation(data[offset], data[offset + 1]);
                    } else {
                        controlPoints.add(new EqPointDouble(data[offset], data[offset + 1]));
                    }
                }
                double ang = circ.angleOf(master.getX(), master.getY());
                double precAng = circ.angleOf(lastMaster.getX(), lastMaster.getY());

                double distToFirst = Point2D.distance(firstPoint[0], firstPoint[1], master.x, master.y);
                sb.append(master.x).append(", ").append(master.y)
                        .append("\n angle ").append(ang).append('\u00b0')
                        .append("\n dist to first point ").append(distToFirst);
                sb.append("\n dist to preceding: ").append(Point2D.distance(lastMaster.x, lastMaster.y, master.x, master.y)).append('\n');
                sb.append("\n angle diff from preceding: ").append(ang - precAng);

                if (!controlPoints.isEmpty()) {
                    for (int i = 0; i < controlPoints.size(); i++) {
                        EqPointDouble cp = controlPoints.get(i);
                        double distToMaster = Point2D.distance(master.getX(), master.getY(), cp.x, cp.y);
                        Circle temp = new Circle(master.x, master.y, distToMaster);
                        EqLine tangent = circ.tangent(ang, distToMaster);
                        sb.append("\n\tCP ").append(i + 1).append(". ")
                                .append(cp.x).append(", ").append(cp.y).append('\n');
                        sb.append("\t\tdist to master: ").append(distToMaster)
                                .append(" radius percentage ");
                        double percentageOfRadius = 100 * (distToMaster / circ.radius());
                        sb.append(percentageOfRadius).append('\n');

                        double distToTangent1 = Point2D.distance(tangent.x1, tangent.y1, cp.x, cp.y);
                        double distToTangent2 = Point2D.distance(tangent.x2, tangent.y2, cp.x, cp.y);

                        int bestTangentPointIndex = distToTangent1 < distToTangent2 ? 0 : 1;
                        EqPointDouble bestTangentPoint = new EqPointDouble(bestTangentPointIndex == 0 ? tangent.x1 : tangent.x2,
                                bestTangentPointIndex == 0 ? tangent.y1 : tangent.y2);

                        sb.append("\t\tNearest tangent point ").append(bestTangentPointIndex)
                                .append(" at ").append(bestTangentPoint.x).append(bestTangentPoint.y)
                                .append(" dist ").append(bestTangentPointIndex == 0
                                ? distToTangent1 : distToTangent2).append('\n');
                        sb.append("\t\tTangent angle ").append(temp.angleOf(bestTangentPoint.x, bestTangentPoint.y)).append('\n');
                        sb.append("\t\tActual angle ").append(temp.angleOf(cp.x, cp.y));
                        sb.append("\t\tAngle difference ").append(temp.angleOf(cp.x, cp.y) - temp.angleOf(bestTangentPoint.x, bestTangentPoint.y));
                        sb.append('\n');
                    }
                }
                lastMaster = master;
            }
            sb.append('\n');
            pi.next();
        }
        DialogBuilder.forName("debug").nonModal()
                .showMultiLineTextLineDialog(sb.toString(), ignored -> true, ign -> {
                });

    }
}
