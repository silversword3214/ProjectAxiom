package silversword.axiom.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import silversword.axiom.client.render.font.Fonts;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FontConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "font_settings.json";

    private FontConfigManager() {}

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void saveFont(String fontName) {
        try {
            Path path = getPath();
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            FontData data = new FontData();
            data.fontName = fontName;
            Files.writeString(path, GSON.toJson(data));
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to save font setting");
            t.printStackTrace();
        }
    }

    public static String loadFont() {
        Path path = getPath();
        try {
            if (!Files.exists(path)) return null;
            String json = Files.readString(path).trim();
            if (json.isEmpty()) return null;
            FontData data = GSON.fromJson(json, FontData.class);
            return data != null ? data.fontName : null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static class FontData {
        String fontName;
    }
}