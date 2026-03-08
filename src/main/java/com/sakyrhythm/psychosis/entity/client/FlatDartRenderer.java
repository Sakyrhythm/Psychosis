// 示例：FlatDartRenderer.java (放在 client/render 包下)
package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.FlatDartProjectile;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

public class FlatDartRenderer extends EntityRenderer<FlatDartProjectile> {

    private final ItemStack DART_STACK = new ItemStack(ModItems.FLAT_DART);

    public FlatDartRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(FlatDartProjectile entity) {
        // 这个方法在这里不重要，因为我们渲染的是模型
        return Identifier.of("psychosis", "textures/item/flat_dart.png");
    }

    @Override
    public void render(FlatDartProjectile entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        matrices.push();

        // 缩放和定位
        matrices.scale(2.5F, 1.5F, 1.5F);

        // 旋转模型以使其始终面向玩家 (Billboard效果或跟随投射方向)
        // 关键：使平面始终面向正确方向

        // 1. 获取投射物方向的旋转
        Quaternionf rotation = this.dispatcher.getRotation();
        matrices.multiply(rotation);

        // 2. 额外旋转90度，使贴图平面正确对齐
        matrices.multiply(new Quaternionf().rotationX(90.0F * ((float)Math.PI / 180F)));

        // 使用 ItemRenderer 渲染指定物品的模型
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                DART_STACK,
                ModelTransformationMode.FIXED, // 使用FIXED模式
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                entity.getId()
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}