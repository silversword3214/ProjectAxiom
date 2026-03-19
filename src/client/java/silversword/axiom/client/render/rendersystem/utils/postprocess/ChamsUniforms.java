package silversword.axiom.client.render.rendersystem.utils.postprocess;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gl.DynamicUniformStorage;

import java.nio.ByteBuffer;

public class ChamsUniforms {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
            .putInt()   // renderMode
            .putInt()   // color (ARGB)
            .putInt()   // throughWalls
            .putInt()   // padding
            .get();

    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("Obsidian - Chams UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        STORAGE.clear();
    }

    public static GpuBufferSlice write(int renderMode, int color, int throughWalls) {
        return STORAGE.write(new Data(renderMode, color, throughWalls, 0));
    }

    private record Data(int renderMode, int color, int throughWalls, int padding) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putInt(renderMode)
                    .putInt(color)
                    .putInt(throughWalls)
                    .putInt(padding);
        }
    }
}