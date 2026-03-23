package silversword.axiom;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ProjectAxiom {
    public static final String MOD_ID = "projectaxiom";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION;

    static {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
        VERSION = container.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}