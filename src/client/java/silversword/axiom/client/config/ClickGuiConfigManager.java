package silversword.axiom.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ClickGuiConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "axiom_client_clickgui.json";
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    private static ClickGuiConfig config;

    public static boolean isBlurEnabled() { return getConfig().blurEnabled; }
    public static void setBlurEnabled(boolean enabled) { getConfig().blurEnabled = enabled; save(); }

    public static String getThemeName() { return getConfig().themeName; }
    public static void setThemeName(String name) { getConfig().themeName = name; save(); }

    public static int getGlobalAlpha() { return getConfig().globalAlpha; }
    public static void setGlobalAlpha(int alpha) { getConfig().globalAlpha = Math.max(0, Math.min(100, alpha)); save(); }

    public static boolean isRainbowWaveEnabled() { return getConfig().rainbowWaveEnabled; }
    public static void setRainbowWaveEnabled(boolean enabled) { getConfig().rainbowWaveEnabled = enabled; save(); }

    public static float getRainbowWaveSpeed() { return getConfig().rainbowWaveSpeed; }
    public static void setRainbowWaveSpeed(float speed) { getConfig().rainbowWaveSpeed = Math.max(0.1f, Math.min(5.0f, speed)); save(); }

    private static ClickGuiConfig getConfig() {
        if (config == null) load();
        return config;
    }



    private static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, ClickGuiConfig.class);

            } catch (Exception e) { e.printStackTrace(); }
        }
        if (config == null) config = new ClickGuiConfig();
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static class ClickGuiConfig {
        boolean blurEnabled = true;
        String themeName = "Default";
        int globalAlpha = 100;
        boolean rainbowWaveEnabled = false;
        float rainbowWaveSpeed = 1.0f;
    }
}