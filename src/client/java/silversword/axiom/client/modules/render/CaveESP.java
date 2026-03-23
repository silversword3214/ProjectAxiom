package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;

import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

import java.util.HashSet;
import java.util.Set;

public final class CaveESP extends AxiomMod implements KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    private final SettingSlider renderDistance;
    private final SettingSlider maxYLevel;
    private final SettingColor caveColor;

    private final Set<BlockPos> caveBlocks = new HashSet<>();
    private int scanTimer = 0;
    private static final int SCAN_INTERVAL = 40; // 2 sekuntia

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public CaveESP() {
        super("CaveESP", "Highlights caves underground", ModuleCategory.RENDER);

        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256}, 64);
        maxYLevel = new SettingSlider("Max Y Level", new double[]{16, 32, 40, 48, 56, 64}, 40);
        caveColor = new SettingColor("Cave Color", new Color(255, 255, 255, 180));

        addHiddenSetting(caveColor.getSetting());
        addSetting(renderDistance);
        addSetting(maxYLevel);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        scanTimer++;
        if (scanTimer >= SCAN_INTERVAL) {
            scanTimer = 0;
            scanCaves();
        }
    }

    private void scanCaves() {
        caveBlocks.clear();
        double maxDist = renderDistance.getValue();
        int maxY = (int) maxYLevel.getValue();
        int bottomY = mc.level.getMinY();

        int chunkRadius = (int) Math.ceil(maxDist / 16.0) + 1;
        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                ChunkAccess chunk = mc.level.getChunk(cx, cz);
                if (!(chunk instanceof LevelChunk) || ((LevelChunk) chunk).isEmpty()) continue;

                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        for (int y = bottomY; y <= maxY; y++) {
                            pos.set(cx * 16 + dx, y, cz * 16 + dz);
                            if (!mc.level.getBlockState(pos).isAir()) continue;

                            // Tarkistetaan, onko tämä ilmalohko luolan osa (koskettaa kiinteää lohkoa)
                            if (isAdjacentToSolid(pos)) {
                                caveBlocks.add(pos.immutable());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isAdjacentToSolid(BlockPos pos) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    neighbor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!mc.level.getBlockState(neighbor).isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null || caveBlocks.isEmpty()) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 playerPos = mc.player.position();

        Color color = caveColor;

        for (BlockPos pos : caveBlocks) {
            if (pos.distToCenterSqr(playerPos) > maxDistSq) continue;

            double half = 0.5;
            event.getRenderer().drawBox(
                    pos.getX() + 0.5 - half, pos.getY(), pos.getZ() + 0.5 - half,
                    pos.getX() + 0.5 + half, pos.getY() + 1.0, pos.getZ() + 0.5 + half,
                    color, color, ShapeModeEnum.LINES, 0
            );
        }
    }

    @Override
    protected void onDisable() {
        caveBlocks.clear();
        super.onDisable();
    }
}