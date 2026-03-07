package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.NailEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class NailRenderer extends EntityRenderer<NailEntity> {
    public static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/nail.png");

    // 1. 持有一个模型的引用
    protected final NailModel model;

    public NailRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        // 2. 初始化模型（确保你在 ModModelLayers 里定义了 NAIL 层）
        this.model = new NailModel(ctx.getPart(ModModelLayers.NAIL));
    }

    @Override
    public Identifier getTexture(NailEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(NailEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // ⭐ 核心修改：如果还在 1.75 秒的预警期内，直接跳过渲染
        if (entity.getSpawnDelay() > 0) {
            return;
        }

        matrices.push();

        // 这里的转向建议使用 MathHelper.lerp 来保证平滑感
        float fYaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
        float fPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-fPitch));

        // 你的模型姿态修正 (尖头朝上转为朝前)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(this.getTexture(entity)));
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}