package silversword.axiom.client.modules.moduleutils;

import net.minecraft.world.level.block.Block;

public interface BlockSelectable {
    void toggleBlock(Block block);
    boolean isBlockSelected(Block block);
}