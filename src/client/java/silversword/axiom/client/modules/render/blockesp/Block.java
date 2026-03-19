package silversword.axiom.client.modules.render.blockesp;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


import silversword.axiom.client.render.rendersystem.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.world.Dir;

public class Block {
    public final BlockPos pos;
    public final net.minecraft.block.Block block;
    public int neighbours; // bitmask using Dir constants

    public Group group; // optional, for tracers

    public Block(BlockPos pos, net.minecraft.block.Block block) {
        this.pos = pos;
        this.block = block;
        this.neighbours = 0;
    }

    public void updateNeighbours(Chunk chunk) {
        neighbours = 0;
        if (isNeighbour(chunk, Direction.WEST)) neighbours |= Dir.WEST;
        if (isNeighbour(chunk, Direction.EAST)) neighbours |= Dir.EAST;
        if (isNeighbour(chunk, Direction.NORTH)) neighbours |= Dir.NORTH;
        if (isNeighbour(chunk, Direction.SOUTH)) neighbours |= Dir.SOUTH;
        if (isNeighbour(chunk, Direction.UP)) neighbours |= Dir.UP;
        if (isNeighbour(chunk, Direction.DOWN)) neighbours |= Dir.DOWN;
    }

    private boolean isNeighbour(Chunk chunk, Direction dir) {
        BlockPos neighbourPos = pos.offset(dir);
        Block neighbour = chunk.getBlock(neighbourPos);
        return neighbour != null && neighbour.block == this.block;
    }

    public void render(Renderer3D renderer, BlockData data) {
        Color line = data.lineColor.getCurrentColor();
        Color side = data.sideColor.getCurrentColor();


        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;

        renderer.drawBox(x1, y1, z1, x2, y2, z2, side, line, data.shapeMode, neighbours);
    }
}