package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.FallenSwordEntity;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class FallenSwordEntityRenderer extends EntityRenderer<FallenSwordEntity> {
    private final ItemRenderer itemRenderer;
    private static final ItemStack SWORD_STACK = new ItemStack(ModItems.DARKSWORD);

    public FallenSwordEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(FallenSwordEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // 1. 稍微往上移一点，确保模型中心和实体对齐
        matrices.translate(0, 1.5, 0);

        // 2. 放大 (让神剑看起来更有张力)
        matrices.scale(2.0f, 2.0f, 2.0f);

        // 3. 核心旋转逻辑：使用新的 rotationDegrees 方法

        // a) 让它竖起来 (绕 X 轴旋转 180 度)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));

        // b) 抵消 handheld 模型默认的 45 度倾斜，使其垂直 (绕 Z 轴旋转 45 度)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F));

        // c) 随时间缓慢旋转（增加神秘感）(绕 Y 轴旋转)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.age * 2.0f));

        // 4. 调用物品渲染器
        this.itemRenderer.renderItem(SWORD_STACK, ModelTransformationMode.FIXED, light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), entity.getId());

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
    @Override
    public Identifier getTexture(FallenSwordEntity entity) {
        return Identifier.of("minecraft", "textures/atlas/blocks.png");
    }
}