package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.gui.components.TutorialDialog;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class PhaseWBoat extends AxiomMod implements KeybindConfigurable {

    // ===== TUTORIAL TEXT =====
    private static final String TUTORIAL_TEXT =
                    "Before you do anything:\n" +
                    "1. Go to Flight -module's setting and set the VERTICAL speed to 0.5, Mode to Packet and Anti-Kick to Dolphin.\n\n" +
                    "Ensure you changed those settings!\n\n" +
                    "Enable this while on a boat. Exit the clickgui and hold space. When you have reached the height you wanted - press shift.";
    // =========================

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingBoolean tutorialDisabled = new SettingBoolean("TutorialDisabled", false);

    private Flight flight;
    private BoatPhase boatPhase;

    private int phaseDelay = 40;       // ticks until BoatPhase activates
    private int flightDelay = 0;       // ticks until Flight deactivates
    private boolean shiftPressed = false;
    private boolean phaseActivated = false;
    private boolean waitingForTutorial = false; // to avoid multiple windows

    public PhaseWBoat() {
        super("PhaseWBoat", "Starts Flight, then BoatPhase, and Shift to exit.", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
        addHiddenSetting(tutorialDisabled);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        flight = ModuleManager.getInstance().getModule(Flight.class);
        boatPhase = ModuleManager.getInstance().getModule(BoatPhase.class);

        if (flight == null || boatPhase == null || mc.player == null) {
            toggle(); // disable if modules missing or player null
            return;
        }

        // Show tutorial if not disabled (and not already waiting)
        if (!tutorialDisabled.get() && !waitingForTutorial) {
            showTutorial();
            waitingForTutorial = true;
            return; // wait for tutorial to finish before actually enabling
        }

        // Proceed with actual enable only if in a boat
        if (!(mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat)) {
            toggle();
            return;
        }

        startModule();
    }

    private void showTutorial() {
        TutorialDialog tutorial = new TutorialDialog(
                "PhaseWBoat",
                TUTORIAL_TEXT,
                () -> {
                    // OK clicked: close the GUI and start the module (if in a boat)
                    mc.setScreen(null); // close any open screen (clickgui)

                    // Check again if the player is in a boat – if not, disable the module
                    if (!(mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat)) {
                        setEnabled(false);
                    } else {
                        startModule();
                    }
                    waitingForTutorial = false;
                },
                (dontShow) -> {
                    // Update the setting when checkbox changes
                    tutorialDisabled.set(dontShow);
                }
        );

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory != null) {
            // Open a larger window (500x300)
            factory.openPopupWindow(
                    "boat_phase_tutorial",
                    "Tutorial",
                    (screenW - 850) / 2,
                    (screenH - 300) / 2,
                    850,
                    300,
                    tutorial
            );
        } else {
            // Fallback: just start without tutorial
            startModule();
            waitingForTutorial = false;
        }
    }

    private void startModule() {
        // Enable Flight first (assume it is already configured for Packet + Dolphin)
        if (!flight.isEnabled()) {
            flight.setEnabled(true);
        }

        phaseDelay = 1; // 2 seconds (20 ticks per second)
        flightDelay = 0;
        shiftPressed = false;
        phaseActivated = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            disableAll();
            return;
        }

        // If the player is no longer in a boat, clean up
        if (!(mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat)) {
            disableAll();
            return;
        }

        // If we are still waiting for tutorial, do nothing else
        if (waitingForTutorial) return;

        // --- Phase activation timer ---
        if (!phaseActivated && phaseDelay > 0) {
            phaseDelay--;
            if (phaseDelay == 0) {
                // Enable BoatPhase
                if (!boatPhase.isEnabled()) {
                    boatPhase.setEnabled(true);
                }
                phaseActivated = true;
            }
        }

        // --- Shift detection ---
        boolean shiftDown = mc.options.keyShift.isDown();
        if (shiftDown && !shiftPressed && phaseActivated) {
            shiftPressed = true;

            // Disable BoatPhase immediately
            if (boatPhase.isEnabled()) {
                boatPhase.setEnabled(false);
            }

            // Start 1‑second timer to disable Flight
            flightDelay = 1; // 1 second
        }

        // --- Flight deactivation timer ---
        if (flightDelay > 0) {
            flightDelay--;
            if (flightDelay == 0) {
                // Disable Flight and then this module
                if (flight.isEnabled()) {
                    flight.setEnabled(false);
                }
                setEnabled(false);
                return;
            }
        }
    }

    @Override
    protected void onDisable() {
        disableAll();
        waitingForTutorial = false; // reset flag
    }

    private void disableAll() {
        if (flight != null && flight.isEnabled()) {
            flight.setEnabled(false);
        }
        if (boatPhase != null && boatPhase.isEnabled()) {
            boatPhase.setEnabled(false);
        }
        phaseActivated = false;
        shiftPressed = false;
    }
}