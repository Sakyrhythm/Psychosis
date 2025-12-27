package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.animation.ScytheAnimation;
import com.sakyrhythm.psychosis.entity.custom.ScytheBossEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AnimationState;
import org.joml.Vector3f;

public class ScytheModel <T extends ScytheBossEntity> extends SinglePartEntityModel<T> {
    private final Vector3f tempVec = new Vector3f();
    private final ModelPart bone6;
    private final ModelPart bone2;
    private final ModelPart bone5;
    private final ModelPart bone3;
    private final ModelPart bone;
    private final ModelPart bone4;
    public ScytheModel(ModelPart root) {
        this.bone6 = root.getChild("bone6");
        this.bone2 = this.bone6.getChild("bone2");
        this.bone5 = this.bone2.getChild("bone5");
        this.bone3 = this.bone6.getChild("bone3");
        this.bone = this.bone3.getChild("bone");
        this.bone4 = this.bone.getChild("bone4");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bone6 = modelPartData.addChild("bone6", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData bone2 = bone6.addChild("bone2", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

        ModelPartData cube_r1 = bone2.addChild("cube_r1", ModelPartBuilder.create().uv(0, 46).cuboid(-0.5F, -1.5F, -1.5F, 1.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-1.2745F, 1.0066F, -0.5F, 0.0F, 0.0F, -0.8727F));

        ModelPartData cube_r2 = bone2.addChild("cube_r2", ModelPartBuilder.create().uv(24, 46).cuboid(-0.5F, -1.4F, -1.5F, 1.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(1.7255F, 0.0F, -0.5F, 0.0F, 0.0F, -0.0873F));

        ModelPartData cube_r3 = bone2.addChild("cube_r3", ModelPartBuilder.create().uv(48, 0).cuboid(1.0F, -4.0F, -2.0F, 1.0F, 4.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.6981F));

        ModelPartData cube_r4 = bone2.addChild("cube_r4", ModelPartBuilder.create().uv(46, 49).cuboid(1.0F, -3.0F, -2.0F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F))
                .uv(50, 25).cuboid(-2.0F, -3.0F, -3.0F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F))
                .uv(16, 50).cuboid(-2.0F, -3.0F, 1.0F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-2.0F, -36.0F, -2.0F, 3.0F, 36.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));

        ModelPartData cube_r5 = bone2.addChild("cube_r5", ModelPartBuilder.create().uv(48, 33).cuboid(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, -3.0F, 1.9199F, 0.0F, -0.5236F));

        ModelPartData cube_r6 = bone2.addChild("cube_r6", ModelPartBuilder.create().uv(44, 20).cuboid(-1.0F, -0.05F, -2.3F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 1.2217F, 0.0F, -0.5236F));

        ModelPartData bone5 = bone2.addChild("bone5", ModelPartBuilder.create(), ModelTransform.pivot(-8.9731F, -17.9269F, -3.3317F));

        ModelPartData cube_r7 = bone5.addChild("cube_r7", ModelPartBuilder.create().uv(54, 47).cuboid(-0.8149F, -3.5286F, 6.4016F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-4.4503F, 0.8062F, 0.7594F, -1.962F, 0.9599F, 2.7015F));

        ModelPartData cube_r8 = bone5.addChild("cube_r8", ModelPartBuilder.create().uv(55, 42).cuboid(0.5F, 1.5F, -2.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-1.3494F, -5.9898F, 1.6887F, 1.3023F, 0.2815F, -0.2061F));

        ModelPartData cube_r9 = bone5.addChild("cube_r9", ModelPartBuilder.create().uv(54, 42).cuboid(-0.5F, -1.5F, -1.0F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-0.8494F, -5.9898F, 1.6887F, 1.2792F, 0.2574F, -0.2931F));

        ModelPartData cube_r10 = bone5.addChild("cube_r10", ModelPartBuilder.create().uv(32, 50).cuboid(2.3788F, -4.6207F, 4.86F, 1.0F, 4.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-5.003F, -3.3896F, 6.2997F, -2.3077F, -0.7098F, -3.0048F));

        ModelPartData cube_r11 = bone5.addChild("cube_r11", ModelPartBuilder.create().uv(22, 54).cuboid(1.8291F, -4.6286F, 4.8127F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-5.853F, -1.8896F, 4.5497F, -2.1321F, -0.189F, 3.0509F));

        ModelPartData cube_r12 = bone5.addChild("cube_r12", ModelPartBuilder.create().uv(54, 20).cuboid(0.4389F, -4.7532F, 5.1266F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-5.453F, -0.6396F, 2.2497F, -2.0108F, 0.3414F, 2.8481F));

        ModelPartData cube_r13 = bone5.addChild("cube_r13", ModelPartBuilder.create().uv(54, 52).cuboid(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-2.8595F, -7.1758F, -0.7377F, 0.902F, 1.1878F, -0.6159F));

        ModelPartData cube_r14 = bone5.addChild("cube_r14", ModelPartBuilder.create().uv(54, 52).cuboid(-1.0462F, -2.6449F, 6.3481F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-3.2003F, -0.1938F, -3.2406F, -0.6279F, 1.2804F, -2.2024F));

        ModelPartData cube_r15 = bone5.addChild("cube_r15", ModelPartBuilder.create().uv(54, 52).cuboid(-1.0462F, -2.6449F, 6.3481F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-3.2003F, 0.8062F, -0.4906F, -2.1496F, 1.3685F, 2.5293F));

        ModelPartData cube_r16 = bone5.addChild("cube_r16", ModelPartBuilder.create().uv(16, 54).cuboid(-0.5F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-2.9583F, -6.3442F, -0.25F, 1.4896F, 1.1962F, -0.008F));

        ModelPartData bone3 = bone6.addChild("bone3", ModelPartBuilder.create(), ModelTransform.of(-0.3838F, -33.3437F, -2.3692F, -0.3008F, 0.0831F, -0.0261F));

        ModelPartData bone = bone3.addChild("bone", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.2618F, 0.0F, 0.4363F));

        ModelPartData cube_r17 = bone.addChild("cube_r17", ModelPartBuilder.create().uv(26, 26).cuboid(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-0.6142F, -1.0066F, -5.4117F, -1.0036F, 0.0F, -0.4363F));

        ModelPartData cube_r18 = bone.addChild("cube_r18", ModelPartBuilder.create().uv(26, 26).cuboid(-0.6312F, 6.5031F, -3.9265F, 1.0F, 10.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -1.1345F, 0.0F, -0.4363F));

        ModelPartData cube_r19 = bone.addChild("cube_r19", ModelPartBuilder.create().uv(8, 46).cuboid(-0.6312F, 16.2F, -2.9637F, 1.0F, 3.0F, 1.0F, new Dilation(0.0F))
                .uv(16, 13).cuboid(-0.6312F, 1.2F, -7.9637F, 1.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(15, 13).cuboid(-1.6312F, 1.2F, -6.9637F, 3.0F, 4.0F, 1.0F, new Dilation(0.0F))
                .uv(16, 13).cuboid(-0.6312F, 5.2F, -6.9637F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F))
                .uv(13, 11).cuboid(-1.6312F, 1.2F, -5.9637F, 3.0F, 13.0F, 3.0F, new Dilation(0.0F))
                .uv(30, 25).cuboid(-2.6312F, 0.2F, -8.9637F, 4.0F, 1.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -1.1345F, 0.0F, -0.4363F));

        ModelPartData cube_r20 = bone.addChild("cube_r20", ModelPartBuilder.create().uv(12, 55).cuboid(-0.6312F, -3.7239F, -7.2831F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(6.2502F, 13.3675F, -17.6331F, -0.7854F, 0.0F, -0.4363F));

        ModelPartData cube_r21 = bone.addChild("cube_r21", ModelPartBuilder.create().uv(38, 55).cuboid(-0.6312F, 20.2851F, -5.9955F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(28, 54).cuboid(-0.6312F, 15.2851F, -4.9955F, 1.0F, 3.0F, 1.0F, new Dilation(0.0F))
                .uv(8, 50).cuboid(-0.6312F, 18.2851F, -6.9955F, 1.0F, 2.0F, 3.0F, new Dilation(0.0F))
                .uv(50, 29).cuboid(-0.6312F, 17.2851F, -7.9955F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(12, 43).cuboid(-1.6312F, 13.2851F, -7.9955F, 3.0F, 4.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -1.0036F, 0.0F, -0.4363F));

        ModelPartData cube_r22 = bone.addChild("cube_r22", ModelPartBuilder.create().uv(20, 27).cuboid(2.2547F, -7.8F, -8.2341F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(1.448F, 7.5721F, -6.1776F, -1.1523F, 0.1274F, -0.7147F));

        ModelPartData cube_r23 = bone.addChild("cube_r23", ModelPartBuilder.create().uv(12, 27).cuboid(-2.4291F, -7.8F, -8.1924F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(3.198F, 6.3221F, -6.6776F, -1.1476F, -0.1096F, -0.1981F));

        ModelPartData cube_r24 = bone.addChild("cube_r24", ModelPartBuilder.create().uv(38, 50).cuboid(-0.6312F, -3.0945F, -7.0001F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-1.4504F, -3.1104F, 7.2044F, -0.6545F, 0.0F, -0.4363F));

        ModelPartData cube_r25 = bone.addChild("cube_r25", ModelPartBuilder.create().uv(6, 55).cuboid(-0.5798F, -6.4009F, 11.0448F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.1003F, -0.7126F, 4.8692F, 1.2423F, -0.2409F, -0.2467F));

        ModelPartData cube_r26 = bone.addChild("cube_r26", ModelPartBuilder.create().uv(32, 42).cuboid(-0.5798F, -7.7784F, 4.6004F, 1.0F, 2.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(0.1003F, -0.7126F, 4.8692F, 1.1551F, -0.2409F, -0.2467F));

        ModelPartData cube_r27 = bone.addChild("cube_r27", ModelPartBuilder.create().uv(32, 33).cuboid(-0.9987F, -8.6598F, -1.306F, 1.0F, 2.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.1003F, -0.7126F, 4.8692F, 0.868F, -0.1296F, -0.1098F));

        ModelPartData cube_r28 = bone.addChild("cube_r28", ModelPartBuilder.create().uv(34, 0).cuboid(1.3688F, -9.4955F, -3.2851F, 1.0F, 2.0F, 6.0F, new Dilation(0.0F))
                .uv(12, 0).cuboid(-1.6312F, -9.4955F, -3.2851F, 3.0F, 2.0F, 8.0F, new Dilation(0.0F)), ModelTransform.of(0.6003F, 1.2874F, 4.8692F, 0.5672F, 0.0F, -0.4363F));

        ModelPartData cube_r29 = bone.addChild("cube_r29", ModelPartBuilder.create().uv(44, 14).cuboid(-1.6312F, -7.5154F, -5.0774F, 3.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.6003F, 1.2874F, 4.8692F, 0.0F, 0.0F, -0.4363F));

        ModelPartData cube_r30 = bone.addChild("cube_r30", ModelPartBuilder.create().uv(44, 8).cuboid(-1.6312F, -8.3762F, -4.8043F, 3.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.6003F, 1.2874F, 4.8692F, 0.3491F, 0.0F, -0.4363F));

        ModelPartData cube_r31 = bone.addChild("cube_r31", ModelPartBuilder.create().uv(0, 55).cuboid(-0.6312F, -8.5154F, -5.0774F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(26, 10).cuboid(-1.6312F, -7.5154F, -8.0774F, 3.0F, 2.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(1.023F, 2.1937F, 7.8692F, 0.0F, 0.0F, -0.4363F));

        ModelPartData cube_r32 = bone.addChild("cube_r32", ModelPartBuilder.create().uv(48, 38).cuboid(-1.6312F, -4.6512F, -5.7492F, 3.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(1.023F, 2.1937F, 7.8692F, -0.5672F, 0.0F, -0.4363F));

        ModelPartData cube_r33 = bone.addChild("cube_r33", ModelPartBuilder.create().uv(26, 18).cuboid(-1.6312F, -4.9291F, -7.919F, 3.0F, 1.0F, 6.0F, new Dilation(0.0F))
                .uv(27, 19).cuboid(-1.6312F, -3.9291F, -7.919F, 3.0F, 1.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(1.023F, 2.1937F, 7.8692F, -0.3491F, 0.0F, -0.4363F));

        ModelPartData bone4 = bone.addChild("bone4", ModelPartBuilder.create(), ModelTransform.pivot(0.1003F, -0.7126F, 4.8692F));

        ModelPartData cube_r34 = bone4.addChild("cube_r34", ModelPartBuilder.create().uv(0, 39).cuboid(-1.5798F, -8.6598F, 0.0945F, 1.0F, 2.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.8933F, -0.2409F, -0.2467F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    public void updateAnimation(AnimationState animationState, Animation animation, float animationProgress, float speed) {
        AnimationHelper.animate(this, animation, animationState.getTimeRunning(), speed, this.tempVec);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // 1. 重置所有零件的变换（核心：防止每一帧叠加）
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        // 2. 应用静态动画
        this.updateAnimation(entity.animationState, ScytheAnimation.ANIMATION, animationProgress, 1.0F);
    }

    @Override
    public ModelPart getPart() {
        return this.bone6;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        float scale = 1.25F;
        matrices.scale(scale, 1.5f, scale);
        // 不需要再写 matrices.translate/scale，因为 updateAnimation 已经修改了 bone 和 bone5 的参数
        this.bone6.render(matrices, vertexConsumer, light, overlay, color);
    }
}
