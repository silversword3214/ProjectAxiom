package silversword.axiom.client.hud.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.TexturedRenderLayers;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureRegion;

import java.util.HashMap;
import java.util.Map;

public final class EffectIconHelper {
    private static final Map<String, TextureRegion> regionCache = new HashMap<>();

    public static TextureRegion getEffectRegion(StatusEffect effect) {
        if (effect == null) {
            System.out.println("[EffectIconHelper] effect is null");
            return null;
        }

        Identifier effectId = Registries.STATUS_EFFECT.getId(effect);
        if (effectId == null) {
            System.out.println("[EffectIconHelper] could not get ID for effect: " + effect);
            return null;
        }

        String path = effectId.getPath();
        Identifier spriteId = Identifier.of("effect", path);
        System.out.println("[EffectIconHelper] trying to get sprite: " + spriteId);

        MinecraftClient mc = MinecraftClient.getInstance();
        SpriteAtlasTexture guiAtlas = (SpriteAtlasTexture) mc.getTextureManager()
                .getTexture(TexturedRenderLayers.GUI_ATLAS_TEXTURE);
        if (guiAtlas == null) {
            System.out.println("[EffectIconHelper] GUI atlas not found");
            return null;
        }

        Sprite sprite = guiAtlas.getSprite(spriteId);
        if (sprite == null) {
            System.out.println("[EffectIconHelper] sprite not found for: " + spriteId);
            return null;
        }

        String key = effectId.toString();
        TextureRegion region = regionCache.get(key);
        if (region == null) {
            region = new TextureRegion(1, 1);
            region.x1 = sprite.getMinU();
            region.y1 = sprite.getMinV();
            region.x2 = sprite.getMaxU();
            region.y2 = sprite.getMaxV();
            regionCache.put(key, region);
            System.out.println("[EffectIconHelper] created region for " + key + ": u=" + region.x1 + "-" + region.x2 + " v=" + region.y1 + "-" + region.y2);
        }
        return region;
    }

    public static int getColorWithAlpha(float alpha) {
        int a = (int) (alpha * 255);
        return 0x00FFFFFF | (a << 24);
    }
}