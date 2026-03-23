package silversword.axiom.client.modules.render.blockesp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import silversword.axiom.client.modules.render.SearchBlocks;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Chunk {
    public final ChunkPos pos;
    private final Map<BlockPos, Block> blocks = new HashMap<>();

    public Chunk(ChunkPos pos) {
        this.pos = pos;
    }

    public Block addBlock(BlockPos pos, net.minecraft.world.level.block.Block block) {
        Block espBlock = new Block(pos, block);
        blocks.put(pos, espBlock);
        return espBlock;
    }

    public Block getBlock(BlockPos pos) {
        return blocks.get(pos);
    }

    public void removeBlock(BlockPos pos) {
        blocks.remove(pos);
    }

    public Collection<Block> getBlocks() {
        return blocks.values();
    }

    public void updateNeighbours() {
        for (Block block : blocks.values()) {
            block.updateNeighbours(this);
        }
    }

    public int size() {
        return blocks.size();
    }

    public void render(Renderer3D renderer, SearchBlocks module) {
        for (Block block : blocks.values()) {
            BlockData data = module.getBlockData(block.block);
            block.render(renderer, data);
        }
    }

    public boolean shouldBeDeleted(int viewDistance, ChunkPos playerChunk) {
        return Math.abs(pos.x - playerChunk.x) > viewDistance || Math.abs(pos.z - playerChunk.z) > viewDistance;
    }
}