package com.sakyrhythm.psychosis;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.client.*;
import com.sakyrhythm.psychosis.entity.client.feature.CorrosionFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.WitherEntityRenderer;

@Environment(EnvType.CLIENT)
public class PsychosisModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer) {
                registrationHelper.register(new CorrosionFeatureRenderer((PlayerEntityRenderer) entityRenderer));
            }
        });
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLAYER_STEVE, ()->TexturedModelData.of(PlayerModelCopy.getTexturedModelData(Dilation.NONE,false),64,64));
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLAYER_SLIM, ()->TexturedModelData.of(PlayerModelCopy.getTexturedModelData(Dilation.NONE,true),64,64));
        EntityRendererRegistry.register(ModEntities.PLAYER, PlayerRenderer::new);
        EntityRendererRegistry.register(ModEntities.DEGENERATEWITHER, DWitherEntityRenderer::new);

        // 3. 关键一步：将你的单头模型数据注册到自定义的模型层上
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.MODEL_DEGENERATE_WITHER_LAYER,
                DegenerateWitherModel::getTexturedModelData
        );
    }
}