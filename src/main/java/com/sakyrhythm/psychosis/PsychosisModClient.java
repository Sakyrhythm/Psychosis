package com.sakyrhythm.psychosis;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.client.*;
import com.sakyrhythm.psychosis.entity.client.feature.CorrosionFeatureRenderer;
import com.sakyrhythm.psychosis.item.DarkSwordItem;
import com.sakyrhythm.psychosis.item.UmbrellaItem;
import com.sakyrhythm.psychosis.networking.LeftClickC2SPayload;
import com.sakyrhythm.psychosis.networking.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class PsychosisModClient implements ClientModInitializer {

    // 用于检测按键状态
    private boolean wasAttackKeyPressed = false;
    private long lastClickTime = 0;

    private static void spawnBeamParticles(World world, double startX, double startY, double startZ, double endX, double endY, double endZ) {
        Vec3d start = new Vec3d(startX, startY, startZ);
        Vec3d end = new Vec3d(endX, endY, endZ);
        Vec3d delta = end.subtract(start);
        double distance = delta.length();

        double particlesPerBlock = 30.0;
        int numParticles = (int)(distance * particlesPerBlock);

        for (int i = 0; i <= numParticles; i++) {
            double t = (double)i / numParticles;

            double currentX = startX + delta.x * t;
            double currentY = startY + delta.y * t;
            double currentZ = startZ + delta.z * t;

            world.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    currentX,
                    currentY,
                    currentZ,
                    0.0, 0.0, 0.0
            );
        }
    }

    @Override
    public void onInitializeClient() {
        System.out.println("========== PsychosisModClient INITIALIZED ==========");

        // 注册网络接收器
        ModNetworking.registerClientReceiver(payload -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                double startX = payload.startX();
                double startY = payload.startY();
                double startZ = payload.startZ();
                double endX = payload.endX();
                double endY = payload.endY();
                double endZ = payload.endZ();

                if (client.world != null) {
                    spawnBeamParticles(client.world, startX, startY, startZ, endX, endY, endZ);
                }
            });
        });

        // 注册实体渲染器
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer) {
                registrationHelper.register(new CorrosionFeatureRenderer((PlayerEntityRenderer) entityRenderer));
            }
        });

        // ========== 关键修复：添加 GUI 状态检查 ==========
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 检查是否在 GUI 中
            boolean isInGui = client.currentScreen != null;

            if (!isInGui) {
                boolean isAttackPressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
                if (isAttackPressed && !wasAttackKeyPressed) {
                    handleInGameAttack(client);
                }
                wasAttackKeyPressed = isAttackPressed;
            } else {
                // 在 GUI 中时重置状态
                wasAttackKeyPressed = false;
            }
        });

        // 注册所有模型和渲染器
        registerModelsAndRenderers();
    }

    private void handleInGameAttack(MinecraftClient client) {
        if (client.player == null) return;

        // 防抖：防止同一帧多次触发
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 50) return;
        lastClickTime = currentTime;

        ItemStack mainHand = client.player.getMainHandStack();

        // 只有手持黑暗剑时才处理
        if (mainHand.getItem() instanceof DarkSwordItem) {
            // 发送数据包到服务端
            sendLeftClickPacket();

            // 客户端特效（可选）
            client.particleManager.addEmitter(
                    client.player,
                    ParticleTypes.SWEEP_ATTACK,
                    3
            );

            System.out.println("In-game attack detected with Dark Sword!");
        }
    }

    private void sendLeftClickPacket() {
        ClientPlayNetworking.send(new LeftClickC2SPayload("attack_key"));
        Psychosis.LOGGER.debug("Sent left click packet from client");
    }

    private void registerModelsAndRenderers() {
        UmbrellaItem.registerModelPredicate();
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLAYER_STEVE, ()->TexturedModelData.of(PlayerModelCopy.getTexturedModelData(Dilation.NONE,false),64,64));
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLAYER_SLIM, ()->TexturedModelData.of(PlayerModelCopy.getTexturedModelData(Dilation.NONE,true),64,64));
        EntityRendererRegistry.register(ModEntities.PLAYER, PlayerRenderer::new);
        EntityRendererRegistry.register(ModEntities.EYE_OF_DARK, EyeOfDarkRenderer::new);
        EntityRendererRegistry.register(ModEntities.DEGENERATEWITHER, DWitherEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DARK_DART,DarkDartModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.DARK_DART_PROJECTILE,DarkDartRenderer::new);
        EntityRendererRegistry.register(ModEntities.FLAT_DART, FlatDartRenderer::new);
        EntityRendererRegistry.register(ModEntities.WHIRLWIND_SLASH_ENTITY_TYPE, WhirlwindSlashRenderer::new);
        EntityRendererRegistry.register(ModEntities.NAIL, NailRenderer::new);
        EntityRendererRegistry.register(ModEntities.SCYTHE, ScytheRenderer::new);
        EntityRendererRegistry.register(ModEntities.TENTACLE, TentacleEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.TENTACLE, TentacleModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MODEL_WHIRLWIND_SLASH, WhirlwindSlashModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MODEL_DEGENERATE_WITHER_LAYER, DegenerateWitherModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DARK_GOD_MODEL_LAYER, DarkGodModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.NAIL, NailModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SCYTHE, ScytheModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.DARK_GOD, DarkGodRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.GODDESS,GoddessModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.GODDESS, GoddessRenderer::new);
    }
}