package silversword.axiom.client.modules.render.blockesp;

import net.minecraft.util.math.BlockPos;
import silversword.axiom.client.render.rendersystem.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class Group {
    public final net.minecraft.block.Block block;
    private final List<Block> blocks = new ArrayList<>();

    public Group(net.minecraft.block.Block block) {
        this.block = block;
    }

    public void add(Block block) {
        blocks.add(block);
        block.group = this;
    }

    public void remove(Block block) {
        blocks.remove(block);
        block.group = null;
    }

    public int size() {
        return blocks.size();
    }

    public BlockPos getAveragePos() {
        double x = 0, y = 0, z = 0;
        for (Block b : blocks) {
            x += b.pos.getX();
            y += b.pos.getY();
            z += b.pos.getZ();
        }
        int size = blocks.size();
        return new BlockPos((int)(x / size), (int)(y / size), (int)(z / size));
    }

    public void renderTracer(Renderer3D renderer, Color color) {
        if (blocks.isEmpty()) return;
        BlockPos avg = getAveragePos();
        renderer.drawLine(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                avg.getX() + 0.5, avg.getY() + 0.5, avg.getZ() + 0.5, color);
    }
}