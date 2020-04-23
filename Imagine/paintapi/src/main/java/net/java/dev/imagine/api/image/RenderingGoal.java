package net.java.dev.imagine.api.image;

/**
 *
 * @author Tim Boudreau
 */
public enum RenderingGoal {

    ACTIVE_EDITING,
    EDITING,
    THUMBNAIL,
    PRODUCTION // ,TEXTURES_DISABLED
    ;

    public boolean isEditing() {
        return this == EDITING || this == ACTIVE_EDITING;
    }

    public boolean isProduction() {
        return this == PRODUCTION;
    }
}
