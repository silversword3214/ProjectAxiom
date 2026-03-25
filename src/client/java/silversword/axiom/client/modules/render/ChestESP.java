package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.StorageType;

import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

public final class ChestESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // Värit (package-private customizerille)
    final SettingColor chestColor;
    final SettingColor trappedChestColor;
    final SettingColor barrelColor;
    final SettingColor enderChestColor;
    final SettingColor shulkerColor;
    final SettingColor furnaceColor;

    // Suodatusasetukset
    private final SettingBoolean drawChests;
    private final SettingBoolean drawTrapped;
    private final SettingBoolean drawBarrel;
    private final SettingBoolean drawEnder;
    private final SettingBoolean drawShulker;
    private final SettingBoolean drawFurnace;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingSlider renderDistance;
    private final SettingMode boxMode; // Outline, Filled, Both

    public ChestESP() {
        super("ChestESP", "Highlights storage blocks", ModuleCategory.RENDER);

        // Oletusvärit
        chestColor         = new SettingColor("Chest Color",         new Color(255, 255, 0, 180));   // keltainen
        trappedChestColor  = new SettingColor("Trapped Chest Color", new Color(255, 80, 80, 180));   // punertava
        barrelColor        = new SettingColor("Barrel Color",        new Color(150, 100, 50, 180));  // ruskea
        enderChestColor    = new SettingColor("Ender Chest Color",   new Color(120, 0, 150, 180));   // tumma violetti
        shulkerColor       = new SettingColor("Shulker Color",       new Color(255, 105, 180, 180)); // pinkki
        furnaceColor       = new SettingColor("Furnace Color",       new Color(120, 120, 120, 180)); // harmaa

        // Suodatukset
        drawChests   = new SettingBoolean("Draw Chests", true);
        drawTrapped  = new SettingBoolean("Draw Trapped Chests", true);
        drawBarrel   = new SettingBoolean("Draw Barrels", true);
        drawEnder    = new SettingBoolean("Draw Ender Chests", true);
        drawShulker  = new SettingBoolean("Draw Shulkers", true);
        drawFurnace  = new SettingBoolean("Draw Furnaces", true);

        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256}, 64);
        boxMode = new SettingMode("Box Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");

        // Piilotetut väriasetukset
        addHiddenSetting(chestColor.getSetting());
        addHiddenSetting(trappedChestColor.getSetting());
        addHiddenSetting(barrelColor.getSetting());
        addHiddenSetting(enderChestColor.getSetting());
        addHiddenSetting(shulkerColor.getSetting());
        addHiddenSetting(furnaceColor.getSetting());

        addHiddenSetting(toggleKey);

        // Näkyvät asetukset
        addSetting(renderDistance);
        addSetting(boxMode);
        addSetting(drawChests);
        addSetting(drawTrapped);
        addSetting(drawBarrel);
        addSetting(drawEnder);
        addSetting(drawShulker);
        addSetting(drawFurnace);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        int chunkRadius = (int) Math.ceil(renderDistance.getValue() / 16.0) + 1;
        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                    if (blockCenter.distanceToSqr(cameraPos) > maxDistSq) continue;

                    StorageType type = getStorageType(blockEntity);
                    if (type == StorageType.OTHER) continue;
                    if (!shouldDrawType(type)) continue;

                    Color baseColor = getColorForType(type).getCurrentColor();
                    int sideColor = new Color(baseColor.r, baseColor.g, baseColor.b, 30).getARGB();
                    int lineColor = baseColor.getARGB();

                    ShapeModeEnum mode = switch (boxMode.getMode()) {
                        case "Filled" -> ShapeModeEnum.SIDES;
                        case "Both"   -> ShapeModeEnum.BOTH;
                        default       -> ShapeModeEnum.LINES;
                    };

                    double half = 0.5;
                    Vec3 cam = cameraPos;
                    event.getRenderer().drawBox(
                            pos.getX() + 0.5 - half, pos.getY(), pos.getZ() + 0.5 - half,
                            pos.getX() + 0.5 + half, pos.getY() + 1.0, pos.getZ() + 0.5 + half,
                            sideColor, lineColor, mode, 0
                    );
                }
            }
        }
    }

    private StorageType getStorageType(BlockEntity be) {
        if (be instanceof ChestBlockEntity) {
            // Tarkista onko trapped chest? ChestBlockEntity ei erota, pitää katsoa lohkon tyyppiä
            // Yksinkertaisuuden vuoksi käytetään nimeä tai lohkoa
            if (be.getBlockState().getBlock().getDescriptionId().contains("trapped")) {
                return StorageType.TRAPPED_CHEST;
            }
            return StorageType.CHEST;
        }
        if (be instanceof BarrelBlockEntity) return StorageType.BARREL;
        if (be instanceof EnderChestBlockEntity) return StorageType.ENDER_CHEST;
        if (be instanceof ShulkerBoxBlockEntity) return StorageType.SHULKER;
        if (be instanceof AbstractFurnaceBlockEntity) return StorageType.FURNACE;
        return StorageType.OTHER;
    }

    private boolean shouldDrawType(StorageType type) {
        return switch (type) {
            case CHEST         -> drawChests.get();
            case TRAPPED_CHEST -> drawTrapped.get();
            case BARREL        -> drawBarrel.get();
            case ENDER_CHEST   -> drawEnder.get();
            case SHULKER       -> drawShulker.get();
            case FURNACE       -> drawFurnace.get();
            default -> false;
        };
    }

    // Muutetaan paluutyyppi SettingColor:ksi
    private SettingColor getColorForType(StorageType type) {
        return switch (type) {
            case CHEST         -> chestColor;
            case TRAPPED_CHEST -> trappedChestColor;
            case BARREL        -> barrelColor;
            case ENDER_CHEST   -> enderChestColor;
            case SHULKER       -> shulkerColor;
            case FURNACE       -> furnaceColor;
            default -> chestColor; // fallback
        };
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Chest", chestColor),
                new NamedColor("Trapped Chest", trappedChestColor),
                new NamedColor("Barrel", barrelColor),
                new NamedColor("Ender Chest", enderChestColor),
                new NamedColor("Shulker", shulkerColor),
                new NamedColor("Furnace", furnaceColor)
        );
    }

    // Värinmuokkausikkunan avaus
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("chestesp_color", "ChestESP Color Customizer", sw, sh, content);
    }
}