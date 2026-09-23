package silversword.axiom.client.render.font;

import net.minecraft.client.gui.Font;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class VanillaTextRenderer implements TextRenderer {
    public static final VanillaTextRenderer INSTANCE = new VanillaTextRenderer();

    private double scale = 2.0;
    private boolean building;
    private double alpha = 1.0;

    private VanillaTextRenderer() {}

    @Override
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;
        if (length != text.length()) text = text.substring(0, length);
        return (mc.font.width(text) + (shadow ? 1 : 0)) * scale;
    }

    @Override
    public double getAscent() {
        return mc.font.lineHeight * scale;
    }

    @Override
    public double getHeight(boolean shadow) {
        return (mc.font.lineHeight + (shadow ? 1 : 0)) * scale;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new IllegalStateException("VanillaTextRenderer.begin() called twice");
        this.scale = scale * 2.0;
        this.building = true;
    }

    @Override
    public double getCharAdvance(char c) {
        return (mc.font.width(String.valueOf(c))) * scale;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        double width = getWidth(text, shadow);
        if (!wasBuilding) end();
        return x + width;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new IllegalStateException("VanillaTextRenderer.end() called without begin()");
        scale = 2.0;
        building = false;
    }
}
