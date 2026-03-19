package silversword.axiom.client.config;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.util.Properties;

public class ResourcePackBlockerConfig {
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "projectaxiom/resourcepackblocker.properties");
    private static boolean enabled = true;

    public static void load() {
        Properties props = new Properties();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                props.load(reader);
                enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(enabled));
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            props.store(writer, "Resource Pack Blocker Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }
}