package silversword.axiom.client.render.rendersystem.utils.misc;

public enum ShapeModeEnum {
    LINES,
    SIDES,
    BOTH;

    public boolean lines() {
        return this == LINES || this == BOTH;
    }

    public boolean sides() {
        return this == SIDES || this == BOTH;
    }
}