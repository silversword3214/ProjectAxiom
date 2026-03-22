package silversword.axiom.client.hud.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.Sheets;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureRegion;

import java.util.HashMap;
import java.util.Map;

public final class EffectIconHelper {
    private static final Map<String, TextureRegion> regionCache = new HashMap<>();

    public static TextureRegion getEffectRegion(MobEffect effect) {
        if (effect == null) {
            System.out.println("[EffectIconHelper] effect is null");
            return null;
        }

        Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (effectId == null) {
            System.out.println("[EffectIconHelper] could not get ID for effect: " + effect);
            return null;
        }

        String path = effectId.getPath();
        Identifier spriteId = Identifier.fromNamespaceAndPath("effect", path);
        System.out.println("[EffectIconHelper] trying to get sprite: " + spriteId);

        Minecraft mc = Minecraft.getInstance();
        TextureAtlas guiAtlas = (TextureAtlas) mc.getTextureManager()
                .getTexture(Sheets.GUI_SHEET);
        if (guiAtlas == null) {
            System.out.println("[EffectIconHelper] GUI atlas not found");
            return null;
        }

        TextureAtlasSprite sprite = guiAtlas.getSprite(spriteId);
        if (sprite == null) {
            System.out.println("[EffectIconHelper] sprite not found for: " + spriteId);
            return null;
        }

        String key = effectId.toString();
        TextureRegion region = regionCache.get(key);
        if (region == null) {
            region = new TextureRegion(1, 1);
            region.x1 = sprite.getU0();
            region.y1 = sprite.getV0();
            region.x2 = sprite.getU1();
            region.y2 = sprite.getV1();
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