package silversword.axiom.client.gui.window;

import java.util.ArrayList;
import java.util.List;

public final class ModuleWindowData {

    public final List<String> moduleIds = new ArrayList<>();

    public int size() {
        return moduleIds.size();
    }

    public int indexOf(String id) {
        return moduleIds.indexOf(id);
    }

    public boolean contains(String id) {
        return moduleIds.contains(id);
    }

    public void remove(String id) {
        moduleIds.remove(id);
    }

    public void insertAt(String id, int index) {
        if (id == null || id.isEmpty()) return;

        // Ensure unique
        moduleIds.remove(id);

        int i = Math.max(0, Math.min(index, moduleIds.size()));
        moduleIds.add(i, id);
    }

    public void append(String id) {
        insertAt(id, moduleIds.size());
    }

    public void swap(int a, int b) {
        if (a < 0 || b < 0) return;
        if (a >= moduleIds.size() || b >= moduleIds.size()) return;
        if (a == b) return;

        String tmp = moduleIds.get(a);
        moduleIds.set(a, moduleIds.get(b));
        moduleIds.set(b, tmp);
    }
}
