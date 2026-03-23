package silversword.axiom.client.modules.render;

import net.minecraft.world.level.LightLayer;

public final class FullbrightState {
    private FullbrightState() {}

    public static volatile boolean enabled = false;


    public static volatile int minimumLight = 15;


    public static volatile LightLayer type = null;
}
