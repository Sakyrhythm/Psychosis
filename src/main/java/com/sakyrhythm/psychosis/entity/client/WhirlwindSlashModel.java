package com.sakyrhythm.psychosis.entity.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

// T 泛型是您的实体类型，即 WhirlwindSlashEntity
public class WhirlwindSlashModel<T extends Entity> extends EntityModel<T> {

    // 模型根部件
    private final ModelPart root;
    // 承载贴图的平面部件
    private final ModelPart slashPlane;

    public WhirlwindSlashModel(ModelPart root) {
        this.root = root;
        // 【关键】：确保这里引用的名称与 getTexturedModelData() 中的一致
        this.slashPlane = root.getChild("slash_plane");
    }

    // 静态方法，用于在客户端注册模型结构
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // 水平半径 4 个方块，即 32F
        final float RADIUS_F = 32.0F;
        final float THICKNESS_F = 0.1F; // 保持扁平

        modelPartData.addChild("slash_plane",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        // 定义一个水平的、扁平的 cuboid：厚度在 Y 轴，宽度和深度在 X/Z 轴
                        .cuboid(-RADIUS_F, -THICKNESS_F / 2.0F, -RADIUS_F,
                                RADIUS_F * 2.0F, THICKNESS_F, RADIUS_F * 2.0F,
                                new Dilation(0.0F)),
                // 模型中心位于 (0, 0, 0)
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // 贴图尺寸设置为 64x64，以匹配 64x64 的 cuboid 尺寸
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // 特效实体不需要默认的身体/头部运动
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        // 渲染根部件
        root.render(matrices, vertices, light, overlay, color);
    }
}