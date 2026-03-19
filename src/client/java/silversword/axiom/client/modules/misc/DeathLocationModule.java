package silversword.axiom.client.modules.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.misc.deathlocation.DeathEntry;
import silversword.axiom.client.modules.misc.deathlocation.DeathLocationManager;
import silversword.axiom.client.modules.misc.deathlocation.DeathLocationListWindow;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public class DeathLocationModule extends AxiomMod {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean wasAlive = true;

    public final SettingBoolean enabled = new SettingBoolean("Enabled", true);
    public final SettingBoolean showMessage = new SettingBoolean("Show Message", true);
    public final SettingKeybind openListKey = new SettingKeybind("Open List Key", 0);

    public DeathLocationModule() {
        super("DeathLocation", "Records your death locations", ModuleCategory.MISC);
        addSetting(enabled);
        addSetting(showMessage);
        addHiddenSetting(openListKey);
    }

    @Override
    protected void onTick() {
        if (!enabled.get() || mc.player == null) return;

        boolean alive = mc.player.isAlive();
        if (!alive && wasAlive) {
            onPlayerDeath();
        }
        wasAlive = alive;
    }

    private void onPlayerDeath() {
        if (mc.player == null || mc.world == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        String world = mc.world.getRegistryKey().getValue().toString();

        DeathEntry entry = new DeathEntry(x, y, z, world);
        DeathLocationManager.getInstance().addEntry(entry);

        if (showMessage.get()) {
            String msg = String.format("§c[DeathLocation] §fYou died at §e%d %d %d §fin §e%s",
                    (int)x, (int)y, (int)z, world);
            mc.player.sendMessage(Text.literal(msg), false);
        }
    }

    // Kutsutaan asetusnapista
    public void openListWindow() {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        factory.openPopupWindow("deathlocation_list", "Death Locations",
                (sw - 400) / 2, (sh - 300) / 2, 400, 300,
                new DeathLocationListWindow());
    }
}