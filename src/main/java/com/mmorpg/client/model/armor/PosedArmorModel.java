package com.mmorpg.client.model.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Rotations;

/** Deferred armor submissions animate again: preserve the stand's six authored poses at that stage. */
public final class PosedArmorModel extends HumanoidModel<HumanoidRenderState> {
    public PosedArmorModel(ModelPart root) { super(root); }
    private static void pose(ModelPart part, Rotations rotation) {
        part.xRot=rotation.x()*(float)Math.PI/180;
        part.yRot=rotation.y()*(float)Math.PI/180;
        part.zRot=rotation.z()*(float)Math.PI/180;
    }
    @Override public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if(state instanceof ArmorStandRenderState stand) {
            pose(head,stand.headPose);pose(body,stand.bodyPose);
            pose(leftArm,stand.leftArmPose);pose(rightArm,stand.rightArmPose);
            pose(leftLeg,stand.leftLegPose);pose(rightLeg,stand.rightLegPose);
            head.y=1;leftLeg.y=rightLeg.y=11;
        }
    }
}
