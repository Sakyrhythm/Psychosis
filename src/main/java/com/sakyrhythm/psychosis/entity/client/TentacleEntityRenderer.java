package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.TentacleEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TentacleEntityRenderer extends EntityRenderer<TentacleEntity> {
    private final TentacleModel<TentacleEntity> model;
    private static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/tentacles.png");

    public TentacleEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new TentacleModel<>(context.getPart(ModModelLayers.TENTACLE));
    }

    @Override
    public void render(TentacleEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        int index = entity.getSegmentIndex();

        // 1. 计算缩放曲线 (基础粗细)
        float curve = 0.22f + 0.78f / (1.0f + 0.3f * index);

        // 2. 插值平滑旋转
        float lerpYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        float lerpPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());

        // 🎯 核心修复：旋转必须在缩放“之前”（代码顺序在缩放代码的上方）
        // 这样 matrices.scale 才会作用于旋转后的模型本地坐标轴

        // A. 处理水平偏航 (Yaw)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-lerpYaw));
        // B. 处理俯仰 (Pitch) 并让模型倒下指向前方
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lerpPitch + 90f));

        // 🎯 增加：随机水平扭转 (Twist)
        // 使用实体 ID 产生固定随机角度，打破每一节贴图都一模一样的机械感
        float randomTwist = (entity.getId() * 123.45f) % 360.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(randomTwist));

        // 🎯 3. 应用缩放 (此时 Y 轴已经是触手延伸的方向)
        float baseWidth = 0.75f * curve;
        // X, Z 轴是触手的粗细
        // Y 轴是触手的长度，额外拉长 1.4 倍
        matrices.scale(baseWidth, baseWidth, baseWidth);

        // 4. 深度修正
        if (index == 0) {
            // 在旋转后进行平移，会沿着触手的轴向后退，从而深埋进方块中心
            matrices.translate(0, -0.3, 0);
        }

        var vertices = vertexConsumers.getBuffer(this.model.getLayer(TEXTURE));

        // ⚠️ 注意：如果你在 TentacleModel 类的 render 方法里已经写了 scale(1, 1.4, 1)
        // 请把上面的 matrices.scale(baseWidth, baseWidth * 1.4f, baseWidth)
        // 改为 matrices.scale(baseWidth, baseWidth, baseWidth) 避免重复拉长。
        this.model.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(TentacleEntity entity) {
        return TEXTURE;
    }
}