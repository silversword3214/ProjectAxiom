package silversword.axiom.client.gui.window;

import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.setting.Setting;
import silversword.axiom.client.setting.SettingKeybind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WindowFactory {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "All", "Enabled", "Movement", "Combat", "Render", "Player", "World", "Misc"
    );

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final WindowManager windowManager;

    public WindowFactory(WindowManager windowManager) {
        this.windowManager = Objects.requireNonNull(windowManager);

        // Keep ids unique across sessions.
        int max = 0;
        for (Window w : windowManager.getWindows()) {
            if (w == null || w.id == null) continue;
            if (!w.id.startsWith("win:")) continue;
            try {
                int n = Integer.parseInt(w.id.substring("win:".length()));
                if (n > max) max = n;
            } catch (NumberFormatException ignored) {}
        }
        int next = Math.max(1, max + 1);
        COUNTER.updateAndGet(cur -> Math.max(cur, next));
    }

    private void centerWindow(Window win, int screenW, int screenH) {
        win.x = (screenW - win.width) / 2;
        win.y = (screenH - win.height) / 2;
    }

    public Window ensureMainWindow(
            int screenW, int screenH,
            Supplier<String> searchText,
            Consumer<String> setSearchText,
            Supplier<String> selectedCategory,
            Consumer<String> setSelectedCategory,
            Supplier<List<AxiomMod>> modulesSupplier,
            Consumer<AxiomMod> onOpenSettings
    ) {
        Window existing = windowManager.getWindowById(WindowIds.MAIN);
        if (existing != null) {
            windowManager.bringToFront(existing);
            return existing;
        }

        int w = 420;
        int h = 300;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        Window main = new Window(WindowIds.MAIN, "", x, y, w, h);
        main.setClosable(false);

        CategoryTabs tabs = new CategoryTabs(
                DEFAULT_CATEGORIES,
                selectedCategory,
                setSelectedCategory
        );

        SearchBar search = new SearchBar(searchText, setSearchText);

        ActionButton createWindowBtn =
                new ActionButton("Create Window", () -> createNewWindow(screenW, screenH));

        ModuleListView list = new ModuleListView(
                WindowIds.MAIN,
                modulesSupplier,
                searchText,
                selectedCategory,
                onOpenSettings
        );

        main.clearChildren();
        main.add(tabs);
        main.add(search);
        main.add(createWindowBtn);
        main.add(list);

        windowManager.add(main);
        windowManager.bringToFront(main);
        main.open();
        return main;
    }

    public Window createNewWindow(int screenW, int screenH) {
        int n = COUNTER.getAndIncrement();

        String id = "win:" + n;
        String title = "Window " + n;

        int w = 320;
        int h = 240;

        int x = 60 + (n * 18);
        int y = 60 + (n * 18);

        x = Math.max(10, Math.min(x, Math.max(10, screenW - w - 10)));
        y = Math.max(10, Math.min(y, Math.max(10, screenH - h - 10)));

        Window win = new Window(id, title, x, y, w, h);
        win.setClosable(false);
        win.setOnClose(() -> win.close());

        ModuleWindowData data = new ModuleWindowData();
        win.setUserData(data);

        Supplier<List<AxiomMod>> supplier = () -> {
            List<AxiomMod> out = new ArrayList<>();
            ModuleManager mm = ModuleManager.getInstance();
            for (String mid : data.moduleIds) {
                AxiomMod m = mm.getById(mid);
                if (m != null) out.add(m);
            }
            return out;
        };

        ModuleListView list = new ModuleListView(
                id,
                supplier,
                () -> "",
                () -> "All",
                mod -> openSettingsWindow(mod, screenW, screenH),
                moduleId -> data.remove(moduleId)
        );

        win.clearChildren();
        win.add(list);

        windowManager.add(win);
        windowManager.bringToFront(win);
        win.open();
        return win;
    }

    public Window openSettingsWindow(AxiomMod module, int screenW, int screenH) {
        if (module == null) return null;

        String id = "settings:" + module.getId();
        Window existing = windowManager.getWindowById(id);
        if (existing != null) {
            // Keskittäminen ennen näyttämistä
            centerWindow(existing, screenW, screenH);
            windowManager.openOverlay(existing);
            return existing;
        }

        int w = 300;
        int h = 220;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        Window win = new Window(id, module.getName() + " Settings", x, y, w, h);
        win.setMinimizable(false);
        win.setClosable(false);

        List<SettingKeybind> keybinds = new ArrayList<>();
        for (Setting setting : module.getAllSettings()) {
            if (setting instanceof SettingKeybind kb) {
                keybinds.add(kb);
            }
        }
        if (!keybinds.isEmpty()) {
            win.setHasKeybind(true);
            win.setOnKeybindClick(() -> {
                UiComponent editor = new KeybindEditor(keybinds);
                openCustomWindow("keybind_" + module.getId(), "Keybind Editor", screenW, screenH, editor);
            });
        }

        win.setOnClose(() -> win.close());

        win.clearChildren();
        win.add(new ModuleSettingsView(module));

        windowManager.add(win);
        windowManager.openOverlay(win);
        win.open();
        return win;
    }

    public Window openPopupWindow(String windowId, String title, int x, int y, int width, int height, UiComponent content) {
        Window existing = windowManager.getWindowById(windowId);
        if (existing != null) {
            windowManager.removeWindow(existing);
        }

        Window win = new Window(windowId, title, x, y, width, height);
        win.setMinimizable(false);
        win.setClosable(false);
        win.setOnClose(() -> win.close());
        win.clearChildren();
        win.add(content);
        windowManager.openOverlay(win);
        win.open();
        return win;
    }

    public Window openCustomWindow(String windowId, String title, int screenW, int screenH, UiComponent content) {
        Window existing = windowManager.getWindowById(windowId);
        if (existing != null) {
            // Keskittäminen
            centerWindow(existing, screenW, screenH);
            // Sisältöä ei päivitetä automaattisesti – tämä voi olla ongelma, jos sisältö on muuttunut.
            // Jos halutaan päivittää sisältö, se pitäisi tehdä erikseen.
            windowManager.openOverlay(existing);
            return existing;
        }

        int w = 350;
        int h = 250;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        Window win = new Window(windowId, title, x, y, w, h);
        win.setMinimizable(false);
        win.setClosable(false);
        win.setOnClose(() -> win.close());

        win.clearChildren();
        win.add(content);

        windowManager.openOverlay(win);
        win.open();
        return win;
    }

    // Compatibility aliases
    public Window createPinnedWindow(int screenW, int screenH) {
        return createNewWindow(screenW, screenH);
    }

    public Window createPinnedWindow(int screenW, int screenH, ModuleManager moduleManager) {
        return createNewWindow(screenW, screenH);
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }
}