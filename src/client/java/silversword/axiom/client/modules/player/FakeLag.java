package silversword.axiom.client.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import silversword.axiom.client.event.player.AttackEvent;
import silversword.axiom.client.event.player.UseBlockEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.managers.BlinkManager;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.BlockGhostManager;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.PlayerWireframe;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
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

    // Toimintoasetukset (vanhat)
    public final SettingBoolean blinkOnAttack = new SettingBoolean("Blink On Attack", true);
    public final SettingBoolean blinkBlockPlace = new SettingBoolean("Blink Block Place", true);
    public final SettingNumber releaseTicks = new SettingNumber("Release Ticks", 1, 20, 1, 5);

    // UUDET: Suodatusasetukset
    public final SettingBoolean filterMovement = new SettingBoolean("Filter Movement", true);
    public final SettingBoolean filterAttack = new SettingBoolean("Filter Attack", true);
    public final SettingBoolean filterPlacement = new SettingBoolean("Filter Placement", true);
    public final SettingNumber ghostAlpha = new SettingNumber("Ghost Alpha", 50, 255, 10, 100);
    // UUSI: Burst-asetus (paketteja per tick)
    public final SettingNumber burstPacketsPerTick = new SettingNumber("Burst Packets/Tick", 1, 100, 1, 10);

    private long lastReleaseTime = System.currentTimeMillis();
    private long currentDelayLimit = 600;
    private int waitCounter = 0;
    private boolean isWaiting = false;
    private boolean isBursting = false;

    public FakeLag() {
        super("FakeLag", "Uses blink to fake lag", ModuleCategory.PLAYER);

        addSetting(msLimit);
        addSetting(dynamicLag);
        lagRange.setParent(dynamicLag);
        addSetting(lagRange);

        addSetting(distanceCheck);
        maxDistance.setParent(distanceCheck);
        addSetting(maxDistance);

        addSetting(blinkOnAttack);
        addSetting(blinkBlockPlace);
        addSetting(releaseTicks);

        addSetting(filterMovement);
        addSetting(filterAttack);
        addSetting(filterPlacement);
        addSetting(burstPacketsPerTick);
        addSetting(ghostAlpha);

        addHiddenSetting(toggleKey);
    }

    private void applyFilters() {
        BlinkManager.getInstance().setFilters(
                filterMovement.get(),
                filterAttack.get(),
                filterPlacement.get()
        );
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;
        BlinkManager blink = BlinkManager.getInstance();

        // Etäisyystarkistus (kuten aiemmin)
        if (distanceCheck.get() && blink.isBlinking() && blink.getGhostPos() != null) {
            if (mc.player.position().distanceTo(blink.getGhostPos()) > maxDistance.getValue()) {
                forceRelease();
                return;
            }
        }

        // Burst-moodi
        if (isBursting) {
            int maxPerTick = (int) burstPacketsPerTick.getValue();
            int remaining = blink.flush(maxPerTick);
            if (remaining == 0) {
                isBursting = false;
                isWaiting = true;
                waitCounter = 0;
            }
            return;
        }

        // Odotustila ennen uutta lag-sykliä
        if (isWaiting) {
            waitCounter++;
            if (waitCounter >= releaseTicks.getValue()) {
                isWaiting = false;
                waitCounter = 0;
                updateDelay();
                applyFilters();
                float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                blink.start(mc.player, tickDelta);
                lastReleaseTime = System.currentTimeMillis();
            }
        } else {
            if (System.currentTimeMillis() - lastReleaseTime >= currentDelayLimit) {
                isBursting = true;
            }
        }
    }

    private void forceRelease() {
        BlinkManager blink = BlinkManager.getInstance();
        if (isBursting) {
            isBursting = false;
            blink.flush();
        } else {
            blink.stop();
        }
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


    @Subscribe
    public void onBlockPlace(UseBlockEvent event) {
        if (!isEnabled()) return;
        if (!blinkBlockPlace.get()) return;

        BlinkManager blink = BlinkManager.getInstance();
        if (!blink.isBlinking()) return;

        // Haetaan tarkka asetuspositio (mihin blokki oikeasti asetetaan)
        BlockHitResult hit = event.getHitResult();
        BlockPos placedPos = hit.getBlockPos().relative(hit.getDirection());

        // Tarkistetaan, onko kohdepaikka tyhjä tai korvattavissa
        if (mc.level == null) return;
        if (!mc.level.isEmptyBlock(placedPos) && !mc.level.getBlockState(placedPos).canBeReplaced()) {
            return; // Paikassa on jo kiinteä blokki – ei ghostia
        }

        // Estetään päällekkäiset ghostit samassa paikassa
        if (BlockGhostManager.getInstance().isGhost(placedPos)) {
            return; // Ghost jo olemassa
        }

        // Lisätään ghost
        BlockGhostManager.getInstance().addGhost(placedPos);
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (isEnabled() && blinkOnAttack.get()) {
            BlinkManager blink = BlinkManager.getInstance();
            if (isBursting) {
                isBursting = false;
                blink.flush();
            } else {
                blink.stop();
            }
            applyFilters();
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
        Renderer3D renderer = event.getRenderer();

        // Pelaajan ghost (kuten ennenkin)
        if (blink.isBlinking() && blink.getGhostPos() != null && blink.getCapturedModel() != null) {
            PlayerWireframe.render(
                    blink.getCapturedModel(),
                    blink.getGhostPos(),
                    blink.getGhostYaw(),
                    renderer.getCore(),
                    0xFFFFFFFF,
                    0x33FFFFFF,
                    2.0f
            );
        }

        // Block ghostien renderöinti
        if (blink.isBlinking()) {
            int alpha = (int) ghostAlpha.getValue();
            int sideColor = new Color(255, 255, 255, alpha).getARGB();
            int lineColor = new Color(255, 255, 255, 255).getARGB();

            for (BlockPos pos : BlockGhostManager.getInstance().getGhosts().keySet()) {
                double x = pos.getX();
                double y = pos.getY();
                double z = pos.getZ();
                renderer.drawBox(x, y, z, x + 1, y + 1, z + 1,
                        sideColor, lineColor, ShapeModeEnum.BOTH, 0);
            }
        }

        event.getRenderer().flush();
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }
}