package com.sakyrhythm.psychosis.entity.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class EntityRenderer extends LivingEntityRenderer<HumanoidEntity, PlayerEntityModel<HumanoidEntity>> {

    private final Map<String, Identifier> skinCache = new HashMap<>();
    private final EntityRendererFactory.Context context;

    // 修复构造函数参数
    public EntityRenderer(EntityRendererFactory.Context context) {
        super(context,
                new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), // 使用boolean参数
                0.5F); // 设置适当的阴影半径
        this.context = context;
    }

    @Override
    public Identifier getTexture(HumanoidEntity entity) {
        String uuidString = entity.getSkinOwner();
        if (uuidString == null || uuidString.isEmpty()) return createSafeIdentifier();

        try {
            UUID uuid = UUID.fromString(uuidString);
            return getRealTimeSkin(uuid);
        } catch (IllegalArgumentException e) {
            return createSafeIdentifier();
        }
    }

    private Identifier getRealTimeSkin(UUID uuid) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return createSafeIdentifier();
        }

        if (skinCache.containsKey(uuid.toString())) {
            PlayerListEntry entry = MinecraftClient.getInstance()
                    .getNetworkHandler()
                    .getPlayerListEntry(uuid);

            if (entry == null) {
                skinCache.remove(uuid.toString());
                return createSafeIdentifier();
            }
            return skinCache.get(uuid.toString());
        }

        PlayerListEntry playerEntry = MinecraftClient.getInstance()
                .getNetworkHandler()
                .getPlayerListEntry(uuid);

        if (playerEntry == null) return createSafeIdentifier();

        SkinTextures textures = playerEntry.getSkinTextures();
        updateModelAccordingToSkin(textures.model());

        Identifier skin = textures.texture();
        skinCache.put(uuid.toString(), skin);
        return skin;
    }

    // 使用安全的Identifier构造方式
    private Identifier createSafeIdentifier() {
        return Identifier.of("psychosis:empty");
    }

    private void updateModelAccordingToSkin(SkinTextures.Model modelType) {
        this.model = new PlayerEntityModel<>(
                context.getPart(
                        modelType == SkinTextures.Model.SLIM ?
                                EntityModelLayers.PLAYER_SLIM :
                                EntityModelLayers.PLAYER
                ),
                modelType == SkinTextures.Model.SLIM // 使用boolean参数
        );
    }

    @Override
    public void render(HumanoidEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    protected boolean hasLabel(HumanoidEntity entity) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            skinCache.keySet().removeIf(uuidStr -> {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    return MinecraftClient.getInstance()
                            .getNetworkHandler()
                            .getPlayerListEntry(uuid) == null;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            });
        }
        return super.hasLabel(entity);
    }
}