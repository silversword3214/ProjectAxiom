package silversword.axiom.client.modules.misc;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.LevelChunk;
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

import static net.minecraft.world.level.ClipContext.Block.OUTLINE;
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
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        return eyes.distanceTo(center) <= 4.8;
    }

    private boolean canSee(BlockPos pos) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);

        BlockHitResult result = mc.level.clip(
                new net.minecraft.world.level.ClipContext(
                        eyes,
                        target,
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        mc.player
                )
        );

        return result.getBlockPos().equals(pos);
    }


    private void findNearestTarget() {
        if (targetBlocks.isEmpty() || mc.player == null || mc.level == null) {
            currentTarget = null; blocksToMine.clear(); pathCorners.clear(); allTargets.clear(); return;
        }
        double maxDist = targetRange.getValue();
        double maxDistSq = maxDist * maxDist;
        Vec3 playerPos = mc.player.position();
        int radius = (int) Math.ceil(maxDist / 16) + 1;
        int chunkX = mc.player.chunkPosition().x;
        int chunkZ = mc.player.chunkPosition().z;

        allTargets.clear();
        List<BlockPos> candidates = new ArrayList<>();
        double bestDistSq = Double.MAX_VALUE;

        for (int cx = chunkX - radius; cx <= chunkX + radius; cx++) {
            for (int cz = chunkZ - radius; cz <= chunkZ + radius; cz++) {
                net.minecraft.world.level.chunk.ChunkAccess chunk = mc.level.getChunk(cx, cz);
                if (!(chunk instanceof LevelChunk worldChunk)) continue;
                int minY = mc.level.getMinY();
                int maxY = mc.level.getMaxY();
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        for (int dy = minY; dy < maxY; dy++) {
                            BlockPos pos = new BlockPos(cx * 16 + dx, dy, cz * 16 + dz);
                            double distSq = pos.distToCenterSqr(playerPos);
                            if (distSq > maxDistSq) continue;
                            BlockState state = worldChunk.getBlockState(pos);
                            if (state.isAir()) continue;
                            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                            if (targetBlocks.contains(id)) {
                                allTargets.add(pos.immutable());
                                if (distSq < bestDistSq) {
                                    bestDistSq = distSq;
                                    candidates.clear();
                                    candidates.add(pos.immutable());
                                } else if (distSq == bestDistSq) {
                                    candidates.add(pos.immutable());
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
            currentTargetBlock = mc.level.getBlockState(currentTarget).getBlock();
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

        BlockPos start = mc.player.blockPosition();
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
                    BlockPos head = feet.above();
                    if (isSolidAndMineable(feet)) blocksToMine.add(feet);
                    if (isSolidAndMineable(head)) blocksToMine.add(head);
                }
            } else if (z1 != z2) {
                int stepZ = Integer.signum(z2 - z1);
                for (int z = z1 + stepZ; z != z2 + stepZ; z += stepZ) {
                    BlockPos feet = new BlockPos(x1, y1, z);
                    BlockPos head = feet.above();
                    if (isSolidAndMineable(feet)) blocksToMine.add(feet);
                    if (isSolidAndMineable(head)) blocksToMine.add(head);
                }
            } else if (y1 != y2) {
                int stepY = Integer.signum(y2 - y1);
                for (int y = y1 + stepY; y != y2 + stepY; y += stepY) {
                    BlockPos feet = new BlockPos(x1, y, z1);
                    BlockPos head = feet.above();
                    if (isSolidAndMineable(feet)) blocksToMine.add(feet);
                    if (isSolidAndMineable(head)) blocksToMine.add(head);
                }
            }
        }

        if (!blocksToMine.contains(currentTarget) && isSolidAndMineable(currentTarget))
            blocksToMine.add(currentTarget);

        Vec3 playerPos = mc.player.position();
        blocksToMine.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(playerPos)));
        currentBlockIndex = 0;
    }

    private void handleTargeting() {
        if (currentTarget != null && mc.level.getBlockState(currentTarget).isAir()) {
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
                mc.options.keyUp.setDown(false);
                mc.options.keyJump.setDown(false);
            }
            lookAt(Vec3.atCenterOf(currentTarget));
            mineBlock(currentTarget);
            return;
        }

        // Tarkistetaan, tarvitaanko nousua
        boolean needClimb = false;
        int yDiff = 0;
        if (pathCornerIndex < pathCorners.size()) {
            BlockPos nextCorner = pathCorners.get(pathCornerIndex);
            yDiff = nextCorner.getY() - mc.player.blockPosition().getY();
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
                mc.options.keyUp.setDown(false);
                mc.options.keyJump.setDown(false);
            }
            return;
        }

        // Liikkuminen
        if (currentTarget != null) {
            Vec3 targetVec = Vec3.atCenterOf(currentTarget);
            double distance = mc.player.position().distanceTo(targetVec);

            if (distance < 1.25 && canSee(currentTarget)) {
                mineBlock(currentTarget);
                return;
            }

            if (pathCornerIndex < pathCorners.size()) {
                BlockPos nextCorner = pathCorners.get(pathCornerIndex);
                Vec3 cornerVec = Vec3.atCenterOf(nextCorner);
                double cornerDist = mc.player.position().distanceTo(cornerVec);

                if (cornerDist < 0.5) {
                    pathCornerIndex++;
                    if (pathCornerIndex < pathCorners.size()) {
                        nextCorner = pathCorners.get(pathCornerIndex);
                        cornerVec = Vec3.atCenterOf(nextCorner);
                    } else {
                        nextCorner = currentTarget;
                        cornerVec = Vec3.atCenterOf(nextCorner);
                    }
                }

                state = State.MOVING;
                if (autoWalk.get()) {
                    lookAt(cornerVec);
                    Direction facing = mc.player.getDirection();
                    BlockPos playerFeet = mc.player.blockPosition();
                    BlockPos nextFeet = playerFeet.relative(facing);
                    boolean needJump = nextFeet.getY() > playerFeet.getY() && !skipJump;
                    if (needJump) mc.options.keyJump.setDown(true);
                    else mc.options.keyJump.setDown(false);
                    mc.options.keyUp.setDown(true);
                }
            } else {
                state = State.MOVING;
                if (autoWalk.get()) {
                    lookAt(targetVec);
                    mc.options.keyUp.setDown(true);
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
        for (ItemEntity item : mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(targetRange.getValue()), e -> true)) {
            ItemStack stack = item.getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            BlockItem blockItem = (BlockItem) stack.getItem();
            if (blockItem.getBlock() == minedTargetBlock) {
                double dist = mc.player.distanceTo(item);
                if (dist < closestDist) {
                    closestDist = dist;
                    collectItem = item;
                    collectTarget = item.blockPosition();
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
                mc.options.keyUp.setDown(false);
                mc.options.keyJump.setDown(false);
            }
            return;
        }

        Vec3 itemPos = collectItem.position();
        double distance = mc.player.position().distanceTo(itemPos);

        if (distance < 1.5) {
            // Lähellä, pysäytetään liike ja odotetaan että item katoaa (kerätään automaattisesti)
            if (autoWalk.get()) {
                mc.options.keyUp.setDown(false);
                mc.options.keyJump.setDown(false);
            }
            return;
        }

        // Liiku kohti itemiä
        if (autoWalk.get()) {
            lookAt(itemPos);
            mc.options.keyUp.setDown(true);
        }
    }

    private void handleClimbing() {
        // Pysäytetään vaakaliike
        if (autoWalk.get()) {
            mc.options.keyUp.setDown(false);
        }

        BlockPos playerFeet = mc.player.blockPosition();
        BlockPos aboveHead = playerFeet.above(2);
        BlockPos below = playerFeet.below();

        // Lasketaan korkeusero seuraavaan kulmapisteeseen
        int targetY = pathCorners.get(pathCornerIndex).getY();
        int currentY = playerFeet.getY();
        int yDiff = targetY - currentY;

        switch (climbState) {
            case MINE:
                // Kaivetaan yläpuolinen lohko, jos se on olemassa ja ulottuvilla
                if (isSolidAndMineable(aboveHead) && isWithinReach(aboveHead)) {
                    state = State.MINING;
                    lookAt(Vec3.atCenterOf(aboveHead));
                    mineBlock(aboveHead);
                    return;
                }
                // Jos yläpuoli on tyhjä, siirry hypyn odotukseen
                climbState = ClimbState.JUMP;
                // putoa läpi, jotta hypätään heti seuraavalla tikillä
                break;

            case JUMP:
                if (mc.player.onGround()) {
                    mc.options.keyJump.setDown(true);
                    // Alustetaan place-yritykset ja vakauslaskuri
                    placeAttempts = 0;
                    stableTicks = 0;
                    lastY = mc.player.blockPosition().getY();
                    climbState = ClimbState.PLACE;
                }
                return;

            case PLACE:
                mc.options.keyJump.setDown(false);

                // Yritä asettaa blokkia alapuolelle
                placeBlockFromOffhand(below);

                // Tarkista korkeuden muutos
                int thisY = mc.player.blockPosition().getY();
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
            if (mc.level.getBlockState(below).isAir()) {
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
            mc.options.keyJump.setDown(false);
        }
    }

    private boolean placeBlockFromOffhand(BlockPos pos) {
        // Jos paikalla ei ole ilmaa, ei tarvitse placea
        if (!mc.level.getBlockState(pos).isAir()) return true;

        // Varmista offhand sisältö (kutsu prepareBlockInOffhand() ennen metodia)
        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem)) return false;

        // Priorisoi suunnat: ensin DOWN (alla), sitten sivut, lopuksi UP
        Direction[] preferred = new Direction[] {
                Direction.DOWN,
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST,
                Direction.UP
        };

        // Tallenna vanha katselukulma, jos haluat palauttaa sen myöhemmin
        float oldYaw = mc.player.getYRot();
        float oldPitch = mc.player.getXRot();

        for (Direction dir : preferred) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = mc.level.getBlockState(neighbor);

            // Tarvitsemme solidin naapurilohkon, josta voi klikata pos:in pintaa
            // isSideSolidFullSquare varmistaa että pintaa voi käyttää placementiin
            if (neighborState.isAir() || !neighborState.isFaceSturdy(mc.level, neighbor, dir.getOpposite()))
                continue;

            // Hit-face on naapurin puoli joka osoittaa kohti target-paikkaa
            Direction hitFace = dir.getOpposite();

            // Lasketaan klikattava piste naapurin pinnalle.
            // Esim. jos hitFace == UP, halutaan klikata naapurin yläpintaa (keskellä y + 0.5)
            Vec3 neighborCenter = Vec3.atCenterOf(neighbor);
            Vec3 hitPos = neighborCenter.add(
                    hitFace.getStepX() * 0.45,
                    hitFace.getStepY() * 0.45,
                    hitFace.getStepZ() * 0.45
            );

            // Käännä nopeasti katseen kohti klikattavaa pistettä (tarpeellinen modeissa joissa server vaatii)
            lookAt(hitPos);

            // Luo BlockHitResult, käyttäen naapurin pos:ia ja osumaa sen pintaan
            BlockHitResult bhr = new BlockHitResult(hitPos, hitFace, neighbor, false);

            // Yritä sijoittaa offhandilla
            InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, bhr);

            // Vaihtoehtoisesti joillain versioilla voi tarvita interactItem tai swing ennen/ jälkeen;
            // tässä swingataan jos sijoitus hyväksyttiin.
            if (result.consumesAction()) {
                mc.player.swing(InteractionHand.OFF_HAND);
                // Palauta kamera vanhaksi (valinnainen)
                mc.player.setYRot(oldYaw);
                mc.player.setXRot(oldPitch);
                return true;
            }
            // jos ei hyväksytty, kokeillaan seuraavaa naapuria
        }

        // Jos ei löytynyt sopivaa naapuria tai sijoitus epäonnistui
        // Palauta katselukulma
        mc.player.setYRot(oldYaw);
        mc.player.setXRot(oldPitch);
        return false;
    }

    private boolean mineNormalBlocks() {
        if (currentMiningPos != null) {
            BlockState state = mc.level.getBlockState(currentMiningPos);
            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                mineBlock(currentMiningPos);
                return true;
            } else {
                currentMiningPos = null;
            }
        }

        Direction facing = mc.player.getDirection();
        BlockPos playerFeet = mc.player.blockPosition();

        // Tarkistetaan, vaatiiko seuraava kulmapiste alaspäin liikettä
        boolean needDescend = false;
        int yDiff = 0;
        if (pathCornerIndex < pathCorners.size()) {
            BlockPos nextCorner = pathCorners.get(pathCornerIndex);
            yDiff = nextCorner.getY() - playerFeet.getY();
            if (yDiff < 0) needDescend = true;
        }

        if (needDescend) {
            BlockPos below = playerFeet.below();
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
                    Direction dir = Direction.getNearest(new BlockPos(dx, 0, dz), null);
                    if (dir != null && dir == facing) {
                        BlockPos frontBlock = playerFeet.relative(facing);
                        if (isSolidAndMineable(frontBlock)) {
                            mineBlock(frontBlock);
                            return true;
                        }
                    }
                }
            }
        }

        // Vaakasuuntaiset lohkot (edessä)
        BlockPos frontFeet = playerFeet.relative(facing);
        if (blocksToMine.contains(frontFeet) && isSolidAndMineable(frontFeet)) {
            mineBlock(frontFeet);
            return true;
        }

        BlockPos frontHead = frontFeet.above();
        if (blocksToMine.contains(frontHead) && isSolidAndMineable(frontHead)) {
            mineBlock(frontHead);
            return true;
        }

        return false;
    }

    private boolean isSolidAndMineable(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && state.getBlock() != Blocks.BEDROCK;
    }

    private void mineBlock(BlockPos pos) {
        if (!autoBreak.get()) return;
        if (currentMiningPos == null || !currentMiningPos.equals(pos)) {
            if (currentMiningPos != null) mc.gameMode.stopDestroyBlock();
            mc.gameMode.startDestroyBlock(pos, Direction.UP);
            currentMiningPos = pos;
        } else {
            mc.gameMode.continueDestroyBlock(pos, Direction.UP);
        }
    }

    private void lookAt(Vec3 target) {
        Vec3 playerPos = mc.player.getEyePosition();
        double dx = target.x - playerPos.x;
        double dy = target.y - playerPos.y;
        double dz = target.z - playerPos.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double pitch = Math.toDegrees(-Math.asin(dy / dist));
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setYRot((float) yaw);
        mc.player.setXRot((float) pitch);
    }

    private BlockPos checkGapsAndFluids() {
        Direction direction = mc.player.getDirection();
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos nextPos = playerPos.relative(direction);
        BlockPos belowNextPos = nextPos.below();
        if (alsoPlaceBelow.get() && mc.level.getBlockState(belowNextPos).isAir()) return belowNextPos;
        BlockState nextState = mc.level.getBlockState(nextPos);
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
        int syncId = mc.player.inventoryMenu.containerId;
        if (mc.player.getOffhandItem().getItem() instanceof BlockItem &&
                ((BlockItem) mc.player.getOffhandItem().getItem()).getBlock() == targetBlock) return true;
        int offhandSlotId = 45;
        int fromSlotId = invIndexToPlayerScreenSlotId(slot);
        mc.gameMode.handleInventoryMouseClick(syncId, fromSlotId, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
        mc.gameMode.handleInventoryMouseClick(syncId, offhandSlotId, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
        if (!mc.player.containerMenu.getCarried().isEmpty())
            mc.gameMode.handleInventoryMouseClick(syncId, fromSlotId, 0, ClickType.PICKUP, mc.player);
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
            ItemStack stack = mc.player.getInventory().getItem(i);
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
        if (mc.player == null || mc.level == null) return;
        Direction direction = mc.player.getDirection();
        int len = (int) length.getValue();
        int h = (int) height.getValue();
        int w = (int) width.getValue();
        BlockPos start = mc.player.blockPosition();
        for (int i = 1; i <= len; i++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dx = - (w / 2); dx <= (w / 2); dx++) {
                    BlockPos pos;
                    switch (direction) {
                        case NORTH: pos = start.offset(dx, dy, -i); break;
                        case SOUTH: pos = start.offset(dx, dy, i); break;
                        case WEST:  pos = start.offset(-i, dy, dx); break;
                        case EAST:  pos = start.offset(i, dy, dx); break;
                        default:    pos = start.offset(0, dy, i);
                    }
                    if (mc.level.isInWorldBounds(pos)) blocksToMine.add(pos);
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
        BlockState targetState = mc.level.getBlockState(target);
        if (targetState.isAir()) { currentBlockIndex++; return; }
        double distance = mc.player.position().distanceTo(Vec3.atCenterOf(target));
        if (distance < 1.0) {
            state = State.MINING;
            if (autoWalk.get()) { mc.options.keyUp.setDown(false); mc.options.keyJump.setDown(false); }
            if (autoBreak.get()) {
                if (currentMiningPos == null || !currentMiningPos.equals(target)) {
                    if (currentMiningPos != null) mc.gameMode.stopDestroyBlock();
                    mc.gameMode.startDestroyBlock(target, Direction.UP);
                    currentMiningPos = target;
                } else {
                    mc.gameMode.continueDestroyBlock(target, Direction.UP);
                }
                if (mc.level.getBlockState(target).isAir()) currentBlockIndex++;
            } else { currentBlockIndex++; }
        } else {
            state = State.MOVING;
            if (autoWalk.get()) {
                lookAt(Vec3.atCenterOf(target));
                mc.options.keyUp.setDown(true);
            }
            if (currentMiningPos != null) { mc.gameMode.stopDestroyBlock(); currentMiningPos = null; }
        }
    }

    private void reset() {
        if (autoWalk.get()) { mc.options.keyUp.setDown(false); mc.options.keyJump.setDown(false); }
        if (currentMiningPos != null) mc.gameMode.stopDestroyBlock();
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
                if (mc.level.getBlockState(pos).isAir()) continue;
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
                if (mc.level.getBlockState(pos).isAir()) continue; // ohitetaan jo kaivetut
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
                Vec3 prev = Vec3.atCenterOf(pathCorners.get(0));
                for (int i = 1; i < pathCorners.size(); i++) {
                    Vec3 next = Vec3.atCenterOf(pathCorners.get(i));
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
    @Override public boolean isBlockSelected(Block block) { return targetBlocks.contains(BuiltInRegistries.BLOCK.getKey(block)); }
    @Override public void toggleBlock(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (targetBlocks.contains(id)) targetBlocks.remove(id);
        else targetBlocks.add(id);
    }
    public void openBlockSelector() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new BlockSelectionView(this);
        factory.openCustomWindow("tunnelminer_blocks", "Select Target Blocks", sw, sh, content);
    }
}