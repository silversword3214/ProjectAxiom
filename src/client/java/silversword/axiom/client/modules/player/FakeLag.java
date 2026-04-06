package silversword.axiom.client.modules.player;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.event.player.AttackEvent;
import silversword.axiom.client.event.player.UseBlockEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.managers.BlinkManager;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.PlayerWireframe;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingRangeSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class FakeLag extends AxiomMod implements KeybindConfigurable {
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Viiveasetukset
    public final SettingNumber msLimit = new SettingNumber("MS Delay", 50, 5000, 50, 600);
    public final SettingBoolean dynamicLag = new SettingBoolean("Dynamic Delay", false);
    public final SettingRangeSlider lagRange = new SettingRangeSlider("Lag Range", 200, 1000, 50, 5000, 50);

    // Etäisyysasetukset
    public final SettingBoolean distanceCheck = new SettingBoolean("Distance Limit", false);
    public final SettingNumber maxDistance = new SettingNumber("Max Distance", 1, 10, 0.5, 6);

    // Toimintoasetukset
    public final SettingBoolean blinkOnAttack = new SettingBoolean("Blink On Attack", true);
    public final SettingBoolean blinkBlockPlace = new SettingBoolean("Blink Block Place", true);
    public final SettingNumber releaseTicks = new SettingNumber("Release Ticks", 1, 20, 1, 5);

    private long lastReleaseTime = System.currentTimeMillis();
    private long currentDelayLimit = 600;
    private int waitCounter = 0;
    private boolean isWaiting = false;

    public FakeLag() {
        super("FakeLag", "Advanced periodic lag", ModuleCategory.PLAYER);

        addSetting(msLimit);
        addSetting(dynamicLag);

        // Parent-logiikka: piilotetaan/näytetään dynaamisuuden mukaan
        lagRange.setParent(dynamicLag);
        addSetting(lagRange);

        // Distance parent-logiikka
        maxDistance.setParent(distanceCheck);
        addSetting(distanceCheck);
        addSetting(maxDistance);

        addSetting(blinkOnAttack);
        addSetting(blinkBlockPlace);
        addSetting(releaseTicks);
        addHiddenSetting(toggleKey);
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;
        BlinkManager blink = BlinkManager.getInstance();

        // TARKISTUS: Jos etäisyys kasvaa liian suureksi, pakotetaan vapautus
        if (distanceCheck.get() && blink.isBlinking() && blink.getGhostPos() != null) {
            if (mc.player.position().distanceTo(blink.getGhostPos()) > maxDistance.getValue()) {
                forceRelease();
                return;
            }
        }

        if (isWaiting) {
            waitCounter++;
            if (waitCounter >= releaseTicks.getValue()) {
                isWaiting = false;
                waitCounter = 0;
                updateDelay();
                float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                blink.start(mc.player, tickDelta);
                lastReleaseTime = System.currentTimeMillis();
            }
        } else {
            if (System.currentTimeMillis() - lastReleaseTime >= currentDelayLimit) {
                forceRelease();
            }
        }
    }

    private void forceRelease() {
        BlinkManager.getInstance().stop();
        isWaiting = true;
        waitCounter = 0;
    }

    private void updateDelay() {
        if (dynamicLag.get()) {
            double min = lagRange.getMin();
            double max = lagRange.getMax();
            currentDelayLimit = (long) (min + Math.random() * (max - min));
        } else {
            currentDelayLimit = (long) msLimit.getValue();
        }
    }

    // Tuki block placelle
    @Subscribe
    public void onBlockPlace(UseBlockEvent event) { // Varmista Eventin nimi
        if (isEnabled() && blinkBlockPlace.get()) {
            // Jos haluat, että blokit tulevat viiveellä, varmista että BlinkManager käsittelee ne.
            // Jos haluat ne HETI, kutsu tässä forceRelease().
        }
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (isEnabled() && blinkOnAttack.get()) {
            BlinkManager blink = BlinkManager.getInstance();
            blink.stop();
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            blink.start(mc.player, tickDelta);
            lastReleaseTime = System.currentTimeMillis();
            isWaiting = false;
            waitCounter = 0;
            updateDelay();
        }
    }



    @Subscribe
    public void onRender3D(Render3DEvent event) {
        BlinkManager blink = BlinkManager.getInstance();
        if (blink.isBlinking() && blink.getGhostPos() != null && blink.getCapturedModel() != null) {
            PlayerWireframe.render(
                    blink.getCapturedModel(),
                    blink.getGhostPos(),
                    blink.getGhostYaw(),
                    event.getRenderer().getCore(),
                    0xFFFFFFFF,
                    0x33FFFFFF,
                    2.0f
            );
            event.getRenderer().flush();
        }
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }
}