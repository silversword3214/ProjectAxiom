package silversword.axiom.client.modules.player;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import silversword.axiom.client.event.player.UseBlockEvent;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class GhostHand extends AxiomMod implements KeybindConfigurable {
    public static GhostHand INSTANCE;

    private final Minecraft mc = Minecraft.getInstance();
    private final RenderCore core = RenderAPI.getInstance().getCore();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber range;
    private final SettingBoolean renderBlocks;
    private final SettingBoolean onlyThroughWalls;
    private final SettingMode renderMode;
    private final SettingNumber maxBlocks;

    // Nametag-asetukset
    private final SettingBoolean nametagEnabled;
    private final SettingNumber nametagScale;
    private final SettingNumber nametagOffset;
    private final SettingMode bgMode;
    private final SettingColor textColor;
    private final SettingColor backgroundColor;
    private final SettingColor outlineColor;

    private final List<BlockPos> interactiveBlocks = new ArrayList<>();
    private BlockPos currentTarget = null;

    public GhostHand() {
        super("Ghost Hand", "Allows you to interact with blocks through walls", ModuleCategory.PLAYER);
        INSTANCE = this;

        range = new SettingNumber("Range", 3, 20, 1, 6);
        renderBlocks = new SettingBoolean("Render Blocks", true);
        onlyThroughWalls = new SettingBoolean("Only Through Walls", true);
        renderMode = new SettingMode("Render Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");
        maxBlocks = new SettingNumber("Max Blocks", 10, 200, 10, 50);

        // Nametag-asetukset
        nametagEnabled = new SettingBoolean("Show Block Name", true);
        nametagScale = new SettingNumber("Nametag Scale", 0.5, 3.0, 0.1, 1.5);
        nametagOffset = new SettingNumber("Nametag Offset", -0.50, 2.0, 0.1, -0.20);
        bgMode = new SettingMode("Background", new String[]{"None", "Filled", "Outline", "Rounded"}, "Filled");
        textColor = new SettingColor("Text Color", new Color(255, 255, 255, 255));
        backgroundColor = new SettingColor("Background", new Color(0, 0, 0, 75));
        outlineColor = new SettingColor("Outline", new Color(255, 255, 255, 255));

        addHiddenSetting(toggleKey);
        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(backgroundColor.getSetting());
        addHiddenSetting(outlineColor.getSetting());

        addSetting(range);
        addSetting(renderBlocks);
        addSetting(onlyThroughWalls);
        addSetting(renderMode);
        addSetting(maxBlocks);
        addSetting(nametagEnabled);
        addSetting(nametagScale);
        addSetting(nametagOffset);
        addSetting(bgMode);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    private boolean isInteractiveBlock(BlockPos pos) {
        if (mc.level == null) return false;

        BlockState state = mc.level.getBlockState(pos);
        BlockEntity entity = mc.level.getBlockEntity(pos);

        if (entity != null) {
            return true;
        }

        return state.getBlock() == Blocks.CRAFTING_TABLE ||
                state.getBlock() == Blocks.ENCHANTING_TABLE ||
                state.getBlock() == Blocks.ANVIL ||
                state.getBlock() == Blocks.STONECUTTER ||
                state.getBlock() == Blocks.GRINDSTONE ||
                state.getBlock() == Blocks.LOOM ||
                state.getBlock() == Blocks.CARTOGRAPHY_TABLE ||
                state.getBlock() == Blocks.FLETCHING_TABLE ||
                state.getBlock() == Blocks.SMITHING_TABLE ||
                state.getBlock() == Blocks.COMPOSTER ||
                state.getBlock() == Blocks.BARREL ||
                state.getBlock() == Blocks.BLAST_FURNACE ||
                state.getBlock() == Blocks.SMOKER ||
                state.getBlock() == Blocks.BREWING_STAND ||
                state.getBlock() == Blocks.BEACON ||
                state.getBlock() == Blocks.CONDUIT ||
                state.getBlock() == Blocks.ENDER_CHEST ||
                state.getBlock() == Blocks.SHULKER_BOX ||
                state.getBlock() == Blocks.HOPPER ||
                state.getBlock() == Blocks.DROPPER ||
                state.getBlock() == Blocks.DISPENSER ||
                state.getBlock() == Blocks.COPPER_CHEST ||
                state.getBlock() == Blocks.TRAPPED_CHEST ||
                state.getBlock() == Blocks.WAXED_COPPER_CHEST ||
                state.getBlock() == Blocks.EXPOSED_COPPER_CHEST ||
                state.getBlock() == Blocks.WAXED_OXIDIZED_COPPER_CHEST ||
                state.getBlock() == Blocks.OXIDIZED_COPPER_CHEST ||
                state.getBlock() == Blocks.WAXED_EXPOSED_COPPER_CHEST ||
                state.getBlock() == Blocks.WAXED_WEATHERED_COPPER_CHEST ||
                state.getBlock() == Blocks.WEATHERED_COPPER_CHEST ||
                state.getBlock() == Blocks.JUKEBOX;
    }

    private void scanInteractiveBlocks() {
        interactiveBlocks.clear();
        if (mc.player == null || mc.level == null) return;

        int r = (int) Math.ceil(range.getValue());
        BlockPos center = mc.player.blockPosition();
        Vec3 eyePos = mc.player.getEyePosition();
        double maxDist = range.getValue();
        int max = (int) maxBlocks.getValue();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (interactiveBlocks.size() >= max) return;

                    BlockPos pos = center.offset(x, y, z);
                    double dist = eyePos.distanceTo(Vec3.atCenterOf(pos));
                    if (dist <= maxDist && isInteractiveBlock(pos)) {
                        interactiveBlocks.add(pos);
                    }
                }
            }
        }
    }

    private BlockHitResult raycastForInteractiveBlockHit(double maxDist) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 start = mc.player.getEyePosition();
        Vec3 direction = mc.player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(maxDist));

        double step = 0.1;
        BlockPos lastPos = null;
        for (double d = 0; d < maxDist; d += step) {
            Vec3 point = start.add(direction.scale(d));
            BlockPos pos = BlockPos.containing(point);
            if (pos.equals(lastPos)) continue;
            lastPos = pos;

            if (isInteractiveBlock(pos)) {
                BlockState state = mc.level.getBlockState(pos);
                VoxelShape shape = state.getShape(mc.level, pos);
                if (shape.isEmpty()) shape = state.getCollisionShape(mc.level, pos);
                if (!shape.isEmpty()) {
                    BlockHitResult hit = shape.clip(start, end, pos);
                    if (hit != null) {
                        return hit;
                    }
                }
                return new BlockHitResult(point, Direction.getApproximateNearest(direction.x, direction.y, direction.z), pos, false);
            }
        }
        return null;
    }

    @Subscribe
    private void onUseBlock(UseBlockEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        BlockHitResult targetHit = raycastForInteractiveBlockHit(range.getValue());
        if (targetHit == null) return;

        BlockPos pos = targetHit.getBlockPos();
        double dist = mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
        if (dist > range.getValue()) return;

        if (onlyThroughWalls.get()) {
            Vec3 start = mc.player.getEyePosition();
            Vec3 end = Vec3.atCenterOf(pos);
            ClipContext context = new ClipContext(
                    start, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
            );
            BlockHitResult normalHit = (BlockHitResult) mc.level.clip(context);
            if (normalHit != null && normalHit.getType() == HitResult.Type.BLOCK && normalHit.getBlockPos().equals(pos)) {
                return;
            }
        }

        event.setCancelled(true);

        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                targetHit,
                0
        );
        mc.getConnection().send(packet);
    }

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || !renderBlocks.get() || mc.player == null || mc.level == null) return;

        scanInteractiveBlocks();

        BlockHitResult hit = raycastForInteractiveBlockHit(range.getValue());
        currentTarget = hit != null ? hit.getBlockPos() : null;

        Renderer3D renderer = event.getRenderer();

        for (BlockPos pos : interactiveBlocks) {
            BlockState state = mc.level.getBlockState(pos);
            AABB box = state.getShape(mc.level, pos).bounds();
            double x1 = pos.getX() + box.minX;
            double y1 = pos.getY() + box.minY;
            double z1 = pos.getZ() + box.minZ;
            double x2 = pos.getX() + box.maxX;
            double y2 = pos.getY() + box.maxY;
            double z2 = pos.getZ() + box.maxZ;

            int lineColor;
            int sideColor;

            if (pos.equals(currentTarget)) {
                lineColor = 0xFFFFFF00; // 255,255,0
                sideColor = 0x32FFFF00; // 50,255,255,0
            } else {
                lineColor = 0xFFFFFFFF;
                sideColor = 0x1EFFFFFF; // 30,255,255,255
            }

            ShapeModeEnum mode;
            switch (renderMode.getMode()) {
                case "Filled" -> mode = ShapeModeEnum.SIDES;
                case "Both"   -> mode = ShapeModeEnum.BOTH;
                default       -> mode = ShapeModeEnum.LINES;
            }

            renderer.drawBox(x1, y1, z1, x2, y2, z2, sideColor, lineColor, mode, 0);
        }
    }

    // Nametag piirretään 2D-eventissä
    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || !nametagEnabled.get() || mc.player == null || mc.level == null) return;
        if (currentTarget == null) return;

        renderBlockNametag(event, currentTarget);
    }

    private void renderBlockNametag(Render2DEvent event, BlockPos pos) {
        if (mc.level == null) return;

        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        // 1. Haetaan blokin nimi
        String blockName = Component.translatable(state.getBlock().getDescriptionId()).getString();

        // 2. Lasketaan maailman koordinaatit (keskellä blockia, yläpinnan päällä + offset)
        AABB box = state.getShape(mc.level, pos).bounds();
        double worldX = pos.getX() + 0.5;
        double worldY = pos.getY() + box.maxY + nametagOffset.getValue();
        double worldZ = pos.getZ() + 0.5;

        // 3. Muunnos ruudulle NametagUtilsilla
        Vec3 screenPos = silversword.axiom.client.render.rendersystem.utils.render.NametagUtils.worldToScreen(new Vec3(worldX, worldY, worldZ));

        if (screenPos == null) return;

        double screenX = screenPos.x;
        double screenY = screenPos.y;
        double distance = screenPos.z; // w-arvo eli etäisyys

        // 4. Skaalaus (pienennetään etäisyyden mukaan, mutta pidetään järkevissä rajoissa)
        double distanceScale = 1.0 / Math.max(1.0, distance * 0.2); // Säädä 0.2 jos haluat pienenemisen olevan hitaampaa
        double finalScale = nametagScale.getValue() * distanceScale;
        finalScale = Math.max(finalScale, 0.4); // Minimikoko ettei teksti katoa

        // 5. Mitat käyttäen sinun TextUtils-luokkaasi
        // Lasketaan leveys: merkkien määrä * vakioleveys
        double textWidth = blockName.length() * TextUtils.CHAR_WIDTH;
        double textHeight = TextUtils.FONT_HEIGHT;

        double padding = 2.0;
        double bgWidth = (textWidth + padding * 2) * finalScale;
        double bgHeight = (textHeight + padding * 2) * finalScale;

        // Keskitys
        double renderX = screenX - (bgWidth / 2.0);
        double renderY = screenY - (bgHeight / 2.0);

        // 6. Piirtäminen
        // Varmistetaan että käytetään HUD-kerroksen oikeita mittoja matriisissa
        core.beginFrame(new Matrix4f().setOrtho(0, event.getScreenWidth(), event.getScreenHeight(), 0, -1000, 1000), new Matrix4f().identity());

        drawBackground(renderX, renderY, bgWidth, bgHeight, finalScale);

        TextRenderer text = TextRenderer.get();
        // Aloitetaan tekstin piirto finalScalella
        text.begin(finalScale, true, true); // Ei skaalausta tekstimoottorissa
        text.render(blockName, (float)(renderX + padding * finalScale), (float)(renderY + padding * finalScale), textColor.getCurrentColor(), false);
        text.end();
    }

    private void drawBackground(double x, double y, double width, double height, double finalScale) {
        String mode = bgMode.getMode();
        if (mode.equals("None")) return;

        int bgArgb = backgroundColor.getCurrentColor().getARGB();
        int outlineArgb = outlineColor.getCurrentColor().getARGB();
        double radius = 3.0 * finalScale;
        double thickness = Math.max(1.0, finalScale);

        if (mode.equals("Filled")) {
            core.addRect2D((float) x, (float) y, (float) width, (float) height, bgArgb);
        } else if (mode.equals("Outline")) {
            core.addRectOutline2D((float) x, (float) y, (float) width, (float) height, (float) thickness, outlineArgb);
        } else if (mode.equals("Rounded")) {
            core.addRoundedRect((float) x, (float) y, (float) width, (float) height, (float) radius, bgArgb);
        }

        if (!mode.equals("Outline") && outlineColor.getCurrentColor().getAlpha() > 0) {
            core.addRoundedRectOutline((float) x, (float) y, (float) width, (float) height, (float) radius, (float) thickness, outlineArgb);
        }
    }

    @Override
    protected void onTick() {}
}