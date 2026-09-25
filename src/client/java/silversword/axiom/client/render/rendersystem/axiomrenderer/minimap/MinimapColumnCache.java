package silversword.axiom.client.render.rendersystem.axiomrenderer.minimap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Kolumnipohjainen cache. Tallentaa jokaisen (wx, wz) -sarakkeen
 * värin ja korkeuden. Cache on LRU-pohjainen ja TTL-pohjainen:
 * vanhat entryt päivittyvät 2 sekunnin välein.
 *
 * Käyttää fastutil Long2ObjectOpenHashMap:ia — ei Long-boxausta.
 */
public final class MinimapColumnCache {

    private static final long TTL_MS = 2000L;
    private static final int MAX_ENTRIES = 500_000;

    private final Long2ObjectOpenHashMap<Column> columns = new Long2ObjectOpenHashMap<>(65536);
    private final Map<BlockState, Integer> colorCache = new HashMap<>(512);

    /** Yhden maailmasarakkeen välimuisti. */
    public static final class Column {
        public final int rawColor;      // ilman korkeusvarjostusta
        public final int shadedColor;   // korkeusvarjostuksen kanssa
        public final int topY;
        public final long cachedAt;

        Column(int rawColor, int shadedColor, int topY, long cachedAt) {
            this.rawColor = rawColor;
            this.shadedColor = shadedColor;
            this.topY = topY;
            this.cachedAt = cachedAt;
        }
    }

    private static long key(int wx, int wz) {
        return ((long) wx << 32) | (wz & 0xFFFFFFFFL);
    }

    /** Hakee kolumnin. Jos cache-miss tai vanhentunut, laskee uudelleen. */
    public Column get(Minecraft mc, Level level, int wx, int wz, long now) {
        long k = key(wx, wz);
        Column c = columns.get(k);
        if (c != null && (now - c.cachedAt) < TTL_MS) return c;

        // Cache-miss tai vanhentunut → laske
        int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
        int rawColor = 0;

        if (topY > level.getMinY() && topY <= level.getMaxY()) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(wx, topY - 1, wz);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                rawColor = computeBlockColor(mc, state, pos);
            }
        }

        // Korkeusvarjostus: katso naapurit (rekursio cacheen)
        int shadedColor = rawColor;
        if ((rawColor >>> 24) != 0) {
            int north = peekHeight(wx, wz - 1, topY);
            int south = peekHeight(wx, wz + 1, topY);
            int east  = peekHeight(wx + 1, wz, topY);
            int west  = peekHeight(wx - 1, wz, topY);

            int diff = (north + south + east + west) - topY * 4;
            if (diff != 0) {
                float factor = diff > 0
                        ? 1.0f + Math.min(0.25f, diff * 0.03f)
                        : 1.0f - Math.min(0.25f, -diff * 0.03f);

                int r = clamp((int)(((rawColor >> 16) & 0xFF) * factor));
                int g = clamp((int)(((rawColor >> 8) & 0xFF) * factor));
                int b = clamp((int)((rawColor & 0xFF) * factor));
                shadedColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        Column fresh = new Column(rawColor, shadedColor, topY, now);
        columns.put(k, fresh);

        // LRU-eviktio: jos ylitämme rajan, tyhjennä vanhimmat
        if (columns.size() > MAX_ENTRIES) {
            evictOld(now);
        }

        return fresh;
    }

    private int peekHeight(int wx, int wz, int fallback) {
        Column c = columns.get(key(wx, wz));
        return c != null ? c.topY : fallback;
    }

    private int computeBlockColor(Minecraft mc, BlockState state, BlockPos pos) {
        Integer cached = colorCache.get(state);
        if (cached != null) return cached;

        MapColor mapColor = state.getMapColor(mc.level, pos);
        int rgb = mapColor.col;

        try {
            var tintSource = mc.getBlockColors().getTintSource(state, 0);
            if (tintSource != null) {
                int tint = tintSource.colorInWorld(state, mc.level, pos);
                if (tint != -1) {
                    int tr = (tint >> 16) & 0xFF;
                    int tg = (tint >> 8) & 0xFF;
                    int tb = tint & 0xFF;
                    int br = (rgb >> 16) & 0xFF;
                    int bg = (rgb >> 8) & 0xFF;
                    int bb = rgb & 0xFF;
                    rgb = ((tr * br) / 255 << 16) | ((tg * bg) / 255 << 8) | (tb * bb) / 255;
                }
            }
        } catch (Throwable ignored) {}

        int result = 0xFF000000 | rgb;
        colorCache.put(state, result);
        return result;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    /** Poistaa vanhentuneet entryt (yli 10 s vanhat). */
    private void evictOld(long now) {
        columns.long2ObjectEntrySet().removeIf(e -> (now - e.getValue().cachedAt) > TTL_MS * 5);
    }

    /** Tyhjentää koko cachen — kutsu kun maailma vaihtuu. */
    public void invalidateAll() {
        columns.clear();
        colorCache.clear();
    }

    public int size() {
        return columns.size();
    }
}