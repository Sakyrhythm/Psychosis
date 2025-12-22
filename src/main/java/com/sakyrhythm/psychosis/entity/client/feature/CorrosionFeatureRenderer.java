package com.sakyrhythm.psychosis.entity.client.feature;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class CorrosionFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier CORROSION_TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/player/dark2.png");
    private static final Identifier DEEPCORROSION_TEXTURE = Identifier.of(Psychosis.MOD_ID, "textures/entity/player/dark3.png");

    // 缓存 RegistryEntry
    @Nullable
    private static RegistryEntry<StatusEffect> darkEffectEntryCache = null;
    @Nullable
    private static RegistryEntry<StatusEffect> vulnerableEffectEntryCache = null;
    @Nullable
    private static RegistryEntry<StatusEffect> frenzyEffectEntryCache = null;

    public CorrosionFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    // --- 辅助方法：确保 RegistryEntry 被加载 ---
    // 注意：这个方法会在玩家的世界对象中查找 RegistryManager
    private static void ensureEffectEntriesLoaded(AbstractClientPlayerEntity player) {
        if (darkEffectEntryCache == null) {
            darkEffectEntryCache = player.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "dark")))
                    .orElse(null);
        }
        if (vulnerableEffectEntryCache == null) {
            vulnerableEffectEntryCache = player.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")))
                    .orElse(null);
        }
        if (frenzyEffectEntryCache == null) {
            frenzyEffectEntryCache = player.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "frenzy")))
                    .orElse(null);
        }
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
        // 1. 加载效果注册项
        ensureEffectEntriesLoaded(player);

        Identifier TEXTURE = null;

        // 获取所有相关效果实例
        @Nullable StatusEffectInstance frenzyInstance = frenzyEffectEntryCache != null ? player.getStatusEffect(frenzyEffectEntryCache) : null;
        @Nullable StatusEffectInstance vulnerableInstance = vulnerableEffectEntryCache != null ? player.getStatusEffect(vulnerableEffectEntryCache) : null;
        @Nullable StatusEffectInstance darkInstance = darkEffectEntryCache != null ? player.getStatusEffect(darkEffectEntryCache) : null;


        if (!player.hasStatusEffect(darkEffectEntryCache)) {
            return;
        }
        else if (frenzyInstance != null) {
            TEXTURE = DEEPCORROSION_TEXTURE;
        }
        else if (vulnerableInstance != null) {
            TEXTURE = CORROSION_TEXTURE;
        }
        else if (darkInstance != null) {
            int amplifier = darkInstance.getAmplifier();
            if (amplifier < 29) {
                return;
            }
        }

        if (TEXTURE != null) {
            VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
            this.getContextModel().render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
        }
    }
}