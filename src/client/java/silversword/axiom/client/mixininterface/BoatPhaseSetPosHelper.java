package silversword.axiom.client.mixininterface;

import java.util.ArrayList;
import java.util.List;

public class BoatPhaseSetPosHelper {
    private static final ThreadLocal<Boolean> ALLOW_SET_POS = ThreadLocal.withInitial(() -> false);

    public static void allowSetPos(boolean allow) {
        ALLOW_SET_POS.set(allow);
    }

    public static boolean isSetPosAllowed() {
        return ALLOW_SET_POS.get();
    }
}