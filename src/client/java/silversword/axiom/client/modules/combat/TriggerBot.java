package silversword.axiom.client.modules.combat;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.killaura.AttackController;
import silversword.axiom.client.modules.moduleutils.killaura.TargetManager;
import silversword.axiom.client.setting.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class TriggerBot extends AxiomMod implements KeybindConfigurable {

    private final AttackController attackController = new AttackController();
    private final TargetManager targetManager = new TargetManager();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingBoolean onlySword = new SettingBoolean("Only Sword", false);
    private final SettingMode targetMode = new SettingMode("Target Mode", new String[]{"Players", "Mobs", "Both"}, "Both");
    private final SettingSlider range = new SettingSlider("Range", new double[]{3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0}, 4.5);
    private final SettingSlider minCps = new SettingSlider("Min CPS", new double[]{4,5,6,7,8,9,10,12,15}, 8);
    private final SettingSlider maxCps = new SettingSlider("Max CPS", new double[]{6,7,8,9,10,12,15,20}, 12);
    private final SettingBoolean checkWalls = new SettingBoolean("Check Walls", false);
    private final SettingBoolean ignoreBots = new SettingBoolean("Ignore Bots", true);
    private final SettingBoolean onlyWhenHolding = new SettingBoolean("Only When Holding", false);

    public TriggerBot() {
        super("Trigger Bot", "Automatically attacks when aiming at an entity", ModuleCategory.COMBAT);
        addSetting(targetMode);
        addSetting(range);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(onlySword);
        addSetting(checkWalls);
        addSetting(ignoreBots);
        addSetting(onlyWhenHolding);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() { attackController.reset(); }
    @Override
    protected void onDisable() { attackController.reset(); }

    @Override
    protected void onTick() {

    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null) return;

        if (onlyWhenHolding.get() && !mc.options.keyAttack.isDown()) return;
        if (onlySword.get() && !mc.player.getMainHandItem().is(ItemTags.SWORDS)) return;

        // Haetaan kohde TargetManagerilla (sama kuin KillAurassa)
        LivingEntity target = targetManager.selectTarget(
                mc.player, mc.level,
                "Distance", ignoreBots.get(), targetMode.getMode()
        );
        if (target == null) return;

        // Etäisyys
        if (mc.player.distanceTo(target) > range.getValue()) return;

        // Seinät
        if (checkWalls.get() && !isTargetVisible(target)) return;

        // Onko kohde ristin alla?
        if (!isCrosshairOnTarget(target)) return;

        // Hyökkäys
        if (attackController.canAttack(mc.player)) {
            performAttack(target);
        }
    }

    // ---------- Kopioidut apumetodit KillAurasta ----------
    private boolean isCrosshairOnTarget(LivingEntity target) {
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            return entityHit.getEntity() == target;
        }
        return false;
    }

    private boolean isTargetVisible(LivingEntity target) {
        return mc.player.hasLineOfSight(target);
    }

    private void performAttack(LivingEntity target) {
        if (!isEnabled() || target == null || !target.isAlive()) return;
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(mc.player.getUsedItemHand());
        attackController.recordAttack();
    }
}