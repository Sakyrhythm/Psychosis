package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.WhirlwindSlashEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;

public class WhirlwindSlashRenderer extends EntityRenderer<WhirlwindSlashEntity> {

    // 【重要】替换为您的贴图路径
    private static final Identifier TEXTURE = Identifier.of("psychosis", "textures/entity/whirlwind_slash_texture.png");

    private final WhirlwindSlashModel<WhirlwindSlashEntity> model;

    public WhirlwindSlashRenderer(EntityRendererFactory.Context context) {
        super(context);
        // 【关键】：绑定到正确的模型层 ID
        this.model = new WhirlwindSlashModel<>(context.getPart(ModModelLayers.MODEL_WHIRLWIND_SLASH));
        this.shadowRadius = 0.0F; // 没有影子
    }

    @Override
    public Identifier getTexture(WhirlwindSlashEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(WhirlwindSlashEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        matrices.push();

        // ⭐ 核心修正 1：从实体获取缩放值（假设 WhirlwindSlashEntity 提供了此方法）
        float scale = entity.getScale();

        // ⭐ 核心修正 2：应用动态缩放
        matrices.scale(scale, scale, scale);

        // 1. 获取生命周期进度
        float lifeProgress = (float) entity.age + tickDelta;
        // MAX_LIFE 必须与实体类 (WhirlwindSlashEntity) 中的 MAX_LIFE 字段保持一致
        final float MAX_LIFE = 5.0f;

        // 2. 旋转动画计算
        // 每秒旋转 4 圈 (5 ticks / 20 ticks/sec * 360 * 2)
        float rotationAngle = (lifeProgress / MAX_LIFE) * 360.0F * 2.0F;

        // 3. 矩阵变换：只执行围绕 Y 轴的旋转 (实现水平圆盘效果)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        // 4. 透明度渐变：开始阶段和结束阶段逐渐透明
        float alpha = MathHelper.clamp(lifeProgress / MAX_LIFE * 2.0F, 0.0F, 1.0F); // 快速达到 1.0
        alpha = MathHelper.clamp(alpha * (MAX_LIFE - lifeProgress) * 0.5F, 0.0F, 1.0F); // 结束时淡出

        // 5. 颜色 (白色) 和 Alpha
        int colorInt = ((int)(alpha * 255.0F) << 24) | (0xFFFFFF); // ARGB 格式

        // 6. 渲染模型
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(this.getTexture(entity)));

        // 传入 0 作为 overlay，防止特效实体被击中时闪烁
        this.model.render(matrices, vertexConsumer, light, 0, colorInt);

        matrices.pop();

        // 不调用 super.render，防止默认的实体 yaw 旋转覆盖我们的自定义旋转。
        // super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}