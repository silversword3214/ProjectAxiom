package silversword.axiom.client.modules.moduleutils;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

public class InteractItemEvent {
    private static final InteractItemEvent INSTANCE = new InteractItemEvent();

    public InteractionHand hand;
    public InteractionResult toReturn;

    public static InteractItemEvent get(InteractionHand hand) {
        INSTANCE.hand = hand;
        INSTANCE.toReturn = null;
        return INSTANCE;
    }
}