package silversword.axiom.client.modules.misc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.BlockSelectable;
import silversword.axiom.client.modules.moduleutils.BlockSelectionView;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;

import java.util.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class TunnelMiner extends AxiomMod implements BlockSelectable, KeybindConfigurable {

    public static TunnelMiner INSTANCE;

    // Asetukset
    private final SettingBoolean enabled = new SettingBoolean("Enabled", true);
    private final SettingSlider length = new SettingSlider("Length", new double[]{5, 10, 20, 30, 50, 100}, 20);
    private final SettingSlider height = new SettingSlider("Height", new double[]{1, 2, 3}, 2);
    private final SettingSlider width = new SettingSlider("Width", new double[]{1, 2, 3}, 1);
    private final SettingMode shapeMode = new SettingMode("Shape Mode", new String[]{"Lines", "Sides", "Both"}, "Both");
    private final SettingColor sideColor = new SettingColor("Side Color", new Color(255, 0, 0, 30));
    private final SettingColor lineColor = new SettingColor("Line Color", new Color(255, 0, 0, 255));
    private final SettingBoolean autoBreak = new SettingBoolean("Auto Break", true);
    private final SettingBoolean autoWalk = new SettingBoolean("Auto Walk", true);
    private final SettingBoolean fillGaps = new SettingBoolean("Fill Gaps", true);
    private final SettingMode fillMaterial = new SettingMode("Fill Material", new String[]{"Cobblestone", "Dirt", "Stone", "Deepslate", "Netherrack"}, "Cobblestone");
    private final SettingBoolean replaceLava = new SettingBoolean("Replace Lava", true);
    private final SettingBoolean replaceWater = new SettingBoolean("Replace Water", true);
    private final SettingBoolean alsoPlaceBelow = new SettingBoolean("Place Below", true);
    private final SettingBoolean targetMode = new SettingBoolean("Target Mode", true);
    private final SettingSlider targetRange = new SettingSlider("Target Range", new double[]{16, 32, 48, 64, 80, 100}, 48);
    private final SettingColor targetBoxColor = new SettingColor("Target Box Color", new Color(255, 255, 255, 50));
    private final SettingColor targetLineColor = new SettingColor("Target Line Color", new Color(255, 255, 255, 255));

    private final Set<Identifier> targetBlocks = new HashSet<>();
    private final Setting blockListSetting;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Tilanhallinta
    private List<BlockPos> blocksToMine = new ArrayList<>();
    private int currentBlockIndex = 0;
    private BlockPos currentMiningPos = null;
    private BlockPos pendingPlacePos = null;
    private State state = State.IDLE;

    // Polun kulmapisteet
    private List<BlockPos> pathCorners = new ArrayList<>();
    private int pathCornerIndex = 0;

    private BlockPos currentTarget = null;
    private Block currentTargetBlock = null;            // talteen targetin blokki ennen rikkomista
    private int scanTimer = 0;
    private static final int SCAN_INTERVAL = 20;
    private final Random random = new Random();
    private BlockPos collectTarget = null;
    private int stableTicks = 0;        // kuinka monta tickiä korkeus on pysynyt samana
    private int lastY = 0;


    private boolean skipJump = false; // Estää hyppimisen, kun alapuoli on tyhjä

    private boolean isClimbing = false;

    private enum State { IDLE, MOVING, MINING, PLACING, CLIMBING, COLLECTING }

    private List<BlockPos> allTargets = new ArrayList<>();

    private enum ClimbState {
        MINE,   // kaivetaan yläpuolinen lohko
        JUMP,   // hypätään kerran
        PLACE,  // asetetaan lohko alle
        DONE    // nousu valmis
    }
    private ClimbState climbState = ClimbState.MINE;
    private int placeAttempts = 0;

    private Block minedTargetBlock = null; // viimeksi rikotun targetin blokki (talteen itemin etsintää varten)

    private Entity collectItem = null;

    public TunnelMiner() {
        super("TunnelMiner", "Digs tunnel to target block (experimemtal)", ModuleCategory.MISC);
        INSTANCE = this;

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
            }
            @Override public String getType() { return "block_list"; }
            @Override public double getValue() { return 0; }
            @Override public void setValue(double value) {}
            @Override public int getHeight() { return 0; }
            @Override public void render(int x, int y, int mouseX, int mouseY) {}
            @Override public void mouseClicked(double mouseX, double mouseY, int button) {}
        };
        addHiddenSetting(blockListSetting);

        addSetting(enabled);
        addSetting(length);
        addSetting(height);
        addSetting(width);
        addSetting(shapeMode);
        addSetting(autoBreak);
        addSetting(autoWalk);
        addSetting(fillGaps);
        addSetting(fillMaterial);
        addSetting(replaceLava);
        addSetting(replaceWater);
        addSetting(alsoPlaceBelow);
        addSetting(targetMode);
        addSetting(targetRange);



        addHiddenSetting(sideColor.getSetting());
        addHiddenSetting(lineColor.getSetting());
        addHiddenSetting(targetBoxColor.getSetting());
        addHiddenSetting(targetLineColor.getSetting());

        addHiddenSetting(toggleKey);

        AxiomInitialize.EVENT_BUS.subscribe(this);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled() || !enabled.get()) { reset(); return; }

        if (targetMode.get()) {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                findNearestTarget();
            }
        } else {
            currentTarget = null;
        }

        if (targetMode.get() && currentTarget != null) {
            computeMiningPath();
            handleTargeting();
            return;
        }

        if (!targetMode.get()) {
            handleTunnelMining();
        }

        if (state == State.COLLECTING) {
            handleCollecting();
            return;
        }
    }

    private boolean isWithinReach(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        return eyes.distanceTo(center) <= 4.8;
    }

    private boolean canSee(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);

        BlockHitResult result = mc.world.raycast(
                new net.minecraft.world.RaycastContext(
                        eyes,
                        target,
                        net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );

        return result.getBlockPos().equals(pos);
    }


    private void findNearestTarget() {
        if (targetBlocks.isEmpty() || mc.player == null || mc.world == null) {
            currentTarget = null; blocksToMine.clear(); pathCorners.clear(); allTargets.clear(); return;
        }
        double maxDist = targetRange.getValue();
        double maxDistSq = maxDist * maxDist;
        Vec3d playerPos = mc.player.getEntityPos();
        int radius = (int) Math.ceil(maxDist / 16) + 1;
        int chunkX = mc.player.getChunkPos().x;
        int chunkZ = mc.player.getChunkPos().z;

        allTargets.clear();
        List<BlockPos> candidates = new ArrayList<>();
        double bestDistSq = Double.MAX_VALUE;

        for (int cx = chunkX - radius; cx <= chunkX + radius; cx++) {
            for (int cz = chunkZ - radius; cz <= chunkZ + radius; cz++) {
                net.minecraft.world.chunk.Chunk chunk = mc.world.getChunk(cx, cz);
                if (!(chunk instanceof WorldChunk worldChunk)) continue;
                int minY = mc.world.getBottomY();
                int maxY = mc.world.getTopYInclusive();
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        for (int dy = minY; dy < maxY; dy++) {
                            BlockPos pos = new BlockPos(cx * 16 + dx, dy, cz * 16 + dz);
                            double distSq = pos.getSquaredDistance(playerPos);
                            if (distSq > maxDistSq) continue;
                            BlockState state = worldChunk.getBlockState(pos);
                            if (state.isAir()) continue;
                            Identifier id = Registries.BLOCK.getId(state.getBlock());
                            if (targetBlocks.contains(id)) {
                                allTargets.add(pos.toImmutable());
                                if (distSq < bestDistSq) {
                                    bestDistSq = distSq;
                                    candidates.clear();
                                    candidates.add(pos.toImmutable());
                                } else if (distSq == bestDistSq) {
                                    candidates.add(pos.toImmutable());
                                }
                            }
                        }
                    }
                }
            }
        }
        BlockPos newTarget = null;
        if (!candidates.isEmpty()) newTarget = candidates.get(random.nextInt(candidates.size()));
        if (newTarget != null && !newTarget.equals(currentTarget)) {
            currentTarget = newTarget;
            // Tallenna targetin blokki myöhempää käyttöä varten
            currentTargetBlock = mc.world.getBlockState(currentTarget).getBlock();
            computeMiningPath();
        } else if (newTarget == null) {
            currentTarget = null;
            currentTargetBlock = null;
            blocksToMine.clear();
            pathCorners.clear();
            allTargets.clear();
        }
    }

    private void computeMiningPath() {
        blocksToMine.clear();
        pathCorners.clear();
        if (currentTarget == null || mc.player == null) return;

        BlockPos start = mc.player.getBlockPos();
        int startX = start.getX(), startY = start.getY(), startZ = start.getZ();
        int targetX = currentTarget.getX(), targetY = currentTarget.getY(), targetZ = currentTarget.getZ();

        List<BlockPos> corners = new ArrayList<>();
        corners.add(start);

        // Jos target on samassa pystylinjassa (x ja z samat)
        if (targetX == startX && targetZ == startZ) {
            // Suoraan ylös tai alas
            corners.add(currentTarget);
        } else {
            // Manhattan-polku: ensin vaakasuunnassa, sitten pystysuunnassa
            BlockPos corner1 = new BlockPos(targetX, startY, startZ);
            corners.add(corner1);
            // Jos korkeus muuttuu, lisää corner2
            if (targetY != startY) {
                BlockPos corner2 = new BlockPos(targetX, targetY, startZ);
                corners.add(corner2);
            }
            corners.add(currentTarget);
        }

        // Poista duplikaatit
        for (int i = corners.size()-1; i>0; i--) {
            if (corners.get(i).equals(corners.get(i-1))) {
                corners.remove(i);
            }
        }

        pathCorners = corners;
        pathCornerIndex = 1;

        // Kerää kaikki lohkot reitin varrelta (vain kiinteät)
        for (int i = 0; i < corners.size()-1; i++) {
            BlockPos from = corners.get(i);
            BlockPos to = corners.get(i+1);
            int x1 = from.getX(), y1 = from.getY(), z1 = from.getZ();
            int x2 = to.getX(), y2 = to.getY(), z2 = to.getZ();

            if (x1 != x2) {
                int stepX = Integer.signum(x2 - x1);
                for (int x = x1 + stepX; x != x2 + stepX; x += stepX) {
                    BlockPos feet = new BlockPos(x, y1, z1);
                    BlockPos head = feet.up();
                    if (isSolidAndMineable(feet)) blocksToMine.add(feet);
                    if (isSolidAndMineable(head)) blocksToMine.add(head);
                }
            } else if (z1 != z2) {
                int stepZ = Integer.signum(z2 - z1);
                for (int z = z1 + stepZ; z != z2 + stepZ; z += stepZ) {
                    BlockPos feet = new BlockPos(x1, y1, z);
                    BlockPos head = feet.up();
                    if (isSolidAndMineable(feet)) blocksToMine.add(feet);
                    if (isSolidAndMineable(head)) blocksToMine.add(head);
                }
            } else if (y1 != y2) {
                int stepY = Integer.signum(y2 - y1);
                for (int y = y1 + stepY; y != y2 + stepY; y += stepY) {
                    BlockPos feet = new BlockPos(x1, y, z1);
                    BlockPos head = feet.up();
                    if (isSolidAndMineable(feet)) blocksToMine.add(feet);
                    if (isSolidAndMineable(head)) blocksToMine.add(head);
                }
            }
        }

        if (!blocksToMine.contains(currentTarget) && isSolidAndMineable(currentTarget))
            blocksToMine.add(currentTarget);

        Vec3d playerPos = mc.player.getEntityPos();
        blocksToMine.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(playerPos)));
        currentBlockIndex = 0;
    }

    private void handleTargeting() {
        if (currentTarget != null && mc.world.getBlockState(currentTarget).isAir()) {
            // Target on rikottu
            minedTargetBlock = currentTargetBlock;   // tallennetaan blokki itemin etsintää varten
            currentTarget = null;
            currentTargetBlock = null;
            blocksToMine.clear();
            pathCorners.clear();
            isClimbing = false;
            state = State.COLLECTING;
            findNearestItem();
            return;
        }

        // Aukkojen täyttö
        if (fillGaps.get() && pendingPlacePos == null) {
            BlockPos placePos = checkGapsAndFluids();
            if (placePos != null && prepareBlockInOffhand()) {
                pendingPlacePos = placePos;
            }
        }
        if (pendingPlacePos != null) {
            if (placeBlockFromOffhand(pendingPlacePos)) pendingPlacePos = null;
            return;
        }

        // 🔥 TÄRKEÄ: Jos target on näkyvissä ja ulottuvilla, kaiva sitä suoraan
        if (currentTarget != null && isWithinReach(currentTarget) && canSee(currentTarget)) {
            state = State.MINING;
            if (autoWalk.get()) {
                mc.options.forwardKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
            }
            lookAt(Vec3d.ofCenter(currentTarget));
            mineBlock(currentTarget);
            return;
        }

        // Tarkistetaan, tarvitaanko nousua
        boolean needClimb = false;
        int yDiff = 0;
        if (pathCornerIndex < pathCorners.size()) {
            BlockPos nextCorner = pathCorners.get(pathCornerIndex);
            yDiff = nextCorner.getY() - mc.player.getBlockPos().getY();
            if (yDiff > 0) needClimb = true;
        }

        if (needClimb) {
            if (state != State.CLIMBING) {
                // Aloitetaan uusi nousu, nollataan climbState
                climbState = ClimbState.MINE;
            }
            state = State.CLIMBING;
            handleClimbing();
            return;
        }

        // Normaali kaivaminen (alaspäin tai vaakasuora)
        boolean mined = mineNormalBlocks();

        if (mined) {
            if (autoWalk.get()) {
                mc.options.forwardKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
            }
            return;
        }

        // Liikkuminen
        if (currentTarget != null) {
            Vec3d targetVec = Vec3d.ofCenter(currentTarget);
            double distance = mc.player.getEntityPos().distanceTo(targetVec);

            if (distance < 1.25 && canSee(currentTarget)) {
                mineBlock(currentTarget);
                return;
            }

            if (pathCornerIndex < pathCorners.size()) {
                BlockPos nextCorner = pathCorners.get(pathCornerIndex);
                Vec3d cornerVec = Vec3d.ofCenter(nextCorner);
                double cornerDist = mc.player.getEntityPos().distanceTo(cornerVec);

                if (cornerDist < 0.5) {
                    pathCornerIndex++;
                    if (pathCornerIndex < pathCorners.size()) {
                        nextCorner = pathCorners.get(pathCornerIndex);
                        cornerVec = Vec3d.ofCenter(nextCorner);
                    } else {
                        nextCorner = currentTarget;
                        cornerVec = Vec3d.ofCenter(nextCorner);
                    }
                }

                state = State.MOVING;
                if (autoWalk.get()) {
                    lookAt(cornerVec);
                    Direction facing = mc.player.getHorizontalFacing();
                    BlockPos playerFeet = mc.player.getBlockPos();
                    BlockPos nextFeet = playerFeet.offset(facing);
                    boolean needJump = nextFeet.getY() > playerFeet.getY() && !skipJump;
                    if (needJump) mc.options.jumpKey.setPressed(true);
                    else mc.options.jumpKey.setPressed(false);
                    mc.options.forwardKey.setPressed(true);
                }
            } else {
                state = State.MOVING;
                if (autoWalk.get()) {
                    lookAt(targetVec);
                    mc.options.forwardKey.setPressed(true);
                }
            }
        }
    }

    private void findNearestItem() {
        collectItem = null;
        collectTarget = null;
        if (minedTargetBlock == null) {
            // Ei tiedetä mitä pitäisi kerätä – palataan IDLE
            state = State.IDLE;
            return;
        }

        double closestDist = Double.MAX_VALUE;
        for (ItemEntity item : mc.world.getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(targetRange.getValue()), e -> true)) {
            ItemStack stack = item.getStack();
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            BlockItem blockItem = (BlockItem) stack.getItem();
            if (blockItem.getBlock() == minedTargetBlock) {
                double dist = mc.player.distanceTo(item);
                if (dist < closestDist) {
                    closestDist = dist;
                    collectItem = item;
                    collectTarget = item.getBlockPos();
                }
            }
        }

        if (collectItem == null) {
            // Ei löytynyt haluttua itemiä – palataan IDLE
            state = State.IDLE;
            minedTargetBlock = null;
        }
    }

    private void handleCollecting() {
        if (collectItem == null || !collectItem.isAlive()) {
            // Item on kerätty tai kadonnut
            state = State.IDLE;
            collectItem = null;
            collectTarget = null;
            minedTargetBlock = null;
            if (autoWalk.get()) {
                mc.options.forwardKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
            }
            return;
        }

        Vec3d itemPos = collectItem.getEntityPos();
        double distance = mc.player.getEntityPos().distanceTo(itemPos);

        if (distance < 1.5) {
            // Lähellä, pysäytetään liike ja odotetaan että item katoaa (kerätään automaattisesti)
            if (autoWalk.get()) {
                mc.options.forwardKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
            }
            return;
        }

        // Liiku kohti itemiä
        if (autoWalk.get()) {
            lookAt(itemPos);
            mc.options.forwardKey.setPressed(true);
        }
    }

    private void handleClimbing() {
        // Pysäytetään vaakaliike
        if (autoWalk.get()) {
            mc.options.forwardKey.setPressed(false);
        }

        BlockPos playerFeet = mc.player.getBlockPos();
        BlockPos aboveHead = playerFeet.up(2);
        BlockPos below = playerFeet.down();

        // Lasketaan korkeusero seuraavaan kulmapisteeseen
        int targetY = pathCorners.get(pathCornerIndex).getY();
        int currentY = playerFeet.getY();
        int yDiff = targetY - currentY;

        switch (climbState) {
            case MINE:
                // Kaivetaan yläpuolinen lohko, jos se on olemassa ja ulottuvilla
                if (isSolidAndMineable(aboveHead) && isWithinReach(aboveHead)) {
                    state = State.MINING;
                    lookAt(Vec3d.ofCenter(aboveHead));
                    mineBlock(aboveHead);
                    return;
                }
                // Jos yläpuoli on tyhjä, siirry hypyn odotukseen
                climbState = ClimbState.JUMP;
                // putoa läpi, jotta hypätään heti seuraavalla tikillä
                break;

            case JUMP:
                if (mc.player.isOnGround()) {
                    mc.options.jumpKey.setPressed(true);
                    // Alustetaan place-yritykset ja vakauslaskuri
                    placeAttempts = 0;
                    stableTicks = 0;
                    lastY = mc.player.getBlockPos().getY();
                    climbState = ClimbState.PLACE;
                }
                return;

            case PLACE:
                mc.options.jumpKey.setPressed(false);

                // Yritä asettaa blokkia alapuolelle
                placeBlockFromOffhand(below);

                // Tarkista korkeuden muutos
                int thisY = mc.player.getBlockPos().getY();
                if (thisY > lastY) {
                    // Korkeus nousi, nollataan vakauslaskuri
                    stableTicks = 0;
                    lastY = thisY;
                } else if (thisY == lastY) {
                    // Korkeus pysyi samana, lisätään laskuria
                    stableTicks++;
                    if (stableTicks >= 4) { // 4 tickiä = 0.2 sekuntia
                        // Korkeus on vakiintunut, siirrytään takaisin kaivamaan
                        placeBlockFromOffhand(below);
                        climbState = ClimbState.MINE;
                        stableTicks = 0;
                    }
                } else {
                    // Korkeus laski (harvinaista), nollataan
                    stableTicks = 0;
                    lastY = thisY;
                }
                return;
        }

        // Tarkistetaan onko jo saavutettu haluttu korkeus
        if (currentY >= targetY && pathCornerIndex == pathCorners.size() - 1) {
            // Ennen siirtymistä varmistetaan, että alla on kiinteä lohko
            if (mc.world.getBlockState(below).isAir()) {
                // Alapuoli on ilmaa – yritä asettaa blokki
                if (prepareBlockInOffhand() && placeBlockFromOffhand(below)) {
                    // Asetus onnistui, nyt voidaan siirtyä
                } else {
                    // Ei onnistuttu asettamaan – jäädään PLACE-tilaan odottamaan
                    climbState = ClimbState.PLACE;
                    return;
                }
            }

            // Siirrytään MOVING-tilaan
            skipJump = true; // Estetään hyppy tällä kertaa
            state = State.MOVING;
            pathCornerIndex++;
            climbState = ClimbState.MINE;
            mc.options.jumpKey.setPressed(false);
        }
    }

    private boolean placeBlockFromOffhand(BlockPos pos) {
        // Jos paikalla ei ole ilmaa, ei tarvitse placea
        if (!mc.world.getBlockState(pos).isAir()) return true;

        // Varmista offhand sisältö (kutsu prepareBlockInOffhand() ennen metodia)
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem)) return false;

        // Priorisoi suunnat: ensin DOWN (alla), sitten sivut, lopuksi UP
        Direction[] preferred = new Direction[] {
                Direction.DOWN,
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST,
                Direction.UP
        };

        // Tallenna vanha katselukulma, jos haluat palauttaa sen myöhemmin
        float oldYaw = mc.player.getYaw();
        float oldPitch = mc.player.getPitch();

        for (Direction dir : preferred) {
            BlockPos neighbor = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighbor);

            // Tarvitsemme solidin naapurilohkon, josta voi klikata pos:in pintaa
            // isSideSolidFullSquare varmistaa että pintaa voi käyttää placementiin
            if (neighborState.isAir() || !neighborState.isSideSolidFullSquare(mc.world, neighbor, dir.getOpposite()))
                continue;

            // Hit-face on naapurin puoli joka osoittaa kohti target-paikkaa
            Direction hitFace = dir.getOpposite();

            // Lasketaan klikattava piste naapurin pinnalle.
            // Esim. jos hitFace == UP, halutaan klikata naapurin yläpintaa (keskellä y + 0.5)
            Vec3d neighborCenter = Vec3d.ofCenter(neighbor);
            Vec3d hitPos = neighborCenter.add(
                    hitFace.getOffsetX() * 0.45,
                    hitFace.getOffsetY() * 0.45,
                    hitFace.getOffsetZ() * 0.45
            );

            // Käännä nopeasti katseen kohti klikattavaa pistettä (tarpeellinen modeissa joissa server vaatii)
            lookAt(hitPos);

            // Luo BlockHitResult, käyttäen naapurin pos:ia ja osumaa sen pintaan
            BlockHitResult bhr = new BlockHitResult(hitPos, hitFace, neighbor, false);

            // Yritä sijoittaa offhandilla
            ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, bhr);

            // Vaihtoehtoisesti joillain versioilla voi tarvita interactItem tai swing ennen/ jälkeen;
            // tässä swingataan jos sijoitus hyväksyttiin.
            if (result.isAccepted()) {
                mc.player.swingHand(Hand.OFF_HAND);
                // Palauta kamera vanhaksi (valinnainen)
                mc.player.setYaw(oldYaw);
                mc.player.setPitch(oldPitch);
                return true;
            }
            // jos ei hyväksytty, kokeillaan seuraavaa naapuria
        }

        // Jos ei löytynyt sopivaa naapuria tai sijoitus epäonnistui
        // Palauta katselukulma
        mc.player.setYaw(oldYaw);
        mc.player.setPitch(oldPitch);
        return false;
    }

    private boolean mineNormalBlocks() {
        if (currentMiningPos != null) {
            BlockState state = mc.world.getBlockState(currentMiningPos);
            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                mineBlock(currentMiningPos);
                return true;
            } else {
                currentMiningPos = null;
            }
        }

        Direction facing = mc.player.getHorizontalFacing();
        BlockPos playerFeet = mc.player.getBlockPos();

        // Tarkistetaan, vaatiiko seuraava kulmapiste alaspäin liikettä
        boolean needDescend = false;
        int yDiff = 0;
        if (pathCornerIndex < pathCorners.size()) {
            BlockPos nextCorner = pathCorners.get(pathCornerIndex);
            yDiff = nextCorner.getY() - playerFeet.getY();
            if (yDiff < 0) needDescend = true;
        }

        if (needDescend) {
            BlockPos below = playerFeet.down();
            if (isSolidAndMineable(below)) {
                mineBlock(below);
                return true;
            }
        }

        // Tarkistetaan, onko edessä oleva lohko esteenä seuraavaan kulmapisteeseen
        if (pathCornerIndex < pathCorners.size()) {
            BlockPos nextCorner = pathCorners.get(pathCornerIndex);
            // Jos seuraava kulmapiste on samassa Y-tasossa ja eri suunnassa, tarkistetaan onko siinä lohko
            if (nextCorner.getY() == playerFeet.getY()) {
                // Lasketaan suunta seuraavaan kulmaan
                int dx = Integer.signum(nextCorner.getX() - playerFeet.getX());
                int dz = Integer.signum(nextCorner.getZ() - playerFeet.getZ());
                if (dx != 0 || dz != 0) {
                    Direction dir = Direction.fromVector(new BlockPos(dx, 0, dz), null);
                    if (dir != null && dir == facing) {
                        BlockPos frontBlock = playerFeet.offset(facing);
                        if (isSolidAndMineable(frontBlock)) {
                            mineBlock(frontBlock);
                            return true;
                        }
                    }
                }
            }
        }

        // Vaakasuuntaiset lohkot (edessä)
        BlockPos frontFeet = playerFeet.offset(facing);
        if (blocksToMine.contains(frontFeet) && isSolidAndMineable(frontFeet)) {
            mineBlock(frontFeet);
            return true;
        }

        BlockPos frontHead = frontFeet.up();
        if (blocksToMine.contains(frontHead) && isSolidAndMineable(frontHead)) {
            mineBlock(frontHead);
            return true;
        }

        return false;
    }

    private boolean isSolidAndMineable(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir() && state.getBlock() != Blocks.BEDROCK;
    }

    private void mineBlock(BlockPos pos) {
        if (!autoBreak.get()) return;
        if (currentMiningPos == null || !currentMiningPos.equals(pos)) {
            if (currentMiningPos != null) mc.interactionManager.cancelBlockBreaking();
            mc.interactionManager.attackBlock(pos, Direction.UP);
            currentMiningPos = pos;
        } else {
            mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        }
    }

    private void lookAt(Vec3d target) {
        Vec3d playerPos = mc.player.getEyePos();
        double dx = target.x - playerPos.x;
        double dy = target.y - playerPos.y;
        double dz = target.z - playerPos.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double pitch = Math.toDegrees(-Math.asin(dy / dist));
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    private BlockPos checkGapsAndFluids() {
        Direction direction = mc.player.getHorizontalFacing();
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nextPos = playerPos.offset(direction);
        BlockPos belowNextPos = nextPos.down();
        if (alsoPlaceBelow.get() && mc.world.getBlockState(belowNextPos).isAir()) return belowNextPos;
        BlockState nextState = mc.world.getBlockState(nextPos);
        boolean isLava = nextState.getBlock() == Blocks.LAVA;
        boolean isWater = nextState.getBlock() == Blocks.WATER;
        if ((replaceLava.get() && isLava) || (replaceWater.get() && isWater)) return nextPos;
        return null;
    }

    private boolean prepareBlockInOffhand() {
        Block targetBlock = getMaterialBlock();
        if (targetBlock == null) return false;
        int slot = findBlockInInventory(targetBlock);
        if (slot == -1) return false;
        int syncId = mc.player.playerScreenHandler.syncId;
        if (mc.player.getOffHandStack().getItem() instanceof BlockItem &&
                ((BlockItem) mc.player.getOffHandStack().getItem()).getBlock() == targetBlock) return true;
        int offhandSlotId = 45;
        int fromSlotId = invIndexToPlayerScreenSlotId(slot);
        mc.interactionManager.clickSlot(syncId, fromSlotId, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, offhandSlotId, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty())
            mc.interactionManager.clickSlot(syncId, fromSlotId, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        return true;
    }

    private Block getMaterialBlock() {
        String material = fillMaterial.getMode();
        switch (material) {
            case "Cobblestone": return Blocks.COBBLESTONE;
            case "Dirt": return Blocks.DIRT;
            case "Stone": return Blocks.STONE;
            case "Deepslate": return Blocks.DEEPSLATE;
            case "Netherrack": return Blocks.NETHERRACK;
            default: return Blocks.COBBLESTONE;
        }
    }

    private int findBlockInInventory(Block block) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                BlockItem blockItem = (BlockItem) stack.getItem();
                if (blockItem.getBlock() == block) return i;
            }
        }
        return -1;
    }

    private int invIndexToPlayerScreenSlotId(int invIndex) {
        if (invIndex < 0 || invIndex > 35) return -1;
        if (invIndex < 9) return 36 + invIndex;
        return invIndex;
    }


    private void calculateTunnel() {
        blocksToMine.clear();
        if (mc.player == null || mc.world == null) return;
        Direction direction = mc.player.getHorizontalFacing();
        int len = (int) length.getValue();
        int h = (int) height.getValue();
        int w = (int) width.getValue();
        BlockPos start = mc.player.getBlockPos();
        for (int i = 1; i <= len; i++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dx = - (w / 2); dx <= (w / 2); dx++) {
                    BlockPos pos;
                    switch (direction) {
                        case NORTH: pos = start.add(dx, dy, -i); break;
                        case SOUTH: pos = start.add(dx, dy, i); break;
                        case WEST:  pos = start.add(-i, dy, dx); break;
                        case EAST:  pos = start.add(i, dy, dx); break;
                        default:    pos = start.add(0, dy, i);
                    }
                    if (mc.world.isInBuildLimit(pos)) blocksToMine.add(pos);
                }
            }
        }
    }

    private void handleTunnelMining() {
        if (pendingPlacePos != null) {
            if (placeBlockFromOffhand(pendingPlacePos)) {
                pendingPlacePos = null;
            }
            return;
        }
        if (fillGaps.get()) {
            BlockPos placePos = checkGapsAndFluids();
            if (placePos != null && prepareBlockInOffhand()) {
                pendingPlacePos = placePos;
                return;
            }
        }
        if (blocksToMine.isEmpty() || currentBlockIndex >= blocksToMine.size()) {
            calculateTunnel();
            currentBlockIndex = 0;
            currentMiningPos = null;
        }
        if (currentBlockIndex >= blocksToMine.size()) { reset(); return; }
        BlockPos target = blocksToMine.get(currentBlockIndex);
        BlockState targetState = mc.world.getBlockState(target);
        if (targetState.isAir()) { currentBlockIndex++; return; }
        double distance = mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(target));
        if (distance < 1.0) {
            state = State.MINING;
            if (autoWalk.get()) { mc.options.forwardKey.setPressed(false); mc.options.jumpKey.setPressed(false); }
            if (autoBreak.get()) {
                if (currentMiningPos == null || !currentMiningPos.equals(target)) {
                    if (currentMiningPos != null) mc.interactionManager.cancelBlockBreaking();
                    mc.interactionManager.attackBlock(target, Direction.UP);
                    currentMiningPos = target;
                } else {
                    mc.interactionManager.updateBlockBreakingProgress(target, Direction.UP);
                }
                if (mc.world.getBlockState(target).isAir()) currentBlockIndex++;
            } else { currentBlockIndex++; }
        } else {
            state = State.MOVING;
            if (autoWalk.get()) {
                lookAt(Vec3d.ofCenter(target));
                mc.options.forwardKey.setPressed(true);
            }
            if (currentMiningPos != null) { mc.interactionManager.cancelBlockBreaking(); currentMiningPos = null; }
        }
    }

    private void reset() {
        if (autoWalk.get()) { mc.options.forwardKey.setPressed(false); mc.options.jumpKey.setPressed(false); }
        if (currentMiningPos != null) mc.interactionManager.cancelBlockBreaking();
        currentMiningPos = null;
        pendingPlacePos = null;
        collectTarget = null;
        collectItem = null;
        climbState = ClimbState.MINE;
        placeAttempts = 0;
        stableTicks = 0;
        skipJump = false;
        lastY = 0;
        currentTarget = null;
        currentTargetBlock = null;
        minedTargetBlock = null;
        blocksToMine.clear();
        pathCorners.clear();
        allTargets.clear();
        pathCornerIndex = 0;
        currentBlockIndex = 0;
        state = State.IDLE;
        isClimbing = false;
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || !enabled.get()) return;

        if (!blocksToMine.isEmpty()) {
            ShapeMode mode = ShapeMode.valueOf(shapeMode.getMode());
            Color side = sideColor.getCurrentColor();
            Color line = lineColor.getCurrentColor();
            double margin = 0.1;
            double size = 0.8;
            for (BlockPos pos : blocksToMine) {
                if (mc.world.getBlockState(pos).isAir()) continue;
                double x1 = pos.getX() + margin;
                double y1 = pos.getY() + margin;
                double z1 = pos.getZ() + margin;
                double x2 = x1 + size;
                double y2 = y1 + size;
                double z2 = z1 + size;
                event.render.drawBox(x1, y1, z1, x2, y2, z2, side, line, mode, 0);
            }
        }

        if (pendingPlacePos != null) {
            double x1 = pendingPlacePos.getX() + 0.1;
            double y1 = pendingPlacePos.getY() + 0.1;
            double z1 = pendingPlacePos.getZ() + 0.1;
            double x2 = x1 + 0.8;
            double y2 = y1 + 0.8;
            double z2 = z1 + 0.8;
            event.render.drawBox(x1, y1, z1, x2, y2, z2,
                    new Color(0, 0, 255, 50), new Color(0, 0, 255, 255), ShapeMode.Both, 0);
        }

        if (currentTarget != null) {
            // Piirrä kaikki targetit valkoisella (paitsi nykyinen)
            Color whiteBox = new Color(255, 255, 255, 50);
            Color whiteLine = new Color(255, 255, 255, 255);
            for (BlockPos pos : allTargets) {
                if (mc.world.getBlockState(pos).isAir()) continue; // ohitetaan jo kaivetut
                if (pos.equals(currentTarget)) continue; // piirretään erikseen
                double x1 = pos.getX() + 0.1;
                double y1 = pos.getY() + 0.1;
                double z1 = pos.getZ() + 0.1;
                double x2 = x1 + 0.8;
                double y2 = y1 + 0.8;
                double z2 = z1 + 0.8;
                event.render.drawBox(x1, y1, z1, x2, y2, z2, whiteBox, whiteLine, ShapeMode.Both, 0);
            }

            // Piirrä nykyinen target vihreällä
            Color greenBox = new Color(0, 255, 0, 50);
            Color greenLine = new Color(0, 255, 0, 255);
            double x1 = currentTarget.getX() + 0.1;
            double y1 = currentTarget.getY() + 0.1;
            double z1 = currentTarget.getZ() + 0.1;
            double x2 = x1 + 0.8;
            double y2 = y1 + 0.8;
            double z2 = z1 + 0.8;
            event.render.drawBox(x1, y1, z1, x2, y2, z2, greenBox, greenLine, ShapeMode.Both, 0);

            // Piirrä polku (kuten aiemmin)
            if (!pathCorners.isEmpty()) {
                Vec3d prev = Vec3d.ofCenter(pathCorners.get(0));
                for (int i = 1; i < pathCorners.size(); i++) {
                    Vec3d next = Vec3d.ofCenter(pathCorners.get(i));
                    event.render.drawLine(prev.x, prev.y, prev.z, next.x, next.y, next.z, greenLine);
                    prev = next;
                }
            }
        }
    }

    @Override protected void onEnable() {
        reset();
        if (!targetMode.get()) calculateTunnel();
        else { blocksToMine.clear(); pathCorners.clear(); }
    }
    @Override protected void onDisable() { reset(); }
    public boolean isActive() { return this.isEnabled(); }

    // BlockSelectable
    @Override public boolean isBlockSelected(Block block) { return targetBlocks.contains(Registries.BLOCK.getId(block)); }
    @Override public void toggleBlock(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        if (targetBlocks.contains(id)) targetBlocks.remove(id);
        else targetBlocks.add(id);
    }
    public void openBlockSelector() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new BlockSelectionView(this);
        factory.openCustomWindow("tunnelminer_blocks", "Select Target Blocks", sw, sh, content);
    }
}