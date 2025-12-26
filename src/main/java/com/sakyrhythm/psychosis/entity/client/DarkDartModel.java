package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.DarkDartProjectile;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;

// 继承、类名保持不变
public class DarkDartModel<T extends DarkDartProjectile> extends SinglePartEntityModel<T> {

    // ModelPart 根部件
    private final ModelPart bb_main;

    public DarkDartModel(ModelPart root) {
        // 保持不变：从模型层获取名为 "bullet" 的部件
        // 即使模型是空的，这个根部件依然存在
        this.bb_main = root.getChild("bullet");
    }

    // 静态方法：提供【空的】几何形状
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // *** 核心修改：创建一个名为 "bullet" 的根部件，但其下不添加任何 cuboid ***
        // ModelPartBuilder.create() 后面没有任何几何体定义，因此它是空的。
        ModelPartData bulletPart = modelPartData.addChild("bullet",
                ModelPartBuilder.create(), // 不添加任何几何形状
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        // 移除 cube_r1 的添加，因为根部件已是空的，旋转部件也没有意义了。
        // 如果您希望保留 cube_r1 的定义，也可以如下所示保留，但它不会渲染任何东西：
        // ModelPartData cube_r1 = bulletPart.addChild("cube_r1",
        //         ModelPartBuilder.create(),
        //         ModelTransform.of(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

        // 保持纹理尺寸
        return TexturedModelData.of(modelData, 16, 16);
    }

    @Override
    public ModelPart getPart() {
        return this.bb_main;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // 保持不变，不需要设置任何角度，因为它没有形状
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        // 保持不变，bb_main 会尝试渲染，但因为没有几何形状，所以不会绘制任何东西。
        // 如果想更彻底，可以添加 if(bb_main != null) 检查，但通常不需要。
        this.bb_main.render(matrices, vertexConsumer, light, overlay, color);
    }
}