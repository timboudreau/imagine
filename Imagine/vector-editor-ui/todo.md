
Features:
 * Palette TopComponents
 * SVG Export
 * SVG Import

IO Bugs:
 * Should pad some file sections for future proofing
 * Failing tests of VectorIO with Shapes instance equality

Vector Bugs:
 * "Previous drag operation not ended" seems to happen mostly when snap is on - maybe returning false from something if point hasn't changed?
 * Fix io saving of Text now that transform handled differently
 * Triangles and similar should have edge handle control points
 * Some point kind changes are not working that should (curve -> straight)
 * Fix transform on Text - apply transform when painting, not to font - it should just hold it
 * Text should capture the text line baselines
 * ImageWrapper should take a Text
 * Snap all points to grid action
 * Edit text menu item
 * Set name text menu item
 * Where is rectanglar selection?
 * Dragging point 0 of a rectangle does strange sizing things
 * ShapeEntry's that have a PathIteratorWrapper does not give correct bounds until first call to changed()) 
 * Shapes.edit and friends should take a BooleanConsumer to abort generating an undo action for an action that failed
 * Need multiselect drag / move / etc.
 * New Editor: When scene bounds are < 0, drag offsets are wrong - probably fix in ViewL - should be adding -scene.x/y (or we are doing that twice?))
 * Shape grid snap leaves control points offset - some kind of rounding or update ordering thing?
 * Removing an image layer with an empty vector layer above it results an a 0-size editor
 * Handle background setting with new interface in vectors
 * Test Undo Support
 * Rotations on Text objects screw up the position completely - maybe don't transform the font, just the coordinates
 * Ability to zero texture position on shape (translate the paint, and paint a retranslated shape)
 * Could have text that follows a path...
 
 Palettes:
 * Layout jumps when preferred size of adjacent component changes, at least in demo
 * Real implementation does not do the right thing with loading - does not show all tiles
 * Impl should use same file extension as save action from palette
 * Drag palette item out of designer leaves draggable image widget as a turd 

UI Buglets:
 * File name not associated with TC and name set when native loading
 * Circle tool in weird order with other shape tools
 * File hiding in file open dialog
 * Order of image filters - start with images?
 * When reordering layers, current active layer is not preserved 
 * Default color for solid color fill is clear and customizer not shown
 * XOR painting to indicate selection looks lousy
 * Annotation is registering garbage breaking TC positioning for tools settings & layers
 

Questionable ideas:
 * Derive control point color from shape color?
 * Need a group type - use aggregate?
 * Maybe use a single widget for all control points?  Performance gets bad when there are a lot.  Would be paintful to implement, particularly revalidation and dragging, but may be worth it.
 
 
 For repaint catching:
org.netbeans.api.visual.widget.SceneComponent