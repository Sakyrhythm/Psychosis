package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.ScytheBossEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ScytheRenderer extends MobEntityRenderer<ScytheBossEntity, ScytheModel<ScytheBossEntity>> {
    public static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/scythe.png");

    public ScytheRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new ScytheModel<>(ctx.getPart(ModModelLayers.SCYTHE)), 0.0F);
    }

    @Override
    public Identifier getTexture(ScytheBossEntity entity) {
        return TEXTURE;
    }
}