package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import java.util.List;
import java.util.Optional;

/**
 * Trajectories – ennustaa ammuksen lentoradan kädestä lähtien.
 *
 *  - Heittotavarat (lumi, muna, pearl, potionit): käsi sivulla/alhaalla
 *  - Jousi/varsijousi ladattuna: omat offsetit (Bow * -sliderit)
 *  - Osuma elävään entityyn: kiinteän kokoinen punainen boksi osumakohdassa
 *    (ei entityn bounding box), koko säädettävissä
 *  - Osuma pintaan: neliö pinnan suuntaisesti, koko säädettävissä
 *  - Item-entityt, XP-orbet, nuolet yms. OHITETAAN – vain LivingEntity kelpaa
 */
public final class Trajectories extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // ── Värit ──────────────────────────────────────────────────────────────
    private final SettingColor pathColor;
    private final SettingColor impactColor;
    private final SettingColor entityColor;

    // ── Näkyvyys / simulaatio ─────────────────────────────────────────────
    private final SettingBoolean showPath;
    private final SettingBoolean showImpact;
    private final SettingBoolean highlightEntity;
    private final SettingBoolean onlyWhenCharging;
    private final SettingNumber  maxTicks;
    private final SettingNumber  pathStep;
    private final SettingNumber  lineThickness;

    // ── Koot ──────────────────────────────────────────────────────────────
    private final SettingNumber  impactSize;
    private final SettingNumber  entityHitSize;

    // ── Käden offset – heittotavarat ──────────────────────────────────────
    private final SettingNumber handSideOffset;
    private final SettingNumber handDownOffset;
    private final SettingNumber handForwardOffset;
    private final SettingNumber handRaiseOffset;

    // ── Käden offset – jousi ladattuna ────────────────────────────────────
    private final SettingNumber bowSideOffset;
    private final SettingNumber bowDownOffset;
    private final SettingNumber bowForwardOffset;
    private final SettingNumber bowRaiseOffset;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Trajectories() {
        super("Trajectories", "Predicts projectile trajectories from the hand", ModuleCategory.RENDER);

        // Värit
        pathColor   = new SettingColor("Path Color",   new Color(255, 255, 255, 200));
        impactColor = new SettingColor("Impact Color", new Color(255, 200,  60, 255));
        entityColor = new SettingColor("Entity Color", new Color(255,  40,  40, 160));

        // Näkyvyys / simulaatio
        showPath         = new SettingBoolean("Show Path", true);
        showImpact       = new SettingBoolean("Show Impact", true);
        highlightEntity  = new SettingBoolean("Highlight Entity", true);
        onlyWhenCharging = new SettingBoolean("Only While Charging", false);

        maxTicks      = new SettingNumber("Max Ticks",     40,  500, 1.0,  200);
        pathStep      = new SettingNumber("Path Step",      1,    5,  1.0,  2);
        lineThickness = new SettingNumber("Thickness",      0.5,  4.0, 0.1,  1.5);

        // Koot – step 0.01
        impactSize    = new SettingNumber("Impact Size",     0.05, 1.50, 0.01, 0.30);
        entityHitSize = new SettingNumber("Entity Hit Size", 0.05, 1.50, 0.01, 0.25);

        // Käden offset – heittotavarat – step 0.01
        handSideOffset    = new SettingNumber("Hand Side",    0.0,   0.60, 0.01, 0.13);
        handDownOffset    = new SettingNumber("Hand Down",    0.0,   0.40, 0.01, 0.25);
        handForwardOffset = new SettingNumber("Hand Forward", -0.30, 0.40, 0.01, 0.10);
        handRaiseOffset   = new SettingNumber("Hand Raise",   -0.20, 0.40, 0.01, 0.20);

        // Käden offset – jousi ladattuna – step 0.01
        bowSideOffset    = new SettingNumber("Bow Side",     0.0,   0.40, 0.01,  0.07);
        bowDownOffset    = new SettingNumber("Bow Down",     0.0,   0.40, 0.01,  0.15);
        bowForwardOffset = new SettingNumber("Bow Forward", -0.30,  0.30, 0.01, -0.10);
        bowRaiseOffset   = new SettingNumber("Bow Raise",   -0.20,  0.20, 0.01,  0.14);

        // Piilotetut väriasetukset
        addHiddenSetting(pathColor.getSetting());
        addHiddenSetting(impactColor.getSetting());
        addHiddenSetting(entityColor.getSetting());
        addHiddenSetting(toggleKey);

        // Näkyvät asetukset
        addSetting(showPath);
        addSetting(showImpact);
        addSetting(highlightEntity);
        addSetting(onlyWhenCharging);
        addSetting(maxTicks);
        addSetting(pathStep);
        addSetting(lineThickness);
        addSetting(impactSize);
        addSetting(entityHitSize);

        addSetting(handSideOffset);
        addSetting(handDownOffset);
        addSetting(handForwardOffset);
        addSetting(handRaiseOffset);

        addSetting(bowSideOffset);
        addSetting(bowDownOffset);
        addSetting(bowForwardOffset);
        addSetting(bowRaiseOffset);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        // Ei tarvita
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RENDER
    // ─────────────────────────────────────────────────────────────────────────

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        ProjectileData data = resolveProjectile();
        if (data == null) return;
        if (onlyWhenCharging.get() && !data.charging) return;

        Renderer3D renderer = event.getRenderer();

        // Lähtöpiste kädestä
        Vec3 start = getHandPosition(mc.player, event.getTickDelta(), data.charging);
        Vec3 look  = mc.player.getViewVector(event.getTickDelta());

        Vec3 pos = start;
        Vec3 vel = look.scale(data.velocity);

        int   pathArgb   = pathColor.getCurrentColor().getARGB();
        int   impactArgb = impactColor.getCurrentColor().getARGB();
        int   entityArgb = entityColor.getCurrentColor().getARGB();
        float thickness  = (float) lineThickness.getValue();
        int   maxT       = (int) maxTicks.getValue();
        int   step       = Math.max(1, (int) pathStep.getValue());

        double impactHalf = impactSize.getValue();
        double entityHalf = entityHitSize.getValue();

        Vec3 prevSegStart = pos;

        for (int i = 0; i < maxT; i++) {
            vel = vel.add(0.0, -data.gravity, 0.0);
            Vec3 next = pos.add(vel);

            // 1) Entity-osuma – vain elävät
            EntityHitResult entityHit = findLivingEntityOnPath(pos, next);

            // 2) Lohko-osuma
            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    pos, next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player));
            boolean blockBlocked = blockHit.getType() == HitResult.Type.BLOCK;

            double entityDist = entityHit != null ? pos.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;
            double blockDist  = blockBlocked   ? pos.distanceToSqr(blockHit.getLocation())   : Double.MAX_VALUE;

            boolean hitEntity = entityHit != null && entityDist <= blockDist;
            boolean hitBlock  = blockBlocked && !hitEntity;

            Vec3 segEnd = hitEntity ? entityHit.getLocation()
                    : hitBlock  ? blockHit.getLocation()
                    : next;

            // Viiva
            if (showPath.get() && (hitEntity || hitBlock || i % step == 0)) {
                renderer.drawLine(
                        prevSegStart.x, prevSegStart.y, prevSegStart.z,
                        segEnd.x, segEnd.y, segEnd.z,
                        pathArgb, thickness);
                prevSegStart = segEnd;
            }

            // ── OSUMA ELÄVÄÄN ENTITYYN: kiinteän kokoinen boksi osumakohdassa
            if (hitEntity) {
                if (highlightEntity.get()) {
                    drawFixedHitBox(renderer, entityHit.getLocation(), entityHalf, entityArgb);
                }
                break;
            }

            // ── OSUMA PINTAAN: neliö pinnan suuntaisesti
            if (hitBlock) {
                if (showImpact.get()) {
                    drawFaceQuad(renderer, blockHit, impactArgb, (float) impactHalf);
                }
                break;
            }

            pos = next;
            vel = vel.scale(data.drag);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  KIINTEÄN KOKOINEN BOKSI OSUMAKOHDASSA
    // ─────────────────────────────────────────────────────────────────────────

    private void drawFixedHitBox(Renderer3D renderer, Vec3 center, double half, int color) {
        double minX = center.x - half, minY = center.y - half, minZ = center.z - half;
        double maxX = center.x + half, maxY = center.y + half, maxZ = center.z + half;

        renderer.boxFill(minX, minY, minZ, maxX, maxY, maxZ, color, 0);
        renderer.boxOutline(minX, minY, minZ, maxX, maxY, maxZ, color, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  KÄDEN SIJAINTI
    // ─────────────────────────────────────────────────────────────────────────

    private Vec3 getHandPosition(Player player, float tickDelta, boolean charging) {
        Vec3 eye  = player.getEyePosition(tickDelta);
        Vec3 look = player.getViewVector(tickDelta);

        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();

        double side, down, forward, raise;
        if (charging) {
            side    = bowSideOffset.getValue();
            down    = bowDownOffset.getValue();
            forward = bowForwardOffset.getValue();
            raise   = bowRaiseOffset.getValue();
        } else {
            side    = handSideOffset.getValue();
            down    = handDownOffset.getValue();
            forward = handForwardOffset.getValue();
            raise   = handRaiseOffset.getValue();
        }

        return eye.add(right.scale(side))
                .add(0, -down + raise, 0)
                .add(look.scale(forward));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NELIÖ PINNAN SUUNTAISESTI
    // ─────────────────────────────────────────────────────────────────────────

    private void drawFaceQuad(Renderer3D renderer, BlockHitResult hit, int color, float half) {
        Direction dir = hit.getDirection();
        Vec3 normal  = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        Vec3 center  = hit.getLocation().add(normal.scale(0.02));

        Vec3 t1, t2;
        switch (dir.getAxis()) {
            case Y  -> { t1 = new Vec3(1, 0, 0); t2 = new Vec3(0, 0, 1); }
            case X  -> { t1 = new Vec3(0, 1, 0); t2 = new Vec3(0, 0, 1); }
            default -> { t1 = new Vec3(1, 0, 0); t2 = new Vec3(0, 1, 0); }
        }

        Vec3 p1 = center.add(t1.scale(-half)).add(t2.scale(-half));
        Vec3 p2 = center.add(t1.scale( half)).add(t2.scale(-half));
        Vec3 p3 = center.add(t1.scale( half)).add(t2.scale( half));
        Vec3 p4 = center.add(t1.scale(-half)).add(t2.scale( half));

        renderer.quad(
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,
                p4.x, p4.y, p4.z,
                color);

        int outline = (color & 0x00FFFFFF) | 0xFF000000;
        renderer.drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, outline, 2.0f);
        renderer.drawLine(p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, outline, 2.0f);
        renderer.drawLine(p3.x, p3.y, p3.z, p4.x, p4.y, p4.z, outline, 2.0f);
        renderer.drawLine(p4.x, p4.y, p4.z, p1.x, p1.y, p1.z, outline, 2.0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ENTITY-OSUMAN ETSINTÄ
    // ─────────────────────────────────────────────────────────────────────────

    private EntityHitResult findLivingEntityOnPath(Vec3 from, Vec3 to) {
        AABB search = new AABB(from, to).inflate(1.0);
        Entity closest = null;
        Vec3 closestHit = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.level.getEntities(mc.player, search,
                ent -> ent instanceof LivingEntity && ent.isAlive())) {
            if (e == mc.player) continue;

            AABB box = e.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < closestDist) {
                    closestDist = d;
                    closest = e;
                    closestHit = hit.get();
                }
            }
        }
        return closest != null ? new EntityHitResult(closest, closestHit) : null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AMMUKSEN TUNNISTUS
    // ─────────────────────────────────────────────────────────────────────────

    private ProjectileData resolveProjectile() {
        Player player = mc.player;
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();

        InteractionHand usingHand = player.isUsingItem() ? player.getUsedItemHand() : null;

        ProjectileData d = resolveFor(main, usingHand == InteractionHand.MAIN_HAND);
        if (d != null) return d;

        return resolveFor(off, usingHand == InteractionHand.OFF_HAND);
    }

    private ProjectileData resolveFor(ItemStack stack, boolean charging) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() instanceof BowItem) {
            double velocity;
            if (charging) {
                int useTicks = mc.player.getTicksUsingItem();
                float charge = Math.min(1.0f, useTicks / 20.0f);
                velocity = 3.0 * charge;
                if (velocity < 0.1) velocity = 3.0;
            } else {
                velocity = 3.0;
            }
            return new ProjectileData(velocity, 0.05, 0.99, charging);
        }

        if (stack.is(Items.CROSSBOW))          return new ProjectileData(3.15, 0.05, 0.99, charging);
        if (stack.is(Items.TRIDENT))           return new ProjectileData(2.5,  0.05, 0.99, charging);
        if (stack.is(Items.SNOWBALL))          return new ProjectileData(1.5,  0.03, 0.99, false);
        if (stack.is(Items.EGG))               return new ProjectileData(1.5,  0.03, 0.99, false);
        if (stack.is(Items.ENDER_PEARL))       return new ProjectileData(1.5,  0.03, 0.99, false);
        if (stack.is(Items.SPLASH_POTION))     return new ProjectileData(0.5,  0.05, 0.99, false);
        if (stack.is(Items.LINGERING_POTION))  return new ProjectileData(0.5,  0.05, 0.99, false);
        if (stack.is(Items.EXPERIENCE_BOTTLE)) return new ProjectileData(0.7,  0.07, 0.99, false);

        return null;
    }

    private static final class ProjectileData {
        final double  velocity;
        final double  gravity;
        final double  drag;
        final boolean charging;

        ProjectileData(double velocity, double gravity, double drag, boolean charging) {
            this.velocity = velocity;
            this.gravity  = gravity;
            this.drag     = drag;
            this.charging = charging;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VÄRIKONFIGURAATIO
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<NamedColor> getColors() {
        return List.of(
                new NamedColor("Path",   pathColor),
                new NamedColor("Impact", impactColor),
                new NamedColor("Entity", entityColor)
        );
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("trajectories_color", "Trajectories Color Customizer", sw, sh, content);
    }
}