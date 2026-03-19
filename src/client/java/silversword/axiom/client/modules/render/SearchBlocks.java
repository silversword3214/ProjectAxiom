package silversword.axiom.client.modules.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.BlockColorSelectable;
import silversword.axiom.client.modules.moduleutils.BlockSelectionView;
import silversword.axiom.client.modules.render.blockesp.Block;
import silversword.axiom.client.modules.render.blockesp.Group;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;
import silversword.axiom.client.modules.render.blockesp.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SearchBlocks extends AxiomMod implements BlockColorSelectable, KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final SettingSlider renderDistance;
    private final SettingMode boxMode;
    private final SettingBoolean tracerEnabled;
    private final Set<Identifier> targetBlocks = new HashSet<>();
    private final Map<Identifier, SettingColor> blockColors = new HashMap<>();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Per‑block rendering data cache
    private final Map<net.minecraft.block.Block, BlockData> blockDataMap = new ConcurrentHashMap<>();

    private final Setting blockListSetting;
    private final Setting blockColorsSetting;

    // Global map of all blocks for fast rendering and removal
    private final Map<BlockPos, Block> allBlocks = new ConcurrentHashMap<>();

    // Chunk cache (still needed for neighbour updates)
    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final Set<Group> groups = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Chunk scanning queue
    private final Queue<ChunkPos> chunksToScan = new ConcurrentLinkedQueue<>();
    private static final int CHUNKS_PER_TICK = 5;

    // Default block data (rainbow by default)
    private BlockData defaultBlockData;

    public SearchBlocks() {
        super("Search", "Highlight specific blocks", ModuleCategory.RENDER);

        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256}, 64);
        boxMode = new SettingMode("Box Mode", new String[]{"Lines", "Sides", "Both"}, "Lines");
        tracerEnabled = new SettingBoolean("Tracers", false);

        blockListSetting = new Setting("TargetBlocks") {
            @Override public Object getJsonValue() {
                List<String> list = new ArrayList<>();
                for (Identifier id : targetBlocks) list.add(id.toString());
                return list;
            }
            @Override public void setJsonValue(Object v) {
                targetBlocks.clear();
                if (v instanceof List) {
                    for (Object o : (List) v) {
                        String s = o.toString();
                        Identifier id = Identifier.tryParse(s);
                        if (id != null) targetBlocks.add(id);
                    }
                }
                rebuildAll();
            }
            @Override public String getType() { return "block_list"; }
            @Override public double getValue() { return 0; }
            @Override public void setValue(double value) {}
            @Override public int getHeight() { return 0; }
            @Override public void render(int x, int y, int mouseX, int mouseY) {}
            @Override public void mouseClicked(double mouseX, double mouseY, int button) {}
        };
        addHiddenSetting(blockListSetting);
        addHiddenSetting(toggleKey);


        blockColorsSetting = new Setting("BlockColors") {
            @Override public Object getJsonValue() {
                Map<String, Object[]> map = new HashMap<>();
                for (Map.Entry<Identifier, SettingColor> e : blockColors.entrySet()) {
                    SettingColor sc = e.getValue();
                    map.put(e.getKey().toString(), new Object[]{ sc.r, sc.g, sc.b, sc.a, sc.rainbow ? 1 : 0, sc.speed });
                }
                return map;
            }
            @Override public void setJsonValue(Object v) {
                blockColors.clear();
                if (v instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?,?>) v).entrySet()) {
                        String key = e.getKey().toString();
                        Object val = e.getValue();
                        if (val instanceof Object[] arr && arr.length >= 4) {
                            Identifier id = Identifier.tryParse(key);
                            if (id != null) {
                                try {
                                    int r = ((Number) arr[0]).intValue();
                                    int g = ((Number) arr[1]).intValue();
                                    int b = ((Number) arr[2]).intValue();
                                    int a = ((Number) arr[3]).intValue();
                                    SettingColor sc = new SettingColor("temp", new Color(r, g, b, a));
                                    if (arr.length > 4) sc.rainbow = ((Number) arr[4]).intValue() == 1;
                                    if (arr.length > 5) sc.speed = ((Number) arr[5]).floatValue();
                                    blockColors.put(id, sc);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
                rebuildAll();
            }
            @Override public String getType() { return "color_map"; }
            @Override public double getValue() { return 0; }
            @Override public void setValue(double value) {}
            @Override public int getHeight() { return 0; }
            @Override public void render(int x, int y, int mouseX, int mouseY) {}
            @Override public void mouseClicked(double mouseX, double mouseY, int button) {}
        };
        addHiddenSetting(blockColorsSetting);

        addSetting(renderDistance);

        // Create default colors with rainbow enabled
        SettingColor defaultLine = new SettingColor("defaultLine", new Color(0, 255, 200, 255));
        defaultLine.rainbow = true;
        defaultLine.speed = 1.0f;

        SettingColor defaultSide = new SettingColor("defaultSide", new Color(0, 255, 200, 30));
        defaultSide.rainbow = true;
        defaultSide.speed = 1.0f;

        SettingColor defaultTracer = new SettingColor("defaultTracer", new Color(0, 255, 200, 125));
        defaultTracer.rainbow = true;
        defaultTracer.speed = 1.0f;

        defaultBlockData = new BlockData(
                ShapeMode.Lines,
                defaultLine,
                defaultSide,
                false,
                defaultTracer
        );

        registerEvents();
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    private void registerEvents() {
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (isEnabled() && world == mc.world) {
                chunksToScan.offer(chunk.getPos());
            }
        });

        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (isEnabled() && world == mc.world) {
                onChunkUnload((WorldChunk) chunk);
            }
        });

        // Use client-side block break event for instant removal
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
            if (isEnabled() && world == mc.world && player == mc.player) {
                onBlockBreak(pos);
            }
        });
    }

    @Override
    protected void onEnable() {
        for (WorldChunk chunk : getLoadedChunks()) {
            chunksToScan.offer(chunk.getPos());
        }
    }

    @Override
    protected void onDisable() {
        allBlocks.clear();
        chunks.clear();
        groups.clear();
        chunksToScan.clear();
        blockDataMap.clear();
    }

    @Override
    public void onTick() {
        int processed = 0;
        while (processed < CHUNKS_PER_TICK && !chunksToScan.isEmpty()) {
            ChunkPos pos = chunksToScan.poll();
            if (pos != null) {
                WorldChunk chunk = mc.world.getChunk(pos.x, pos.z);
                if (chunk != null && chunk.getPos().equals(pos)) {
                    scanChunk(chunk);
                }
            }
            processed++;
        }
    }

    private void rebuildAll() {
        allBlocks.clear();
        chunks.clear();
        groups.clear();
        blockDataMap.clear();
        chunksToScan.clear();
        if (isEnabled()) {
            for (WorldChunk chunk : getLoadedChunks()) {
                chunksToScan.offer(chunk.getPos());
            }
        }
    }

    private void onChunkUnload(WorldChunk chunk) {
        long key = chunk.getPos().toLong();
        Chunk espChunk = chunks.remove(key);
        if (espChunk != null) {
            for (Block block : espChunk.getBlocks()) {
                allBlocks.remove(block.pos);
                if (block.group != null) {
                    block.group.remove(block);
                    if (block.group.size() == 0) groups.remove(block.group);
                }
            }
        }
    }

    public void onBlockBreak(BlockPos pos) {
        Block removed = allBlocks.remove(pos);
        if (removed != null) {
            long key = new ChunkPos(pos).toLong();
            Chunk chunk = chunks.get(key);
            if (chunk != null) {
                chunk.removeBlock(pos);
            }
            if (removed.group != null) {
                removed.group.remove(removed);
                if (removed.group.size() == 0) groups.remove(removed.group);
            }
            updateNeighboursAround(pos);
        }
    }

    private void updateNeighboursAround(BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbourPos = center.add(dx, dy, dz);
                    Block neighbour = allBlocks.get(neighbourPos);
                    if (neighbour != null) {
                        long key = new ChunkPos(neighbourPos).toLong();
                        Chunk chunk = chunks.get(key);
                        if (chunk != null) {
                            neighbour.updateNeighbours(chunk);
                        }
                    }
                }
            }
        }
    }

    private void scanChunk(WorldChunk chunk) {
        if (targetBlocks.isEmpty()) return;
        long key = chunk.getPos().toLong();
        Chunk espChunk = new Chunk(chunk.getPos());

        int minY = mc.world.getBottomY();
        int maxY = mc.world.getTopYInclusive();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (targetBlocks.contains(Registries.BLOCK.getId(state.getBlock()))) {
                        Block block = espChunk.addBlock(pos.toImmutable(), state.getBlock());
                        allBlocks.put(pos.toImmutable(), block);
                    }
                }
            }
        }

        if (espChunk.size() > 0) {
            espChunk.updateNeighbours();
            chunks.put(key, espChunk);
        }
    }

    private List<WorldChunk> getLoadedChunks() {
        if (mc.world == null) return Collections.emptyList();
        List<WorldChunk> list = new ArrayList<>();
        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();
        for (int x = playerChunk.x - viewDist; x <= playerChunk.x + viewDist; x++) {
            for (int z = playerChunk.z - viewDist; z <= playerChunk.z + viewDist; z++) {
                WorldChunk chunk = mc.world.getChunk(x, z);
                if (chunk != null && chunk.getPos().x == x && chunk.getPos().z == z) {
                    list.add(chunk);
                }
            }
        }
        return list;
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3d cameraPos = event.cameraX != 0 ? new Vec3d(event.cameraX, event.cameraY, event.cameraZ) : mc.player.getEntityPos();

        for (Block block : allBlocks.values()) {
            if (block.pos.getSquaredDistance(cameraPos) > maxDistSq) continue;
            BlockData data = getBlockData(block.block);
            block.render(event.render, data);
        }

        if (tracerEnabled.get()) {
            for (Group group : groups) {
                BlockData data = getBlockData(group.block);
                if (data.tracer) {
                    group.renderTracer(event.render, data.tracerColor.getCurrentColor());
                }
            }
        }
    }

    public BlockData getBlockData(net.minecraft.block.Block block) {
        // Return cached data if exists
        BlockData data = blockDataMap.get(block);
        if (data != null) return data;

        // Create from default and apply custom colors if any
        SettingColor line = defaultBlockData.lineColor.copy();
        SettingColor side = defaultBlockData.sideColor.copy();
        SettingColor tracer = defaultBlockData.tracerColor.copy();

        SettingColor custom = blockColors.get(Registries.BLOCK.getId(block));
        if (custom != null) {
            line.set(custom.r, custom.g, custom.b, custom.a);
            side.set(custom.r, custom.g, custom.b, custom.a);
            tracer.set(custom.r, custom.g, custom.b, custom.a);
            line.rainbow = custom.rainbow;
            line.speed = custom.speed;
            side.rainbow = custom.rainbow;
            side.speed = custom.speed;
            tracer.rainbow = custom.rainbow;
            tracer.speed = custom.speed;
        }

        data = new BlockData(
                defaultBlockData.shapeMode,
                line,
                side,
                defaultBlockData.tracer,
                tracer
        );
        blockDataMap.put(block, data);
        return data;
    }

    // ---- BlockSelectable / BlockColorSelectable ----
    @Override
    public boolean isBlockSelected(net.minecraft.block.Block block) {
        return targetBlocks.contains(Registries.BLOCK.getId(block));
    }

    @Override
    public void toggleBlock(net.minecraft.block.Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        if (targetBlocks.contains(id)) {
            targetBlocks.remove(id);
            blockColors.remove(id);
            blockDataMap.remove(block);
        } else {
            targetBlocks.add(id);
        }
        rebuildAll();
    }

    @Override
    public SettingColor getBlockColor(net.minecraft.block.Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        return blockColors.get(id);
    }

    @Override
    public void setBlockColor(net.minecraft.block.Block block, SettingColor color) {
        Identifier id = Registries.BLOCK.getId(block);
        if (targetBlocks.contains(id)) {
            blockColors.put(id, color);
            blockDataMap.remove(block); // force rebuild on next render
        }
    }

    public void openBlockSelector() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new BlockSelectionView(this);
        factory.openCustomWindow("search_blocks", "Select Blocks to Search", sw, sh, content);
    }
}