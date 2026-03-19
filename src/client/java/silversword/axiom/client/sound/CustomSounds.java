
package silversword.axiom.client.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import silversword.axiom.ProjectAxiom;


public class CustomSounds {

    public static final SoundEvent OOF = registerSound("oof-minecraft");

    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.of(ProjectAxiom.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }
    public static void initialize() {
        System.out.println("Registering custom sounds!");
    }
}