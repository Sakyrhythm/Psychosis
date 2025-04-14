package com.sakyrhythm.psychosis;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.client.ModModelLayers;
import com.sakyrhythm.psychosis.entity.client.PlayerModel;
import com.sakyrhythm.psychosis.entity.client.PlayerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class PsychosisModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLAYER_STEVE, PlayerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLAYER_SLIM, PlayerModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.PLAYER, PlayerRenderer::new);
    }
}