
Features:
 * SVG Import

IO Bugs:
 * Should pad some file sections for future proofing
 * Failing tests of VectorIO with Shapes instance equality
 * Picture should save current zoom level
 * Need recent files
 * File still not associated correctly
 * Current zoom level should be saved with picture

Misc:
 * Non-ugly defaults for linear and radial gradient
 * Create vector icons for shapes palette and paints palette
 * Save As chooser should hide hidden folders and files
 * Pick layer type in new image dlg
 * New image dialog does not retain sizes
 * Need a "resize layer to contents size" action to go with that
 * Fix activation / focus, which is haywire right now
 * General model for drawing tools which can snap
 * Popup slider ui should also work on a single click, for tablet mode
 * Turn off fine level logging in SaveSupport and friends

SVG Import Bugs:
 * Should be able to split groups into separate layers
 * Colors lose their alpha on import - referred to as stop-opacity in the svg
 * Need an AlphaComposite wrapper, or detect when an entire layer is being painted with the same opacity

Vector Bugs:
 * A transform set on the graphics context in VectorWrapperGraphics is not applied - should apply it with applyTransform() to items as they arrive
 * GradientPaintWrapper and LinearPaintWrapper should implement Transformable
 * On draw, outline-only shapes are getting filled with the outline color and tracked as Fill instead
 * Tool action forwarding appears to be devouring keyboard shortcuts
 * Double-click on text or path text should open text editor
 * Path text loses last character of string - there but not painted
 * Shape Design tool widget not removed when it is detached
 * Z-Order is backwards for OneShapeWidgets
 * Polygon2D delete point can leave coordinates array with an odd length - test boundary conditions
 * PathTool drawing extra line to end point - caused by fix for no line when only one point present and moving mouse
 * PathTool adds a duplicate point instead of just closing the shape when you close by clicking the start point
 * Fix tab key handler - still expects a widget per control point
 * Ensure when tool is a layer providing one, that we do not set focus to the layer widget because the tool received an event
 * PathTool should be able to close a shape with a QUADTO or CUBICTO, not insist on a LINETO to close
 * Self-snapping is happening - probably testing instance equality when eliminating points to collect, or some similar equality test problem
 * Rectangle tool mouse release doesn't respect shift-down for square
 * Preferred height of colors and fractions editor is too tall for the settings window (or do better at minimum size computation and make the layout respect it)
 * Resizing vector layers never resizes contents
 * Rectangle and others need to revalidate all control points after drag
 * Rotation should not work on a rectangle
 * Should not apply extent/angle snap to shapes that can only have right angles such as rectangle and round rects
 * Oval needs to renormalize points after applying a transform, particularly rotation which can give negative dimensions
 * Ability to create a new picture starting with a Vector layer
 * Put back keyboard actions for rewrite of points widget
 * Stroke is not scaled to zoom when doing regular drawing, but is when editing
 * Grid snap of 2 does not work at high zoom (5000%)
 * Shapes.edit and friends should take a BooleanConsumer to abort generating an undo action for an action that failed
 * Triangles and similar should have edge handle control points
 * Where is rectanglar selection?
 * Dragging point 0 of a rectangle does strange sizing things
 * Need multiselect drag / move / etc.
 * New Editor: When scene bounds are < 0, drag offsets are wrong - probably fix in ViewL - should be adding -scene.x/y (or we are doing that twice?))
 * Handle background setting with new interface in vectors
    * Need to save background with picture, if it is not in a layer
 * Ability to zero texture position on shape (translate the paint, and paint a retranslated shape)
 
 Palettes:
 * Impl should use same file extension as save action from palette
 * Drag palette item out of designer leaves draggable image widget as a turd 

UI Buglets:
 * Should be possible to use a commplex fill for a stroke, not just a fill
 * PaintParticipant drawing needs to be scaled to zoom
 * File name not associated with TC and name set when native loading
 * Circle tool in weird order with other shape tools
 * File hiding in file open dialog
 * Remember last choice of filter in open dialog
 * Order of image filters - start with images?
 * Default color for solid color fill is clear and customizer not shown
 * Annotation is registering garbage breaking TC positioning for tools settings & layers 

Questionable ideas:
 * Need a group type - use aggregate?

