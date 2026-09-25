package silversword.axiom.client.render.rendersystem.axiomrenderer.blockchams;

import net.minecraft.client.renderer.SubmitNodeStorage;

/** Submit-jono block entityjen renderöintiin mask-RT:hen. */
public class BlockSubmitQueue extends SubmitNodeStorage {
    private int color = 0xFFFFFFFF;
    public void setColor(int argb) { this.color = argb; }
    public int  getColor()         { return this.color; }
}