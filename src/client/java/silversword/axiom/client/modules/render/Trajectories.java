package silversword.axiom.client.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;

import java.util.ArrayList;
import java.util.List;

public final class Trajectories extends AxiomMod implements KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final SettingColor lineColor;
    private final SettingColor impactColor;
    private final SettingBoolean drawImpact;
    private final SettingMode lineStyle;
    private final SettingBoolean drawThroughBlocks;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final List<Vec3d> points = new ArrayList<>();
    private Vec3d impactPoint = null;
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

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        PlayerEntity player = mc.player;
        ItemStack heldItem = player.getMainHandStack();

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

            Vec3d p1 = points.get(i);
            Vec3d p2 = points.get(i + 1);

            if (!drawThroughBlocks.get() && !isVisible(p1, p2)) continue;

            event.render.drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, currentLineColor);
        }

        if (drawImpact.get() && impactPoint != null) {
            Color currentImpactColor = lastHitType == HitResult.Type.ENTITY ?
                    new Color(255, 0, 0, 30) : impactColor;

            if (lastHitType == HitResult.Type.ENTITY) {
                double size = 0.5;
                event.render.drawBox(
                        impactPoint.x - size/2, impactPoint.y - size/2, impactPoint.z - size/2,
                        impactPoint.x + size/2, impactPoint.y + size/2, impactPoint.z + size/2,
                        currentImpactColor, currentImpactColor, ShapeMode.Both, 0
                );
            } else if (lastHitType == HitResult.Type.BLOCK) {
                drawOrientedSquare(event, impactPoint, hitSide, currentImpactColor);
            }
        }
    }

    private void drawOrientedSquare(Render3DEvent event, Vec3d point, Direction side, Color color) {
        double size = 0.3;
        Vec3d right, up;

        switch (side) {
            case UP:
            case DOWN:
                right = new Vec3d(size, 0, 0);
                up = new Vec3d(0, 0, size);
                break;
            case NORTH:
            case SOUTH:
                right = new Vec3d(size, 0, 0);
                up = new Vec3d(0, size, 0);
                break;
            case EAST:
            case WEST:
                right = new Vec3d(0, 0, size);
                up = new Vec3d(0, size, 0);
                break;
            default:
                return;
        }

        Vec3d p1 = point.add(right).add(up);
        Vec3d p2 = point.add(right).subtract(up);
        Vec3d p3 = point.subtract(right).subtract(up);
        Vec3d p4 = point.subtract(right).add(up);

        event.render.drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, color);
        event.render.drawLine(p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, color);
        event.render.drawLine(p3.x, p3.y, p3.z, p4.x, p4.y, p4.z, color);
        event.render.drawLine(p4.x, p4.y, p4.z, p1.x, p1.y, p1.z, color);
    }

    private boolean isThrowable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof TridentItem;
    }

    private void calculateTrajectory(PlayerEntity player, ItemStack item) {
        points.clear();
        impactPoint = null;
        lastHitType = HitResult.Type.MISS;

        // Lasketaan lähtöpiste – yritetään ottaa aseen kärki
        Vec3d start = getLaunchPoint(player, item);
        Vec3d lookVec = player.getRotationVec(1.0f);

        double velocity = getInitialVelocity(player, item);
        double gravity = 0.05;
        double step = 0.1;
        int maxSteps = 200;

        Vec3d pos = start;
        Vec3d vel = lookVec.multiply(velocity);

        for (int i = 0; i < maxSteps; i++) {
            points.add(pos);

            Vec3d nextPos = pos.add(vel.multiply(step));

            Entity hitEntity = raycastEntity(pos, nextPos, player);
            if (hitEntity != null) {
                impactPoint = getClosestPointOnSegment(pos, nextPos, hitEntity.getBoundingBox());
                lastHitType = HitResult.Type.ENTITY;
                points.add(impactPoint);
                break;
            }

            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    player
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                impactPoint = hit.getPos();
                hitSide = hit.getSide();
                lastHitType = HitResult.Type.BLOCK;
                points.add(impactPoint);
                break;
            }

            vel = vel.add(0, -gravity * step, 0);

            if (nextPos.y < mc.world.getBottomY()) break;

            pos = nextPos;
        }
    }

    private Vec3d getLaunchPoint(PlayerEntity player, ItemStack stack) {
        // Oletus: silmien korkeus
        Vec3d eyePos = player.getEyePos();
        Item item = stack.getItem();

        // Pieni offset jouselle ja ristijouselle (nuoli lähtee hieman alempaa)
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            // Oletetaan että nuoli lähtee noin 0.1 lohkoa silmien alapuolelta
            return eyePos.add(0, -0.1, 0);
        }
        return eyePos;
    }

    private Entity raycastEntity(Vec3d from, Vec3d to, PlayerEntity player) {
        Vec3d delta = to.subtract(from);
        int steps = 10;

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d point = from.add(delta.multiply(t));

            for (Entity entity : mc.world.getEntities()) {
                if (entity == player || !entity.isAlive()) continue;
                if (entity.getBoundingBox().expand(0.1).contains(point)) {
                    return entity;
                }
            }
        }
        return null;
    }

    private Vec3d getClosestPointOnSegment(Vec3d from, Vec3d to, Box box) {
        return from.add(to).multiply(0.5);
    }

    private double getInitialVelocity(PlayerEntity player, ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BowItem) {
            int useTime = player.getItemUseTime();
            float pull = Math.min((float) useTime / 20.0f, 1.0f);
            return 3.0 * pull;
        } else if (item instanceof CrossbowItem) {
            return 3.0;
        } else if (item instanceof TridentItem) {
            return 2.5;
        }
        return 1.0;
    }

    private boolean isVisible(Vec3d from, Vec3d to) {
        if (drawThroughBlocks.get()) return true;
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
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