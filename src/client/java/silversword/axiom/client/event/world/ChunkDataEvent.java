package silversword.axiom.client.event.world;

import net.minecraft.world.chunk.WorldChunk;

public class ChunkDataEvent {
    private static final ChunkDataEvent INSTANCE = new ChunkDataEvent();

    public WorldChunk chunk;

    public static ChunkDataEvent get(WorldChunk chunk) {
        INSTANCE.chunk = chunk;
        return INSTANCE;
    }
}