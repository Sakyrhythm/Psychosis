package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.DarkDartProjectile; // 替换为您的飞镖实体类
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

// 替换 BulletRenderer -> DarkDartRenderer, BulletEntity -> DarkDartProjectile
public class DarkDartRenderer extends ProjectileEntityRenderer<DarkDartProjectile> {
    // <<< 新增：飞镖纹理路径 >>>
    public static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID,"textures/entity/dark_dart.png");

    private final EntityModel<DarkDartProjectile> model;

    public DarkDartRenderer(EntityRendererFactory.Context context) {
        super(context);
        // 使用您的模型层和模型类
        this.model = new DarkDartModel<>(context.getPart(ModModelLayers.DARK_DART));
        this.shadowRadius = 0f;
    }

    @Override
    public Identifier getTexture(DarkDartProjectile entity) {
        return TEXTURE;
    }

    @Override
    public void render(DarkDartProjectile persistentProjectileEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        // 在这里，您可以添加额外的旋转或缩放，但保持 ProjectileEntityRenderer 默认行为通常足够
        super.render(persistentProjectileEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}