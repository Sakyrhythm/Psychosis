package com.sakyrhythm.psychosis.entity.client.feature;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CorrosionFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier CORROSION_TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/player/dark2.png");
    private static final Identifier DEEPCORROSION_TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/player/dark3.png");
    public CorrosionFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float customAngle,
            float headYaw,
            float headPitch
    ) {
        IPlayerEntity playerInterface = (IPlayerEntity) player;
        int darkLevel = playerInterface.getDark();
        //if (darkLevel <= 24000 || darkLevel > 48000) return;

        if (darkLevel != 100) {
            if (darkLevel == 50) {
                VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(CORROSION_TEXTURE));

                this.getContextModel().render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
            }
            else return;
        };

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(DEEPCORROSION_TEXTURE));

        this.getContextModel().render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
    }
}