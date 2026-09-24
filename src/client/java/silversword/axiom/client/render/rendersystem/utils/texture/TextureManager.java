package silversword.axiom.client.render.rendersystem.utils.texture;

import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class TextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProjectAxiom/TextureManager");
    private static final Map<Identifier, Texture> CACHE = new HashMap<>();

    private TextureManager() {}

    public static Texture getTexture(Identifier id) {
        return CACHE.computeIfAbsent(id, TextureManager::loadTexture);
    }

    private static Texture loadTexture(Identifier id) {
        String path = String.format("/assets/%s/%s", id.getNamespace(), id.getPath());
        LOGGER.info("[TextureManager] Loading texture {} from {}", id, path);

        Texture tex = Texture.readResource(path, false, FilterMode.LINEAR);
        if (tex == null) {
            LOGGER.error("[TextureManager] FAILED to load texture: {} (path={})", id, path);
            return null;
        }
        Minecraft.getInstance().getTextureManager().register(id, tex);
        LOGGER.info("[TextureManager] Registered vanilla texture: {}", id);

        return tex;
    }

    public static void reload() {
        CACHE.values().forEach(Texture::close);
        CACHE.clear();
        LOGGER.info("Texture cache cleared");
    }

    public static void registerTexture(Identifier id, Texture texture) {
        CACHE.put(id, texture);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }
}