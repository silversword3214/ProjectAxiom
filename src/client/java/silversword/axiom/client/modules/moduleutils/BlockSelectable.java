package silversword.axiom.client.modules.moduleutils;

import net.minecraft.block.Block;

public interface BlockSelectable {
    void toggleBlock(Block block);
    boolean isBlockSelected(Block block);
}