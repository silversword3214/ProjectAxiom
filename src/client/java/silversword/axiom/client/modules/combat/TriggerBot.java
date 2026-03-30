package silversword.axiom.client.modules.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.AttackController;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class TriggerBot extends AxiomMod implements KeybindConfigurable {

    private final AttackController attackController = new AttackController();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingBoolean onlySword = new SettingBoolean("Only Sword", false);

    private final SettingMode targetMode = new SettingMode(
            "Target Mode",
            new String[]{"Players", "Mobs", "Both"},
            "Both"
    );

    private final SettingSlider range = new SettingSlider(
            "Range",
            new double[]{3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0},
            4.5
    );

    private final SettingSlider minCps = new SettingSlider(
            "Min CPS",
            new double[]{4, 5, 6, 7, 8, 9, 10, 12, 15},
            8
    );

    private final SettingSlider maxCps = new SettingSlider(
            "Max CPS",
            new double[]{6, 7, 8, 9, 10, 12, 15, 20},
            12
    );

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
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        attackController.reset();
    }

    @Override
    protected void onDisable() {}

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (onlyWhenHolding.get() && !mc.options.keyAttack.isDown()) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) hit).getEntity();
        if (!(target instanceof LivingEntity living)) return;

        // Etäisyystarkistus
        if (mc.player.distanceTo(target) > range.getValue()) return;

        // Tarkistetaan kohdetyyppi
        boolean isPlayer = target instanceof Player;
        boolean isMob = !isPlayer;

        String mode = targetMode.getMode();
        if (mode.equals("Players") && !isPlayer) return;
        if (mode.equals("Mobs") && !isMob) return;

        // Botin tunnistus (vain pelaajille)
        if (ignoreBots.get() && isPlayer && isBot((Player) target)) return;

        // Seinätarkistus
        if (checkWalls.get() && !isTargetVisible(living)) return;

        if (onlySword.get()) {
            net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
            if (!held.is(net.minecraft.tags.ItemTags.SWORDS)) return;
        }

        // Hyökkäys
        if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue())) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());
            attackController.recordAttack();
        }
    }

    private boolean isBot(Player player) {
        return mc.getConnection().getOnlinePlayers().stream()
                .noneMatch(entry -> entry.getProfile().id().equals(player.getUUID()));
    }

    private boolean isTargetVisible(LivingEntity target) {
        return mc.player.hasLineOfSight(target);
    }
}