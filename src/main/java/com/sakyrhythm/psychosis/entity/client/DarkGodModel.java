package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.DarkGodEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class DarkGodModel extends EntityModel<DarkGodEntity> {

    // *** 确保这个值与服务器端 DarkGodEntity.SWIPE_DURATION 匹配 ***
    private static final int SWIPE_DURATION = 20;

    // 抬手动画的关键时间点（归一化进度 0.0 到 1.0）
    private static final float LIFT_START_PROGRESS = 0.25F;
    private static final float SWIPE_POINT_PROGRESS = 0.50F;

    // 最大抬手角度 (向后抬起 140 度，弧度制)
    private static final float MAX_LIFT_ANGLE = (float)Math.toRadians(140.0);


    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public DarkGodModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild(EntityModelPartNames.HEAD);
        this.body = root.getChild(EntityModelPartNames.BODY);
        this.rightArm = body.getChild(EntityModelPartNames.RIGHT_ARM);
        this.leftArm = body.getChild(EntityModelPartNames.LEFT_ARM);
        this.rightLeg = root.getChild(EntityModelPartNames.RIGHT_LEG);
        this.leftLeg = root.getChild(EntityModelPartNames.LEFT_LEG);
    }

    public static TexturedModelData getTexturedModelData() {
        // ... (getTexturedModelData 方法保持不变) ...
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        modelPartData.addChild(EntityModelPartNames.HEAD,
                ModelPartBuilder.create().cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).uv(0, 0),
                net.minecraft.client.model.ModelTransform.pivot(0.0F, 2.0F, 0.0F));

        ModelPartData bodyPart = modelPartData.addChild(EntityModelPartNames.BODY,
                ModelPartBuilder.create().cuboid(-5.0F, 0.0F, -3.0F, 10.0F, 18.0F, 6.0F).uv(16, 16),
                net.minecraft.client.model.ModelTransform.pivot(0.0F, 2.0F, 0.0F));

        bodyPart.addChild(EntityModelPartNames.RIGHT_ARM,
                ModelPartBuilder.create().cuboid(-4.0F, -2.0F, -2.0F, 4.0F, 18.0F, 4.0F).uv(40, 16),
                net.minecraft.client.model.ModelTransform.pivot(-5.0F, 2.0F, 0.0F));

        bodyPart.addChild(EntityModelPartNames.LEFT_ARM,
                ModelPartBuilder.create().cuboid(0.0F, -2.0F, -2.0F, 4.0F, 18.0F, 4.0F).uv(40, 16).mirrored(true),
                net.minecraft.client.model.ModelTransform.pivot(5.0F, 2.0F, 0.0F));

        modelPartData.addChild(EntityModelPartNames.RIGHT_LEG,
                ModelPartBuilder.create().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F).uv(0, 16),
                net.minecraft.client.model.ModelTransform.pivot(-2.0F, 10.0F, 0.0F));

        modelPartData.addChild(EntityModelPartNames.LEFT_LEG,
                ModelPartBuilder.create().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F).uv(0, 16).mirrored(true),
                net.minecraft.client.model.ModelTransform.pivot(2.0F, 10.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    /**
     * @Override: 修正 render 方法签名，适应新的 int color 参数 (包含 RGB 和 Alpha)
     */
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        this.root.render(matrices, vertices, light, overlay, color);
    }

    @Override
    public void setAngles(DarkGodEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        // 1. 重置所有部位的旋转 (重要)
        this.head.pitch = 0.0F; this.head.yaw = 0.0F;
        this.rightArm.pitch = 0.0F; this.rightArm.roll = 0.0F;
        this.leftArm.pitch = 0.0F; this.leftArm.roll = 0.0F;
        this.rightLeg.pitch = 0.0F;
        this.leftLeg.pitch = 0.0F;

        // 头部转动
        this.head.pitch = headPitch * ((float)Math.PI / 180F);
        this.head.yaw = netHeadYaw * ((float)Math.PI / 180F);

        if (entity.isParticlized() || entity.isAppearing()) {
            return;
        }

        // 2. 走路动画 (只有在 IDLE 状态下才启用，防止与攻击动画冲突)
        if (entity.getAttackState() == DarkGodEntity.AttackState.IDLE) {
            this.rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftLeg.pitch = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;

            this.rightArm.pitch = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.8F * limbSwingAmount;
            this.leftArm.pitch = MathHelper.cos(limbSwing * 0.6662F) * 0.8F * limbSwingAmount;
        }

        // 3. 攻击动画覆盖 (半月斩)
        if (entity.getAttackState() == DarkGodEntity.AttackState.SWIPE) {

            int attackTimer = entity.getAttackTimer();
            // **修正 LimbAnimator 错误：使用 ageInTicks 进行平滑**
            float progressTicks = (float)(SWIPE_DURATION - Math.max(1, attackTimer)) + ageInTicks - (entity.age);

            float normalizedProgress = progressTicks / (float)SWIPE_DURATION;

            if (normalizedProgress >= LIFT_START_PROGRESS && normalizedProgress <= SWIPE_POINT_PROGRESS) {

                float liftProgress = (normalizedProgress - LIFT_START_PROGRESS) / (SWIPE_POINT_PROGRESS - LIFT_START_PROGRESS);

                this.rightArm.pitch = MathHelper.lerp(liftProgress, 0.0F, -MAX_LIFT_ANGLE * 0.75f);
                this.rightArm.roll = MathHelper.lerp(liftProgress, 0.0F, -MAX_LIFT_ANGLE * 0.5f);

                this.leftArm.pitch = MathHelper.lerp(liftProgress, 0.0F, MAX_LIFT_ANGLE * 0.5f);

            }
            else if (normalizedProgress > SWIPE_POINT_PROGRESS && normalizedProgress <= 1.0F) {

                float recoveryProgress = (normalizedProgress - SWIPE_POINT_PROGRESS) / (1.0F - SWIPE_POINT_PROGRESS);

                this.rightArm.pitch = MathHelper.lerp(recoveryProgress, -MAX_LIFT_ANGLE * 0.75f, 0.0F);
                this.rightArm.roll = MathHelper.lerp(recoveryProgress, -MAX_LIFT_ANGLE * 0.5f, 0.0F);

                this.leftArm.pitch = MathHelper.lerp(recoveryProgress, MAX_LIFT_ANGLE * 0.5f, 0.0F);

            } else {
                // ... (保持默认姿势) ...
            }
        }
    }
}