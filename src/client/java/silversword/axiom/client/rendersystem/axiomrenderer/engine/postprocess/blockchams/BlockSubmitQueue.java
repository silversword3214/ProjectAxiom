package silversword.axiom.client.rendersystem.axiomrenderer.engine.postprocess.blockchams;

import net.minecraft.client.renderer.SubmitNodeStorage;

public class BlockSubmitQueue extends SubmitNodeStorage {
    private int color = 0xFFFFFFFF;
    public void setColor(int argb) { this.color = argb; }
    public int  getColor()         { return this.color; }
}