// DWitherEntityRenderer.java
package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.DWitherEntity; // 导入你的自定义实体类
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class DWitherEntityRenderer extends MobEntityRenderer<DWitherEntity, DegenerateWitherModel<DWitherEntity>> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/wither/wither.png");

    public DWitherEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DegenerateWitherModel<>(context.getPart(ModModelLayers.MODEL_DEGENERATE_WITHER_LAYER)), 1.0F);
    }

    protected int getBlockLight(DWitherEntity entity, BlockPos blockPos) {
        return 15;
    }

    public Identifier getTexture(DWitherEntity entity) {
        return TEXTURE;
    }

    protected void scale(DWitherEntity entity, MatrixStack matrixStack, float f) {
        float g = 2.0F;
        int i = entity.getInvulnerableTimer();
        if (i > 0) {
            g -= ((float)i - f) / 220.0F * 0.5F;
        }
        matrixStack.scale(g, g, g);
    }
}