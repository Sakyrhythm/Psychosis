package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.EyeOfDarkEntity; // 导入你的自定义实体类
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class EyeOfDarkRenderer extends EntityRenderer<EyeOfDarkEntity> {

    private static final Identifier TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/item/demon.png");

    // 原版 EyeOfEnder 渲染器使用了 ItemRenderer 来绘制物品模型
    private final ItemRenderer itemRenderer;

    public EyeOfDarkRenderer(EntityRendererFactory.Context context) {
        // 渲染阴影大小，通常投掷物较小，设为 0.0f
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(EyeOfDarkEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(EyeOfDarkEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
        matrices.translate(-0.25F, 0.0F, 0.0F); // 小幅平移以匹配原版效果


        this.itemRenderer.renderItem(
                entity.getStack(),
                net.minecraft.client.render.model.json.ModelTransformationMode.GROUND,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                entity.getWorld().random.nextInt()
        );

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.scale(0.5F, 0.5F, 0.5F);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}