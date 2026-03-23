package silversword.axiom.client.modules.moduleutils;

import net.minecraft.world.level.block.Block;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

public interface BlockColorSelectable extends BlockSelectable {
    SettingColor getBlockColor(Block block);
    void setBlockColor(Block block, SettingColor color);
}