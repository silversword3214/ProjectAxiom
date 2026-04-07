package silversword.axiom.client.managers;

import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockGhostManager {
    private static final BlockGhostManager INSTANCE = new BlockGhostManager();
    public static BlockGhostManager getInstance() { return INSTANCE; }

    private final Map<BlockPos, GhostBlock> ghostBlocks = new ConcurrentHashMap<>();

    public void addGhost(BlockPos pos) {
        ghostBlocks.put(pos, new GhostBlock());
    }

    public void removeGhost(BlockPos pos) {
        ghostBlocks.remove(pos);
    }

    public void clearAll() {
        ghostBlocks.clear();
    }

    public Map<BlockPos, GhostBlock> getGhosts() {
        return ghostBlocks;
    }

    public boolean isGhost(BlockPos pos) {
        return ghostBlocks.containsKey(pos);
    }

    public static class GhostBlock {
        public GhostBlock() {}
    }
}