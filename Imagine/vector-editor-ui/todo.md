
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
 * Raster layers should know if they contain a single-color, never-used image (or should not contain one until it is needed),
and not try to blit it during painting.  The biggest performance hit when dragging control points on an vector layer above
an empty image layer is blitting a rectangle of pure white pixel-by-pixel
 * Need a "resize layer to contents size" action to go with that
 * Fix activation / focus, which is haywire right now
 * General model for drawing tools which can snap
 * Popup slider ui should also work on a single click, for tablet mode

Vector Bugs:
 * Rectangle tool mouse release doesn't respect shift-down for square
 * Preferred height of colors and fractions editor is too tall for the settings window (or do better at minimum size computation and make the layout respect it)
 * Resizing vector layers never resizes contents
 * Mouse release while dragging when mouse is out of picture bounds results drag state not getting reset, and
   exception thrown on next attempted drag
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
 * BACK: ShapeEntry's that have a PathIteratorWrapper do not give correct bounds until first call to changed()) 
 * Need multiselect drag / move / etc.
 * New Editor: When scene bounds are < 0, drag offsets are wrong - probably fix in ViewL - should be adding -scene.x/y (or we are doing that twice?))
 * Handle background setting with new interface in vectors
 * Ability to zero texture position on shape (translate the paint, and paint a retranslated shape)
 
 Palettes:
 * Impl should use same file extension as save action from palette
 * Drag palette item out of designer leaves draggable image widget as a turd 

UI Buglets:
 * Colors for snap decorations and similar should probably pay attention to editor background
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
 * Derive control point color from shape color?
 * Need a group type - use aggregate?
 
 
