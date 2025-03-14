package com.sakyrhythm.psychosis;

import com.sakyrhythm.psychosis.entity.client.EntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PsychosisModClient implements ClientModInitializer {
    // 定义模型层（使用新版Dilation参数）
    public static final EntityModelLayer HUMANOID_LAYER =
            new EntityModelLayer(Identifier.of("psychosis", "humanoid"), "main");
    public static final EntityModelLayer HUMANOID_SLIM_LAYER =
            new EntityModelLayer(Identifier.of("psychosis", "humanoid_slim"), "main");

    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        EntityRendererRegistry.register(
                ModEntities.HUMANOID,
                context -> new EntityRenderer(context)
        );

        // 注册标准模型层
        EntityModelLayerRegistry.registerModelLayer(
                HUMANOID_LAYER,
                () -> PlayerEntityModel.getTexturedModelData(
                        new Dilation(0.0F),  // 使用Dilation包装浮点数
                        false                // 非Slim模型
                )
        );

        // 注册Slim模型层
        EntityModelLayerRegistry.registerModelLayer(
                HUMANOID_SLIM_LAYER,
                () -> PlayerEntityModel.getTexturedModelData(
                        new Dilation(0.0F),  // 使用Dilation包装浮点数
                        true                 // Slim模型
                )
        );
    }
}