package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

public final class BlurUtil {
    private static boolean lookedUp = false;
    private static Method blurMethod = null;
    private static Class<?>[] blurSig = null;

    private BlurUtil() {}

    public static boolean tryApplyBlur(Minecraft client, float delta) {
        if (client == null || client.gameRenderer == null) return false;

        if (!lookedUp) {
            lookedUp = true;

            // 1) Yritä tunnetut nimet ensin
            blurMethod = find(client.gameRenderer.getClass(), "renderBlur", float.class);
            blurSig = (blurMethod != null) ? new Class<?>[]{float.class} : null;

            if (blurMethod == null) {
                blurMethod = find(client.gameRenderer.getClass(), "renderBlur");
                blurSig = (blurMethod != null) ? new Class<?>[]{} : null;
            }

            if (blurMethod == null) {
                // 2) Viimeinen yritys: etsi mikä tahansa metodi jossa on "blur"
                for (Method m : client.gameRenderer.getClass().getMethods()) {
                    String n = m.getName().toLowerCase();
                    if (!n.contains("blur")) continue;

                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 0) {
                        blurMethod = m;
                        blurSig = new Class<?>[]{};
                        break;
                    }
                    if (p.length == 1 && p[0] == float.class) {
                        blurMethod = m;
                        blurSig = new Class<?>[]{float.class};
                        break;
                    }
                }
            }

            if (blurMethod != null) {
                blurMethod.setAccessible(true);
                System.out.println("[Axiom] Blur method: " + blurMethod.getName()
                        + " params=" + blurSig.length);
            } else {
                System.out.println("[Axiom] Blur not supported in this version (no suitable GameRenderer blur method).");
            }
        }

        if (blurMethod == null) return false;

        try {
            if (blurSig.length == 0) {
                blurMethod.invoke(client.gameRenderer);
                return true;
            }
            // float signature
            blurMethod.invoke(client.gameRenderer, delta);
            return true;
        } catch (Throwable t) {
            // Älä kaada peliä blurin takia
            return false;
        }
    }

    private static Method find(Class<?> c, String name, Class<?>... params) {
        try {
            return c.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
