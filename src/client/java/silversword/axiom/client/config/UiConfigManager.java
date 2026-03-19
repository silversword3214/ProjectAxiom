package silversword.axiom.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import silversword.axiom.client.gui.components.ModuleListView;
import silversword.axiom.client.gui.window.ModuleWindowData;
import silversword.axiom.client.gui.window.Window;
import silversword.axiom.client.gui.window.WindowIds;
import silversword.axiom.client.gui.window.WindowManager;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class UiConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int FORMAT_VERSION = 2;
    private static final String FILE_NAME_UI = "axiom_client_ui.json";

    private UiConfigManager() {}

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME_UI);
    }

    // =========================
    // SAVE GUI
    // =========================
    public static void saveGui(WindowManager wm) {
        try {
            Path path = getPath();
            if (path.getParent() != null) Files.createDirectories(path.getParent());

            UiData data = loadOrCreate();
            data.version = FORMAT_VERSION;

            data.gui = new GuiData();
            data.gui.windows = new ArrayList<>();

            for (Window w : wm.getWindows()) {

                if (w.id != null && w.id.startsWith("settings:")) continue;

                WindowData wd = new WindowData();
                wd.id = w.id;
                wd.title = w.getTitle();
                wd.x = w.x;
                wd.y = w.y;
                wd.w = w.width;
                wd.h = w.height;
                wd.minimized = w.isMinimized();

                if (w.getUserData() instanceof ModuleWindowData mwd) {
                    wd.modules = new ArrayList<>(mwd.moduleIds);
                }

                data.gui.windows.add(wd);
            }

            Files.writeString(path, GSON.toJson(data));
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to save UI state");
            t.printStackTrace();
        }
    }

    // =========================
    // LOAD EXISTING WINDOW LAYOUT (for windows that already exist)
    // =========================
    public static boolean loadGui(WindowManager wm) {
        try {
            UiData data = loadOrCreate();
            if (data.gui == null || data.gui.windows == null || data.gui.windows.isEmpty()) return false;

            for (WindowData ws : data.gui.windows) {
                if (ws == null || ws.id == null) continue;

                Window w = wm.getWindowById(ws.id);
                if (w == null) continue;

                w.x = ws.x;
                w.y = ws.y;
                w.width = Math.max(140, ws.w);
                w.height = Math.max(60, ws.h);
                if (ws.title != null) w.setTitle(ws.title);

                if (ws.minimized != w.isMinimized()) {
                    w.setMinimized(ws.minimized);
                }
            }

            return true;
        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to load UI state");
            t.printStackTrace();
            return false;
        }
    }

    // =========================
    // LOAD WINDOWS FROM FILE (create missing subwindows + restore module lists)
    // Call this in ClickGui init AFTER MAIN exists.
    // =========================
    public static boolean loadWindows(
            WindowManager wm,
            int screenW,
            int screenH,
            java.util.function.Consumer<AxiomMod> onOpenSettings
    ) {
        try {
            UiData data = loadOrCreate();
            if (data.gui == null || data.gui.windows == null || data.gui.windows.isEmpty()) return false;

            for (WindowData wd : data.gui.windows) {
                if (wd == null || wd.id == null) continue;

                // MAIN is created by ClickGUI init
                if (WindowIds.MAIN.equals(wd.id) || "main".equals(wd.id)) continue;

                // Skip transient settings windows
                if (wd.id.startsWith("settings:")) continue;

                // Create missing window
                if (wm.getWindowById(wd.id) != null) continue;

                int w = Math.max(140, wd.w);
                int h = Math.max(60, wd.h);

                int x = Math.max(10, Math.min(wd.x, Math.max(10, screenW - w - 10)));
                int y = Math.max(10, Math.min(wd.y, Math.max(10, screenH - h - 10)));

                Window win = new Window(wd.id, wd.title == null ? "Window" : wd.title, x, y, w, h);

                win.setOnClose(() -> wm.removeWindow(win));

                ModuleWindowData mwd = new ModuleWindowData();
                if (wd.modules != null) mwd.moduleIds.addAll(wd.modules);
                win.setUserData(mwd);

                ModuleManager mm = ModuleManager.getInstance();
                ModuleListView list = new ModuleListView(
                        win.id,
                        () -> {
                            List<AxiomMod> out = new ArrayList<>();
                            for (String mid : mwd.moduleIds) {
                                AxiomMod mod = mm.getById(mid);
                                if (mod != null) out.add(mod);
                            }
                            return out;
                        },
                        () -> "",
                        () -> "All",
                        onOpenSettings,                   // ✅ EI ENÄÄ tyhjää
                        moduleId -> mwd.remove(moduleId)
                );

                win.clearChildren();
                win.add(list);

                wm.add(win);

                if (wd.minimized) win.setMinimized(true);


            }
            wm.applyWindowDefaults();


            return true;

        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to load windows");
            t.printStackTrace();
            return false;
        }
    }



    // =========================
    // Internal load helper
    // =========================
    private static UiData loadOrCreate() {
        Path path = getPath();
        try {
            if (!Files.exists(path)) return new UiData();
            String json = Files.readString(path).trim();
            if (json.isEmpty()) return new UiData();

            UiData data = GSON.fromJson(json, UiData.class);
            return data == null ? new UiData() : data;

        } catch (Throwable t) {
            t.printStackTrace();
            return new UiData();
        }
    }

    // =========================
    // Data model
    // =========================
    private static final class UiData {
        int version = FORMAT_VERSION;
        GuiData gui = new GuiData();
    }

    private static final class GuiData {
        List<WindowData> windows = new ArrayList<>();
    }

    private static final class WindowData {
        String id;
        String title;
        int x, y, w, h;
        boolean minimized;

        // ✅ module window content
        List<String> modules;
    }
}
