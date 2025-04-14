package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PlayerRenderer extends MobEntityRenderer<PlayerEntity, FakePlayerModel<PlayerEntity>>{
    public static final Identifier TEXTURE = Identifier.of("psychosis", "textures/entity/player.png");
    public PlayerRenderer(EntityRendererFactory.Context context) {
        super(context, new FakePlayerModel<>(context.getPart(ModModelLayers.PLAYER_STEVE),context.getPart(ModModelLayers.PLAYER_SLIM)), 0.5f);
    }

    @Override
    public Identifier getTexture(PlayerEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PlayerEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
