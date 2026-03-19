package silversword.axiom.client.render.rendersystem.utils.texture;

import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Hallinnoi tekstuureja HUD-elementtejä varten.
 * Tekstuurit ladataan modin resursseista ja säilytetään välimuistissa.
 */
public final class TextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProjectAxiom,/TextureManager");
    private static final Map<Identifier, Texture> CACHE = new HashMap<>();

    private TextureManager() {}

    /**
     * Hakee tekstuurin annetulla tunnisteella. Lataa sen tarvittaessa levyltä.
     *
     * @param id Tekstuurin tunniste (esim. new Identifier("projectaxiom", "textures/logo.png"))
     * @return Tekstuuri tai null, jos lataus epäonnistui
     */
    public static Texture getTexture(Identifier id) {
        return CACHE.computeIfAbsent(id, TextureManager::loadTexture);
    }

    /**
     * Lataa tekstuurin resurssipolusta.
     */
    private static Texture loadTexture(Identifier id) {
        // Filepath: "/assets/projectaxiom/textures/logo.png"
        String path = String.format("/assets/%s/%s", id.getNamespace(), id.getPath());
        LOGGER.debug("Loading texture from {}", path);

        // flipY = true, jotta tekstuuri on oikein päin (0,0 on vasen yläkulma)
        Texture tex = Texture.readResource(path, false, FilterMode.LINEAR);
        if (tex == null) {
            LOGGER.error("Failed to load texture: {}", id);
        }
        return tex;
    }

    /**
     * Tyhjentää välimuistin. Hyödyllinen esim. resurssipakettien uudelleenlatauksen yhteydessä.
     */
    public static void reload() {
        CACHE.values().forEach(Texture::close); // jos Texture toteuttaa close-metodin (AbstractTexture perii NativeImage)
        CACHE.clear();
        LOGGER.info("Texture cache cleared");
    }

    /**
     * Rekisteröi tekstuurin manuaalisesti (harvoin tarpeen).
     */
    public static void registerTexture(Identifier id, Texture texture) {
        CACHE.put(id, texture);
    }
}