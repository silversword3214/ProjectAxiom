package silversword.axiom.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import silversword.axiom.client.gui.window.Window;
import silversword.axiom.client.gui.window.WindowManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PauseUiConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FORMAT_VERSION = 1;
    private static final String FILE_NAME = "axiom_client_pause_ui.json";

    private PauseUiConfigManager() {}

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void save(WindowManager wm) {
        try {
            Path path = getPath();
            if (path.getParent() != null) Files.createDirectories(path.getParent());

            PauseUiData data = new PauseUiData();
            data.version = FORMAT_VERSION;
            data.windows = new ArrayList<>();

            for (Window w : wm.getWindows()) {
                WindowData wd = new WindowData();
                wd.id = w.id;
                wd.x = w.x;
                wd.y = w.y;
                wd.w = w.width;
                wd.h = w.height;
                wd.minimized = w.isMinimized();
                data.windows.add(wd);
            }

            Files.writeString(path, GSON.toJson(data));
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to save pause UI state");
            t.printStackTrace();
        }
    }

    public static boolean load(WindowManager wm) {
        try {
            Path path = getPath();
            if (!Files.exists(path)) return false;

            String json = Files.readString(path).trim();
            if (json.isEmpty()) return false;

            PauseUiData data = GSON.fromJson(json, PauseUiData.class);
            if (data == null || data.windows == null) return false;

            for (WindowData wd : data.windows) {
                if (wd.id == null) continue;
                Window w = wm.getWindowById(wd.id);
                if (w == null) continue;

                w.x = wd.x;
                w.y = wd.y;
                w.width = Math.max(100, wd.w);
                w.height = Math.max(60, wd.h);
                if (wd.minimized) w.setMinimized(true);
            }
            return true;
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to load pause UI state");
            t.printStackTrace();
            return false;
        }
    }

    private static class PauseUiData {
        int version = FORMAT_VERSION;
        List<WindowData> windows;
    }

    private static class WindowData {
        String id;
        int x, y, w, h;
        boolean minimized;
    }
}