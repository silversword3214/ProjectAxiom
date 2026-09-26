package silversword.axiom.client.rendersystem.axiomrenderer.engine.postprocess.shaderesp;

import net.minecraft.client.renderer.SubmitNodeStorage;

public class OutlineRenderCommandQueue extends SubmitNodeStorage {
    private int color = 0xFFFFFFFF;
    public void setColor(int argb) { this.color = argb; }
    public int  getColor()         { return this.color; }
}