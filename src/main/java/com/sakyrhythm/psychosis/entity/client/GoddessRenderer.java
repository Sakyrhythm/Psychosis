package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.GoddessEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

// 假设您使用原版 MobEntityRenderer
public class GoddessRenderer extends MobEntityRenderer<GoddessEntity, GoddessModel<GoddessEntity>> {

    public static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID,"textures/entity/goddess.png");

    public GoddessRenderer(EntityRendererFactory.Context context) {
        // 假设 ModModelLayers.GODDESS 是正确的模型层注册
        super(context, new GoddessModel<>(context.getPart(ModModelLayers.GODDESS)), 0.5f);
    }

    @Override
    public Identifier getTexture(GoddessEntity entity) {
        return TEXTURE;
    }

    // 重写 render 方法以实现粒子化/出场不可见
    @Override
    public void render(GoddessEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {

        // --- 【核心逻辑】控制渲染可见性 ---
        if (livingEntity.isParticlized() || livingEntity.isAppearing()) {
            // 如果处于粒子化状态或出场动画中，不进行渲染
            return;
        }

        // 如果不是粒子化状态，则正常渲染模型
        // 可以在这里添加任何缩放/位置调整（例如，如果模型需要向上平移）

        super.render(livingEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}