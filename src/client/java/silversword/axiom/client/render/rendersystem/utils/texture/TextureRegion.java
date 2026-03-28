package silversword.axiom.client.render.rendersystem.utils.texture;

import net.minecraft.resources.Identifier;

public class TextureRegion {

    public Identifier atlasId;

    public double x1, y1;
    public double x2, y2;

    public double diagonal;

    public TextureRegion(double width, double height) {
        diagonal = Math.sqrt(width * width + height * height);
    }
}