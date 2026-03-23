package silversword.axiom.client.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import silversword.axiom.ProjectAxiom;


public class CustomSounds {

    public static final SoundEvent OOF = registerSound("oof-minecraft");

    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.fromNamespaceAndPath(ProjectAxiom.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }
    public static void initialize() {
        System.out.println("Registering custom sounds!");
    }
}