package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;

import java.util.ArrayList;
import java.util.List;

public final class Trajectories extends AxiomMod implements KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    private final SettingColor lineColor;
    private final SettingColor impactColor;
    private final SettingBoolean drawImpact;
    private final SettingMode lineStyle;
    private final SettingBoolean drawThroughBlocks;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final List<Vec3> points = new ArrayList<>();
    private Vec3 impactPoint = null;
    private HitResult.Type lastHitType = HitResult.Type.MISS;
    private Direction hitSide = Direction.UP;

    public Trajectories() {
        super("Trajectories", "Shows projectile trajectory", ModuleCategory.RENDER);

        lineColor = new SettingColor("Line Color", new Color(0, 255, 0, 180));
        impactColor = new SettingColor("Impact Color", new Color(0, 255, 0, 180));
        drawImpact = new SettingBoolean("Draw Impact Point", true);
        lineStyle = new SettingMode("Line Style", new String[]{"Solid", "Dashed"}, "Solid");
        drawThroughBlocks = new SettingBoolean("Draw Through Blocks", false);

        addHiddenSetting(lineColor.getSetting());
        addHiddenSetting(impactColor.getSetting());
        addHiddenSetting(toggleKey);
        addSetting(drawImpact);
        addSetting(lineStyle);
        addSetting(drawThroughBlocks);
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

        Renderer3D renderer = event.getRenderer();

        Player player = mc.player;
        ItemStack heldItem = player.getMainHandItem();

        if (!player.isUsingItem() || !isThrowable(heldItem)) {
            points.clear();
            impactPoint = null;
            return;
        }

        calculateTrajectory(player, heldItem);

        if (points.size() < 2) return;



        Color currentLineColor = lastHitType == HitResult.Type.ENTITY ?
                new Color(255, 0, 0, 30) : lineColor;

        boolean dashed = "Dashed".equals(lineStyle.getMode());

        for (int i = 0; i < points.size() - 1; i++) {
            if (dashed && i % 2 == 1) continue;

            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            if (!drawThroughBlocks.get() && !isVisible(p1, p2)) continue;

            renderer.drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, currentLineColor);
        }

        if (drawImpact.get() && impactPoint != null) {
            Color currentImpactColor = lastHitType == HitResult.Type.ENTITY ?
                    new Color(255, 0, 0, 30) : impactColor;

            if (lastHitType == HitResult.Type.ENTITY) {
                double size = 0.5;
                renderer.drawBox(
                        impactPoint.x - size/2, impactPoint.y - size/2, impactPoint.z - size/2,
                        impactPoint.x + size/2, impactPoint.y + size/2, impactPoint.z + size/2,
                        currentImpactColor, currentImpactColor, ShapeModeEnum.BOTH, 0
                );
            } else if (lastHitType == HitResult.Type.BLOCK) {
                drawOrientedSquare(event, impactPoint, hitSide, currentImpactColor);
            }
        }
    }

    private void drawOrientedSquare(Render3DEvent event, Vec3 point, Direction side, Color color) {

        Renderer3D renderer = event.getRenderer();

        double size = 0.3;
        Vec3 right, up;

        switch (side) {
            case UP:
            case DOWN:
                right = new Vec3(size, 0, 0);
                up = new Vec3(0, 0, size);
                break;
            case NORTH:
            case SOUTH:
                right = new Vec3(size, 0, 0);
                up = new Vec3(0, size, 0);
                break;
            case EAST:
            case WEST:
                right = new Vec3(0, 0, size);
                up = new Vec3(0, size, 0);
                break;
            default:
                return;
        }

        Vec3 p1 = point.add(right).add(up);
        Vec3 p2 = point.add(right).subtract(up);
        Vec3 p3 = point.subtract(right).subtract(up);
        Vec3 p4 = point.subtract(right).add(up);

        renderer.drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, color);
        renderer.drawLine(p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, color);
        renderer.drawLine(p3.x, p3.y, p3.z, p4.x, p4.y, p4.z, color);
        renderer.drawLine(p4.x, p4.y, p4.z, p1.x, p1.y, p1.z, color);
    }

    private boolean isThrowable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof TridentItem;
    }

    private void calculateTrajectory(Player player, ItemStack item) {
        points.clear();
        impactPoint = null;
        lastHitType = HitResult.Type.MISS;

        // Lasketaan lähtöpiste – yritetään ottaa aseen kärki
        Vec3 start = getLaunchPoint(player, item);
        Vec3 lookVec = player.getViewVector(1.0f);

        double velocity = getInitialVelocity(player, item);
        double gravity = 0.05;
        double step = 0.1;
        int maxSteps = 200;

        Vec3 pos = start;
        Vec3 vel = lookVec.scale(velocity);

        for (int i = 0; i < maxSteps; i++) {
            points.add(pos);

            Vec3 nextPos = pos.add(vel.scale(step));

            Entity hitEntity = raycastEntity(pos, nextPos, player);
            if (hitEntity != null) {
                impactPoint = getClosestPointOnSegment(pos, nextPos, hitEntity.getBoundingBox());
                lastHitType = HitResult.Type.ENTITY;
                points.add(impactPoint);
                break;
            }

            BlockHitResult hit = mc.level.clip(new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                impactPoint = hit.getLocation();
                hitSide = hit.getDirection();
                lastHitType = HitResult.Type.BLOCK;
                points.add(impactPoint);
                break;
            }

            vel = vel.add(0, -gravity * step, 0);

            if (nextPos.y < mc.level.getMinY()) break;

            pos = nextPos;
        }
    }

    private Vec3 getLaunchPoint(Player player, ItemStack stack) {
        // Oletus: silmien korkeus
        Vec3 eyePos = player.getEyePosition();
        Item item = stack.getItem();

        // Pieni offset jouselle ja ristijouselle (nuoli lähtee hieman alempaa)
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            // Oletetaan että nuoli lähtee noin 0.1 lohkoa silmien alapuolelta
            return eyePos.add(0, -0.1, 0);
        }
        return eyePos;
    }

    private Entity raycastEntity(Vec3 from, Vec3 to, Player player) {
        Vec3 delta = to.subtract(from);
        int steps = 10;

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = from.add(delta.scale(t));

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == player || !entity.isAlive()) continue;
                if (entity.getBoundingBox().inflate(0.1).contains(point)) {
                    return entity;
                }
            }
        }
        return null;
    }

    private Vec3 getClosestPointOnSegment(Vec3 from, Vec3 to, AABB box) {
        return from.add(to).scale(0.5);
    }

    private double getInitialVelocity(Player player, ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BowItem) {
            int useTime = player.getTicksUsingItem();
            float pull = Math.min((float) useTime / 20.0f, 1.0f);
            return 3.0 * pull;
        } else if (item instanceof CrossbowItem) {
            return 3.0;
        } else if (item instanceof TridentItem) {
            return 2.5;
        }
        return 1.0;
    }

    private boolean isVisible(Vec3 from, Vec3 to) {
        if (drawThroughBlocks.get()) return true;
        BlockHitResult hit = mc.level.clip(new ClipContext(
                from, to,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    @Override
    protected void onDisable() {
        points.clear();
        impactPoint = null;
        super.onDisable();
    }
}