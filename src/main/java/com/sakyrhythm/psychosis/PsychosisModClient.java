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
        EntityRendererRegistry.register(ModEntities.EYE_OF_DARK, EyeOfDarkRenderer::new);
        EntityRendererRegistry.register(ModEntities.DEGENERATEWITHER, DWitherEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DARK_DART,DarkDartModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.DARK_DART_PROJECTILE,DarkDartRenderer::new);
        EntityRendererRegistry.register(ModEntities.FLAT_DART, FlatDartRenderer::new);
        EntityRendererRegistry.register(ModEntities.WHIRLWIND_SLASH_ENTITY_TYPE, WhirlwindSlashRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.MODEL_WHIRLWIND_SLASH, // <-- 使用新的/正确的 ID
                WhirlwindSlashModel::getTexturedModelData // <-- 绑定刀光自己的模型数据
        );
        // 3. 关键一步：将你的单头模型数据注册到自定义的模型层上
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.MODEL_DEGENERATE_WITHER_LAYER,
                DegenerateWitherModel::getTexturedModelData
        );
    }
}