package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class AutoSprint extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public AutoSprint() {
        super("Auto Sprint", "Automatically sprints when moving forward", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }



    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Jos pelaaja liikkuu eteenpäin, sprints
        if (player.zza > 0) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.setSprinting(false);

    }
}
