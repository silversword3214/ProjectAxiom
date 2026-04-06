package silversword.axiom.client.utils.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;

public class CapturedModelState {
    public static class PartState {
        public float xRot, yRot, zRot;
        public float x, y, z; // pivot

        public PartState(ModelPart part) {
            this.xRot = part.xRot;
            this.yRot = part.yRot;
            this.zRot = part.zRot;
            this.x = part.x;
            this.y = part.y;
            this.z = part.z;
        }
    }

    public PartState head, body, leftArm, rightArm, leftLeg, rightLeg;

    public CapturedModelState(PlayerModel model) {
        head = new PartState(model.head);
        body = new PartState(model.body);
        leftArm = new PartState(model.leftArm);
        rightArm = new PartState(model.rightArm);
        leftLeg = new PartState(model.leftLeg);
        rightLeg = new PartState(model.rightLeg);
    }
}