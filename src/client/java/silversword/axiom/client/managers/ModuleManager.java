package silversword.axiom.client.managers;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.combat.*;
import silversword.axiom.client.modules.hidden.Keybinds;
import silversword.axiom.client.modules.movement.*;
import silversword.axiom.client.modules.player.*;
import silversword.axiom.client.modules.render.*;
import silversword.axiom.client.modules.misc.*;
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

    private void registerModules() {
        // Combat
        safeAdd(new BetterMace());
        safeAdd(new Velocity());
        safeAdd(new KillAura());
        safeAdd(new CrystalAura());
        safeAdd(new MultiAura());
        safeAdd(new TPAura());
        safeAdd(new TriggerBot());
        safeAdd(new Criticals());
        safeAdd(new Reach());
        safeAdd(new ShieldBreaker());
        safeAdd(new Aimbot());
        safeAdd(new NoHitDelay());
        safeAdd(new AutoTotem());
        safeAdd(new AutoGapple());
        safeAdd(new HitEffect());
        safeAdd(new Velocity());
        safeAdd(new ReachDisplay());

        // Movement
        safeAdd(new Flight());
        safeAdd(new BoatFly());
        safeAdd(new BoatPhase());
        safeAdd(new PhaseWBoat());
        safeAdd(new Speed());
        safeAdd(new AirJump());
        safeAdd(new AutoSprint());
        safeAdd(new Step());
        safeAdd(new Jesus());
        safeAdd(new Spider());
        safeAdd(new LeatherBoots());
        safeAdd(new ClickTP());
        safeAdd(new Scaffold());
        safeAdd(new Blink());
        safeAdd(new NoSlow());

        // Player
        safeAdd(new NoFall());
        safeAdd(new GhostHand());
        safeAdd(new AutoTool());
        safeAdd(new AirPlace());
        safeAdd(new AutoArmor());
        safeAdd(new ChestStealer());
        safeAdd(new FakePlayer());

        // Render
        safeAdd(new WaypointModule());
        safeAdd(new ESP());
        safeAdd(new ChestESP());
        safeAdd(new CaveESP());
        safeAdd(new LavaESP());
        safeAdd(new NameTags());
        safeAdd(new SearchBlocks());
        safeAdd(new Tracers());
        safeAdd(new Trajectories());
        safeAdd(new XRay());
        safeAdd(new BlockOutline());
        safeAdd(new BlockNametag());
        safeAdd(new RadarModule());
        safeAdd(new CameraDistance());
        safeAdd(new CameraClip());
        safeAdd(new NoFog());
        safeAdd(new Fullbright());
        safeAdd(new NightVision());
        safeAdd(new Zoom());
        safeAdd(new Freecam());
        safeAdd(new ChunkBorders());
        safeAdd(new Breadcrumbs());
        safeAdd(new NoHurtCam());
        safeAdd(new LowerShield());
        safeAdd(new NoParticleModule());
        safeAdd(new NoOverlay());
        safeAdd(new AntiBlind());
        safeAdd(new NoVignette());
        safeAdd(new NoViewBobbingTilt());

        // Misc
        safeAdd(new InvWalk());
        safeAdd(new TargetHUD());
        safeAdd(new PlayerINFO());
        safeAdd(new ArmorHudModule());
        safeAdd(new PotionEffects());
        safeAdd(new DeathLocationModule());
        safeAdd(new TunnelMiner());
        safeAdd(new AutoClicker());
        safeAdd(new PingSpoof());

        // World
        safeAdd(new FastBreak());
        safeAdd(new FastPlace());
        safeAdd(new WeatherChanger());
        safeAdd(new TimeChanger());

        // Hidden
        safeAdd(new Keybinds());
    }


    private void safeAdd(AxiomMod mod) {
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
