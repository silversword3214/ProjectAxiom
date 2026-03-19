package silversword.axiom.client.modules;

import java.util.List;

public interface ColorConfigurable {
    List<NamedColor> getColors();
    void openColorEditor();
}