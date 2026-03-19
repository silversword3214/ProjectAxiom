package silversword.axiom.client.modules.player;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;
import silversword.axiom.client.event.player.UseBlockEvent;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.utils.render.TextUtils;


import java.util.ArrayList;
import java.util.List;

public final class GhostHand extends AxiomMod implements KeybindConfigurable {
    public static GhostHand INSTANCE;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber range;
    private final SettingBoolean renderBlocks;
    private final SettingBoolean onlyThroughWalls;
    private final SettingMode renderMode;
    private final SettingNumber maxBlocks;

    // Nametag-asetukset (kopioitu NameTagsista)
    private final SettingBoolean nametagEnabled;
    private final SettingNumber nametagScale;
    private final SettingNumber nametagOffset;
    private final SettingMode bgMode;
    private final SettingColor textColor;
    private final SettingColor backgroundColor;
    private final SettingColor outlineColor;

    private final List<BlockPos> interactiveBlocks = new ArrayList<>();
    private BlockPos currentTarget = null;

    private final Vector3d nametagPos = new Vector3d();

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
        if (mc.world == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        BlockEntity entity = mc.world.getBlockEntity(pos);

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
                state.getBlock() == Blocks.JUKEBOX;
    }

    private void scanInteractiveBlocks() {
        interactiveBlocks.clear();
        if (mc.player == null || mc.world == null) return;

        int r = (int) Math.ceil(range.getValue());
        BlockPos center = mc.player.getBlockPos();
        Vec3d eyePos = mc.player.getEyePos();
        double maxDist = range.getValue();
        int max = (int) maxBlocks.getValue();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (interactiveBlocks.size() >= max) return;

                    BlockPos pos = center.add(x, y, z);
                    double dist = eyePos.distanceTo(Vec3d.ofCenter(pos));
                    if (dist <= maxDist && isInteractiveBlock(pos)) {
                        interactiveBlocks.add(pos);
                    }
                }
            }
        }
    }

    private BlockHitResult raycastForInteractiveBlockHit(double maxDist) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d start = mc.player.getEyePos();
        Vec3d direction = mc.player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(maxDist));

        double step = 0.1;
        BlockPos lastPos = null;
        for (double d = 0; d < maxDist; d += step) {
            Vec3d point = start.add(direction.multiply(d));
            BlockPos pos = BlockPos.ofFloored(point);
            if (pos.equals(lastPos)) continue;
            lastPos = pos;

            if (isInteractiveBlock(pos)) {
                BlockState state = mc.world.getBlockState(pos);
                VoxelShape shape = state.getOutlineShape(mc.world, pos);
                if (shape.isEmpty()) shape = state.getCollisionShape(mc.world, pos);
                if (!shape.isEmpty()) {
                    BlockHitResult hit = shape.raycast(start, end, pos);
                    if (hit != null) {
                        return hit;
                    }
                }
                return new BlockHitResult(point, Direction.getFacing(direction.x, direction.y, direction.z), pos, false);
            }
        }
        return null;
    }

    @AxiomEvent
    private void onUseBlock(UseBlockEvent event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        BlockHitResult targetHit = raycastForInteractiveBlockHit(range.getValue());
        if (targetHit == null) return;

        BlockPos pos = targetHit.getBlockPos();
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        if (dist > range.getValue()) return;

        if (onlyThroughWalls.get()) {
            Vec3d start = mc.player.getEyePos();
            Vec3d end = Vec3d.ofCenter(pos);
            RaycastContext context = new RaycastContext(
                    start, end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            );
            BlockHitResult normalHit = (BlockHitResult) mc.world.raycast(context);
            if (normalHit != null && normalHit.getType() == HitResult.Type.BLOCK && normalHit.getBlockPos().equals(pos)) {
                return;
            }
        }

        event.setCancelled(true);

        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                targetHit,
                0
        );
        mc.getNetworkHandler().sendPacket(packet);
    }

    @AxiomEvent
    private void onRender3D(Render3DEvent event) {
        // Tarvitaan NametagUtilsia varten
        NametagUtils.onRender(RenderUtils.view);
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || !renderBlocks.get() || mc.player == null || mc.world == null) return;

        scanInteractiveBlocks();

        BlockHitResult hit = raycastForInteractiveBlockHit(range.getValue());
        currentTarget = hit != null ? hit.getBlockPos() : null;

        for (BlockPos pos : interactiveBlocks) {
            BlockState state = mc.world.getBlockState(pos);
            Box box = state.getOutlineShape(mc.world, pos).getBoundingBox();
            double x1 = pos.getX() + box.minX;
            double y1 = pos.getY() + box.minY;
            double z1 = pos.getZ() + box.minZ;
            double x2 = pos.getX() + box.maxX;
            double y2 = pos.getY() + box.maxY;
            double z2 = pos.getZ() + box.maxZ;

            Color lineColor;
            Color sideColor;

            if (pos.equals(currentTarget)) {
                lineColor = new Color(255, 255, 0, 255);
                sideColor = new Color(255, 255, 0, 50);
            } else {
                lineColor = new Color(255, 255, 255, 255);
                sideColor = new Color(255, 255, 255, 30);
            }

            ShapeMode mode = switch (renderMode.getMode()) {
                case "Filled" -> ShapeMode.Sides;
                case "Both"   -> ShapeMode.Both;
                default       -> ShapeMode.Lines;
            };

            event.render.drawBox(x1, y1, z1, x2, y2, z2, sideColor, lineColor, mode, 0);
        }
    }

    // Nametag piirretään erillisessä 2D-eventissä, jotta se on oikeassa kerroksessa
    @AxiomEvent
    private void onRender2D(Render2DEvent event) {
        if (event.drawContext == null) return;
        if (!isEnabled() || !nametagEnabled.get() || mc.player == null || mc.world == null) return;
        if (currentTarget == null) return;

        renderBlockNametag(event, currentTarget);
    }

    private void renderBlockNametag(Render2DEvent event, BlockPos pos) {
        if (mc.world == null) return;

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;

        // Haetaan blokin nimi
        String blockName = Text.translatable(state.getBlock().getTranslationKey()).getString();

        // Lasketaan blokin yläpinnan korkeus
        Box box = state.getOutlineShape(mc.world, pos).getBoundingBox();
        double topY = pos.getY() + box.maxY;

        // Keskipiste ja offsetin verran yläpuolelle
        double x = pos.getX() + 0.5;
        double y = topY + nametagOffset.getValue();
        double z = pos.getZ() + 0.5;
        nametagPos.set(x, y, z);

        // Etäisyyspohjainen skaalaus (sama kuin NameTagsissa)
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        double dist = Math.sqrt(cameraPos.squaredDistanceTo(x, y, z));
        double distanceScale = MathHelper.clamp(1.0 - dist * 0.005, 0.8, 3.0);
        double finalScale = nametagScale.getValue() * distanceScale;

        // Muunnetaan 2D:ksi
        if (!NametagUtils.worldToScreen(nametagPos, finalScale)) return;

        NametagUtils.scale = finalScale;
        NametagUtils.begin(nametagPos, event.drawContext);

        // Piirretään nametag (tyyli kopioitu NameTagsin renderGenericNametagista)
        renderNametag(blockName);

        NametagUtils.end(event.drawContext);
    }

    private void renderNametag(String text) {
        TextRenderer textRenderer = TextRenderer.get();
        boolean shadow = false; // NameTagsissa shadow = false, mutta voidaan lisätä asetus myöhemmin

        // Lasketaan tekstin leveys ja korkeus TextUtilsin avulla
        double textWidth = text.length() * TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;
        double padding = 2.0;

        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;

        // Piirretään tausta asetuksen mukaan
        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        // Piirretään teksti
        textRenderer.begin(1.0, false, true); // scale=1.0, koska NametagUtils hoitaa skaalauksen
        textRenderer.render(text, -textWidth / 2, -textHeight / 2, textColor.getCurrentColor(), shadow);
        textRenderer.end();
    }

    private void drawBackground(double x, double y, double width, double height) {
        String mode = bgMode.getMode();
        if (mode.equals("None")) return;

        Renderer2D.COLOR.begin();

        if (mode.equals("Filled")) {
            Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, backgroundColor.getCurrentColor());
        } else if (mode.equals("Outline")) {
            Renderer2D.COLOR.boxLines(x - 1, y - 1, width + 2, height + 2, backgroundColor.getCurrentColor());
        } else if (mode.equals("Rounded")) {
            double radius = 3.0;
            Renderer2D.COLOR.drawRoundedRect(x - 1, y - 1, width + 2, height + 2, radius, backgroundColor.getCurrentColor());
        }

        // Outline-reunus (jos ei ole Outline-tila ja outline-värillä on alpha)
        if (!mode.equals("Outline") && outlineColor.getCurrentColor().getAlpha() > 0) {
            double outlineThickness = 1.0;
            double radius = 3.0;
            Renderer2D.COLOR.drawRoundedRectOutline(x - 1, y - 1, width + 2, height + 2, radius, outlineColor.getCurrentColor(), outlineThickness);
        }

        Renderer2D.COLOR.end();
        Renderer2D.COLOR.render();
    }

    @Override
    protected void onTick() {}
}