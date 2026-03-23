package silversword.axiom.client.event.world;

import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkDataEvent {
    private static final ChunkDataEvent INSTANCE = new ChunkDataEvent();

    public LevelChunk chunk;

    public static ChunkDataEvent get(LevelChunk chunk) {
        INSTANCE.chunk = chunk;
        return INSTANCE;
    }
}