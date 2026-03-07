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
import net.minecraft.util.math.Vec3d;

public class TentacleEntityRenderer extends EntityRenderer<TentacleEntity> {
    private final TentacleModel<TentacleEntity> model;
    private static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/tentacles.png");

    public TentacleEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new TentacleModel<>(context.getPart(ModModelLayers.TENTACLE));
    }

    @Override
    public void render(TentacleEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        int count = entity.getSegCount();
        if (count <= 0) return;

        for (int i = 0; i < count; i++) {
            matrices.push();

            Vec3d sPos = entity.getSegPos(i);
            // 🎯 计算相对于根实体的偏移
            matrices.translate(sPos.x - entity.getX(), sPos.y - entity.getY(), sPos.z - entity.getZ());

            float curve = 0.22f + 0.78f / (1.0f + 0.3f * i);

            // 使用存储的角度进行平滑（简单起见直接用current，如需极致平滑可记录prev数组）
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getSegYaw(i)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getSegPitch(i) + 90f));

            // 随机扭转
            float twist = (entity.getId() * 123.45f + i * 45.67f) % 360.0f;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(twist));

            float baseWidth = 0.75f * curve;
            matrices.scale(baseWidth, baseWidth, baseWidth);

            if (i == 0) matrices.translate(0, -0.3, 0);

            var vertices = vertexConsumers.getBuffer(this.model.getLayer(TEXTURE));
            this.model.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);

            matrices.pop();
        }
    }

    @Override
    public Identifier getTexture(TentacleEntity entity) {
        return TEXTURE;
    }
}