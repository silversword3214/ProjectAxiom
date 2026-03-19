package silversword.axiom.client.modules.render;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import java.util.*;

public final class LavaESP extends AxiomMod implements KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final SettingSlider renderDistance;
    private final SettingSlider minPoolSize;
    private final SettingSlider maxPoolSize;
    private final SettingMode boxMode;
    private final SettingColor lavaColor;
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public LavaESP() {
        super("LavaESP", "Highlights lava pools for netherite mining", ModuleCategory.RENDER);

        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32}, 16);
        minPoolSize = new SettingSlider("Min Pool Size", new double[]{1, 2, 3, 5, 10, 15, 20}, 1);
        maxPoolSize = new SettingSlider("Max Pool Size", new double[]{1, 2, 3, 5, 10, 15, 20, 30, 50, 100, 200, 500}, 50);
        boxMode = new SettingMode("Box Mode", new String[]{"Lines", "Sides", "Both"}, "Sides");
        lavaColor = new SettingColor("Lava Color", new Color(255, 100, 0, 180));

        addHiddenSetting(lavaColor.getSetting());
        addSetting(renderDistance);
        addSetting(minPoolSize);
        addSetting(maxPoolSize);
        addSetting(boxMode);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3d playerPos = mc.player.getEntityPos();

        int chunkRadius = (int) Math.ceil(renderDistance.getValue() / 16.0) + 1;
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;
        int bottomY = mc.world.getBottomY();
        int topY = mc.world.getTopYInclusive();

        Set<BlockPos> lavaBlocks = new HashSet<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        // Kerätään kaikki laavalohkot renderDistance-säteellä
        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                Chunk chunk = mc.world.getChunk(cx, cz);
                if (!(chunk instanceof WorldChunk worldChunk) || worldChunk.isEmpty()) continue;

                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        for (int y = bottomY; y < topY; y++) {
                            pos.set(cx * 16 + dx, y, cz * 16 + dz);
                            if (pos.getSquaredDistance(playerPos) > maxDistSq) continue;

                            Block block = worldChunk.getBlockState(pos).getBlock();
                            if (isLava(block)) {
                                lavaBlocks.add(pos.toImmutable());
                            }
                        }
                    }
                }
            }
        }

        // Ryhmitellään laavalohkot altaiksi BFS:llä
        Set<BlockPos> visited = new HashSet<>();
        int minSize = (int) minPoolSize.getValue();
        int maxSize = (int) maxPoolSize.getValue();

        for (BlockPos start : lavaBlocks) {
            if (visited.contains(start)) continue;

            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            Set<BlockPos> pool = new HashSet<>();
            boolean tooBig = false;

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                if (visited.contains(current)) continue;
                visited.add(current);
                pool.add(current);

                // Jos altaan koko ylittää maksimin, keskeytetään (ei piirretä)
                if (pool.size() > maxSize) {
                    tooBig = true;
                    break;
                }

                // Tarkista naapurit (6 kardinaalisuuntaa)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;

                            BlockPos neighbor = current.add(dx, dy, dz);
                            if (lavaBlocks.contains(neighbor) && !visited.contains(neighbor)) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }

            if (!tooBig && pool.size() >= minSize) {
                renderPool(event, pool);
            }
        }
    }

    private void renderPool(Render3DEvent event, Set<BlockPos> pool) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos p : pool) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }

        Color color = lavaColor;
        Color sideColor = new Color(color.r, color.g, color.b, 30);
        Color lineColor = color;

        ShapeMode mode = switch (boxMode.getMode()) {
            case "Sides" -> ShapeMode.Sides;
            case "Both" -> ShapeMode.Both;
            default -> ShapeMode.Lines;
        };

        double expand = 0.1;
        event.render.drawBox(
                minX - expand, minY - expand, minZ - expand,
                maxX + 1 + expand, maxY + 1 + expand, maxZ + 1 + expand,
                sideColor, lineColor, mode, 0
        );
    }

    private boolean isLava(Block block) {
        return block == Blocks.LAVA;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }
}