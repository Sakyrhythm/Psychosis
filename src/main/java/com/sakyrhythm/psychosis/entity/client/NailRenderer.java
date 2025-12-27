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
        matrices.push();

        // 平滑插值角度
        float fYaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
        float fPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());

        // 1. 基础转向 (Y轴) - 将模型转向实体的 Yaw
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fYaw));

        // 2. 俯仰转向 (X轴) - 将模型转向实体的 Pitch
        // 注意：这里的正负号取决于你希望尖头向上还是向下弯。通常是负号。
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-fPitch));

        // 3. 模型姿态校准 (关键！)
        // 因为你的尖头在模型里是朝上的 (+Y)，
        // 我们需要把 +Y 旋转到水平的前方 (对应实体朝向的 Z 轴)。
        // 旋转 -90度 或 90度 绕 X 轴：
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(this.getTexture(entity)));
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}