package silversword.axiom.client.modules.player;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.entity.PreEntityMoveEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;

public final class AntiCactus extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    // Lisätään moodit: "Solid" (Mixin-pohjainen) ja "Prevent" (Event-pohjainen)
    public final SettingMode mode = new SettingMode("Mode", new String[]{"Solid", "Prevent"}, "Solid");

    public AntiCactus() {
        super("Anti Cactus", "Prevents damage from cacti", ModuleCategory.PLAYER);
        addSetting(mode);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Subscribe
    public void onPreMove(PreEntityMoveEvent event) {
        // Suoritetaan vain jos moduuli on päällä JA moodi on "Prevent"
        if (!isEnabled() || !mode.getMode().equals("Prevent")) return;

        if (!(event.getEntity() instanceof Player player)) return;
        if (player != AxiomInitialize.mc.player) return;

        Level level = player.level();
        AABB currentBox = player.getBoundingBox();

        if (collidesWithCactus(currentBox, level)) return;

        Vec3 movement = event.getMovement();
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;

        if (collidesWithCactus(currentBox.move(x, 0, 0), level)) x = 0;
        AABB boxAfterX = currentBox.move(x, 0, 0);

        if (collidesWithCactus(boxAfterX.move(0, 0, z), level)) z = 0;
        AABB boxAfterXZ = boxAfterX.move(0, 0, z);

        if (collidesWithCactus(boxAfterXZ.move(0, y, 0), level)) y = 0;

        if (x != movement.x || y != movement.y || z != movement.z) {
            event.setMovement(new Vec3(x, y, z));
        }
    }

    private boolean collidesWithCactus(AABB box, Level level) {
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.CACTUS)) {
                        if (box.intersects(new AABB(pos))) return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void onTick() {

    }
}