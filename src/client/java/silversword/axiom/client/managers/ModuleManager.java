package silversword.axiom.client.managers;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.combat.*;
import silversword.axiom.client.modules.hidden.Keybinds;
import silversword.axiom.client.modules.movement.*;
import silversword.axiom.client.modules.player.*;
import silversword.axiom.client.modules.render.*;
import silversword.axiom.client.modules.misc.*;
import silversword.axiom.client.modules.utility.BedFucker;
import silversword.axiom.client.modules.utility.XCarry;
import silversword.axiom.client.modules.world.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {

    private static ModuleManager instance;

    public static ModuleManager getInstance() {
        if (instance == null) instance = new ModuleManager();
        return instance;
    }

    private final List<AxiomMod> modules = new ArrayList<>();
    private boolean initialized = false;

    private ModuleManager() {}

    public void init() {
        if (initialized) return;
        initialized = true;

        modules.clear();
        registerModules();
    }

    public AxiomMod getModule(String name) {
        return getByName(name);
    }

    public AxiomMod getById(String id) {
        if (id == null || id.isEmpty()) return null;

        for (AxiomMod m : getModules()) { // tai modules-lista suoraan, riippuen sun koodista
            if (m == null) continue;
            if (id.equals(m.getId())) return m;
        }
        return null;
    }


    public <T extends AxiomMod> T getModule(Class<T> clazz) {
        for (AxiomMod m : modules) {
            if (clazz.isInstance(m)) return clazz.cast(m);
        }
        return null;
    }


    /**
     * Add all modules here.
     * This is the ONLY place you should have "new SomeModule()".
     */
    // This list is useful when I want to arrange the modules how I want
    private void registerModules() {
        // Combat
        add(new BetterMace());
        add(new KillAura());
        add(new CrystalAura());
        add(new MultiAura());
        add(new TPAura());
        add(new TriggerBot());
        add(new Criticals());
        add(new Reach());
        add(new ShieldBreaker());
        add(new AimAssist());
        add(new NoHitDelay());
        add(new PotionRefill());
        add(new AutoTotem());
        add(new AutoGapple());
        add(new MiddleClickPearl());
        add(new HitEffect());
        add(new Velocity());
        add(new MaceDmg());

        // Movement
        add(new Flight());
        add(new BoatFly());
        add(new BoatPhase());
        add(new PhaseWBoat());
        add(new Speed());
        add(new HighJump());
        add(new LongJump());
        add(new AirJump());
        add(new SafeWalk());
        add(new AutoSprint());
        add(new AutoWalk());
        add(new Step());
        add(new Slippy());
        add(new IceSpeed());
        add(new FastLadder());
        add(new Jesus());
        add(new Spider());
        add(new Phase());
        add(new LeatherBoots());
        add(new ClickTP());
        add(new GodBridge());
        add(new Blink());
        add(new SlowDown());
        add(new NoSlow());

        // Player
        add(new NoFall());
        add(new GhostHand());
        add(new AutoTool());
        add(new AirPlace());
        add(new AutoArmor());
        add(new ChestStealer());
        add(new FakeLag());
        add(new FakePlayer());
        add(new AntiHunger());
        add(new AntiCactus());

        // Render
        add(new WaypointModule());
        add(new ESP());
        add(new ChestESP());
        add(new CaveESP());
        add(new LavaESP());
        add(new NameTags());
        add(new SearchBlocks());
        add(new Tracers());
        add(new Trajectories());
        add(new XRay());
        add(new BlockOutline());
        add(new BlockNametag());
        add(new RadarModule());
        add(new CameraDistance());
        add(new CameraClip());
        add(new NoFog());
        add(new Fullbright());
        add(new NightVision());
        add(new Zoom());
        add(new Freecam());
        add(new ChunkBorders());
        add(new Breadcrumbs());
        add(new NoHurtCam());
        add(new LowerShield());
        add(new NoParticleModule());
        add(new NoOverlay());
        add(new AntiBlind());
        add(new NoVignette());
        add(new NoViewBobbingTilt());

        // Misc
        add(new InvWalk());
        add(new TargetHUD());
        add(new PlayerINFO());
        add(new ArmorHudModule());
        add(new PotionEffects());
        add(new AutoFish());
        add(new AutoFarm());
        add(new AutoMine());
        add(new DeathLocationModule());
        add(new AutoClicker());
        add(new PingSpoof());

        // Utility
        add(new BedFucker());
        add(new XCarry());

        // World
        add(new FastBreak());
        add(new FastPlace());
        add(new WeatherChanger());
        add(new TimeChanger());

        // Hidden
        add(new Keybinds());
    }


    private void add(AxiomMod mod) {
        if (mod == null) return;

        for (AxiomMod m : modules) {
            if (m.getName().equalsIgnoreCase(mod.getName())) {
                System.err.println("[Axiom] Duplicate module name: " + mod.getName());
                return;
            }
        }
        modules.add(mod);
    }


    public List<AxiomMod> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public AxiomMod getByName(String name) {
        for (AxiomMod m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public void updateAllModules() {
        for (AxiomMod m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.update();
            } catch (Throwable t) {
                System.err.println("[Axiom] Module crashed: " + m.getName());
                t.printStackTrace();
            }
        }
    }

}
