package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.TentacleEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class TentacleModel<T extends TentacleEntity> extends SinglePartEntityModel<T> {
    private final ModelPart root;

    public TentacleModel(ModelPart root) {
        // 这里会去查找 getTexturedModelData 里的那个 "root" 节点
        this.root = root.getChild("root");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // 🎯 修复点：创建一个名为 "root" 的主节点，作为所有骨骼的父容器
        // 我们将它放在原点 (0,0,0)
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.NONE);

        // 🎯 修复点：将所有原本 addChild 到 modelPartData 的代码，全部改为 addChild 到 root 节点下
        root.addChild("bone", ModelPartBuilder.create().uv(24, 33).cuboid(-1.55F, -2.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(12, 43).cuboid(-1.55F, -2.5F, 1.5F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(14, 33).cuboid(-0.8F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F))
                .uv(36, 33).cuboid(-1.55F, 1.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(34, 43).cuboid(-1.55F, -2.5F, -2.5F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(7.3F, 18.5F, -2.5F));

        root.addChild("bone2", ModelPartBuilder.create().uv(14, 28).cuboid(-1.375F, -2.9063F, -2.0F, 3.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(48, 34).cuboid(-1.625F, -1.9063F, -1.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(42, 43).cuboid(-1.375F, -2.4063F, 2.0F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(44, 49).cuboid(-1.625F, -1.9063F, 1.0F, 3.0F, 4.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 28).cuboid(-1.375F, 1.8438F, -2.0F, 3.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(52, 48).cuboid(-1.625F, 0.8438F, -1.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(20, 47).cuboid(-1.375F, -2.6563F, -3.0F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(50, 43).cuboid(-1.625F, -1.9063F, -2.0F, 3.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(3.75F, 17.9063F, 6.875F, 0.0F, -1.5708F, 0.0F));

        root.addChild("bone3", ModelPartBuilder.create().uv(24, 37).cuboid(-1.5F, -2.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(0, 46).cuboid(-1.5F, -2.5F, 1.5F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(36, 37).cuboid(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F))
                .uv(0, 38).cuboid(-1.5F, 1.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(46, 37).cuboid(-1.5F, -2.5F, -2.5F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-4.1667F, 15.75F, 7.25F, 0.0F, -1.5708F, 0.0F));

        root.addChild("bone5", ModelPartBuilder.create().uv(8, 49).cuboid(-1.5F, -2.5F, -2.5F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(12, 39).cuboid(-1.5F, -2.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(48, 28).cuboid(-1.5F, -2.5F, 1.5F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(24, 41).cuboid(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F))
                .uv(0, 42).cuboid(-1.5F, 1.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-7.25F, 18.75F, -2.3333F));

        root.addChild("bone4", ModelPartBuilder.create().uv(28, 28).cuboid(-1.375F, -2.9063F, -2.0F, 3.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(52, 51).cuboid(-1.625F, -1.9063F, -1.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(28, 49).cuboid(-1.375F, -2.4063F, 2.0F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 52).cuboid(-1.625F, -1.9063F, 1.0F, 3.0F, 4.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 33).cuboid(-1.375F, 1.8438F, -2.0F, 3.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(16, 53).cuboid(-1.625F, 0.8438F, -1.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(36, 49).cuboid(-1.375F, -2.6563F, -3.0F, 3.0F, 5.0F, 1.0F, new Dilation(0.0F))
                .uv(54, 37).cuboid(-1.625F, -1.9063F, -2.0F, 3.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-2.25F, 17.9063F, -7.125F, 0.0F, -1.5708F, 0.0F));

        root.addChild("bb_main", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -12.0F, -8.0F, 16.0F, 12.0F, 16.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
    }
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        matrices.push();

        // 🎯 核心点：在此处应用 Y 轴 1.4 倍缩放
        // 这会影响 root 节点下的所有内容（bb_main 和所有 bone）
        matrices.scale(1.0F, 1.4F, 1.0F);

        // 调用 root 进行实际渲染
        this.root.render(matrices, vertices, light, overlay, color);

        matrices.pop();
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}