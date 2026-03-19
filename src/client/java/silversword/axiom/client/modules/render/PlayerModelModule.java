package silversword.axiom.client.modules.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.render.PlayerModelHud;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

public final class PlayerModelModule extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final String HUD_ID = "PlayerModel";
    private PlayerModelHud hud;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // ------------------- Asetukset -------------------
    public final SettingNumber width;
    public final SettingNumber height;
    public final SettingNumber fov;
    public final SettingNumber cameraHeight;
    public final SettingNumber cameraDistance;
    public final SettingNumber cameraSide;
    public final SettingNumber renderDistance;
    public final SettingBoolean showBorder;
    public final SettingColor borderColor;

    public final SettingMode cameraMode;        // BEHIND, FRONT, LEFT, RIGHT
    public final SettingMode lookAtMode;        // PLAYER, FORWARD, BACKWARD

    public final SettingKeybind toggleKey;

    public PlayerModelModule() {
        super("Player Model", "Shows your player model in a HUD element with framebuffer (very experimental)", ModuleCategory.RENDER);

        width           = new SettingNumber("Width", 32, 400, 1, 80);
        height          = new SettingNumber("Height", 32, 400, 1, 160);
        fov             = new SettingNumber("FOV", 30, 120, 1, 70);
        cameraHeight    = new SettingNumber("Camera Height", -10, 10, 0.5, 1.5);
        cameraDistance  = new SettingNumber("Camera Distance", 1, 20, 0.5, 4.0);
        cameraSide      = new SettingNumber("Camera Side", -5, 5, 0.5, 0.0);
        renderDistance  = new SettingNumber("Render Distance", 16, 512, 8, 64);
        showBorder      = new SettingBoolean("Show Border", true);
        borderColor     = new SettingColor("Border Color", new Color(255, 255, 255, 255));

        cameraMode      = new SettingMode("Camera Position", new String[]{"BEHIND", "FRONT", "LEFT", "RIGHT"}, "BEHIND");
        lookAtMode      = new SettingMode("Look At", new String[]{"PLAYER", "FORWARD", "BACKWARD"}, "PLAYER");

        toggleKey       = new SettingKeybind("Toggle Key", 0);

        addHiddenSetting(borderColor.getSetting());
        addHiddenSetting(toggleKey);

        addSetting(width);
        addSetting(height);
        addSetting(fov);
        addSetting(cameraHeight);
        addSetting(cameraDistance);
        addSetting(cameraSide);
        addSetting(renderDistance);
        addSetting(showBorder);
        addSetting(cameraMode);
        addSetting(lookAtMode);

        ensureHudRegistered();
        if (hud != null) {
            hud.setEnabled(this.isEnabled());
        }
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        if (hud != null) hud.setEnabled(true);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    @Override
    protected void onDisable() {
        if (hud != null) hud.setEnabled(false);
        // Tapahtuman poistaminen jätetään – moduuli ei ole päällä, ei haittaa
    }

    private void onClientTick(MinecraftClient client) {
        if (hud != null && hud.enabled() && client.world != null && client.player != null) {
            hud.renderWorldToFramebuffer(client.getRenderTickCounter());
        }
    }

    @Override
    protected void onTick() {
        if (hud == null) return;

        hud.setWidth((int) width.getValue());
        hud.setHeight((int) height.getValue());
        hud.setFov((float) fov.getValue());
        hud.setCameraHeight((float) cameraHeight.getValue());
        hud.setCameraDistance((float) cameraDistance.getValue());
        hud.setCameraSide((float) cameraSide.getValue());
        hud.setRenderDistance((float) renderDistance.getValue());
        hud.setShowBorder(showBorder.get());
        hud.setBorderColor(borderColor.getCurrentColor().getPacked());

        hud.setCameraMode(cameraMode.getMode());
        hud.setLookAtMode(lookAtMode.getMode());
    }

    private void ensureHudRegistered() {
        if (hud == null) {
            for (HudElement e : HudManager.get().elements()) {
                if (HUD_ID.equals(e.id()) && e instanceof PlayerModelHud) {
                    hud = (PlayerModelHud) e;
                    return;
                }
            }
            hud = new PlayerModelHud(this);
            HudManager.get().register(hud);
        }
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Border", borderColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("playermodel_color", "Player Model Color Customizer", sw, sh, content);
    }
}