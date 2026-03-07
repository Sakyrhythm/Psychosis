package com.sakyrhythm.psychosis;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.client.*;
import com.sakyrhythm.psychosis.entity.client.feature.CorrosionFeatureRenderer;
import com.sakyrhythm.psychosis.networking.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class PsychosisModClient implements ClientModInitializer {

    private static void spawnBeamParticles(World world, double startX, double startY, double startZ, double endX, double endY, double endZ) {
        // 调试语句 (如果需要)
        // System.out.println("DEBUG (Client): Starting high-density particle beam.");

        Vec3d start = new Vec3d(startX, startY, startZ);
        Vec3d end = new Vec3d(endX, endY, endZ);
        Vec3d delta = end.subtract(start);
        double distance = delta.length();

        // 💥 关键修改：极高密度，创建实心感
        double particlesPerBlock = 30.0;
        int numParticles = (int)(distance * particlesPerBlock);

        for (int i = 0; i <= numParticles; i++) {
            double t = (double)i / numParticles;

            // 计算当前粒子位置 (插值)
            double currentX = startX + delta.x * t;
            double currentY = startY + delta.y * t;
            double currentZ = startZ + delta.z * t;

            // 💥 发射粒子
            world.addParticle(
                    ParticleTypes.ELECTRIC_SPARK, // 选择一种细小且发光的粒子
                    currentX,
                    currentY,
                    currentZ,
                    0.0, 0.0, 0.0 // 速度必须为零，确保粒子停留在轨迹线上
            );
        }
    }
    @Override
    public void onInitializeClient() {
        ModNetworking.registerClientReceiver(payload -> { // 修正：回调只接收 payload 对象

            // 在主线程执行粒子生成
            // 使用 MinecraftClient.getInstance() 获取客户端实例
                 MinecraftClient client = MinecraftClient.getInstance();

            client.execute(() -> {
                // 修正：直接从 payload 对象中获取数据 (使用 record 的 getter 方法)
                double startX = payload.startX();
                double startY = payload.startY();
                double startZ = payload.startZ();
                double endX = payload.endX();
                double endY = payload.endY();
                double endZ = payload.endZ();

                // 修正：使用 client.world 获取当前客户端世界实例
                // 确保 client.world 不为空 (玩家已进入世界)
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
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.MODEL_WHIRLWIND_SLASH, // <-- 使用新的/正确的 ID
                WhirlwindSlashModel::getTexturedModelData // <-- 绑定刀光自己的模型数据
        );
        // 3. 关键一步：将你的单头模型数据注册到自定义的模型层上
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.MODEL_DEGENERATE_WITHER_LAYER,
                DegenerateWitherModel::getTexturedModelData
        );
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.DARK_GOD_MODEL_LAYER,
                DarkGodModel::getTexturedModelData
        );
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.NAIL,
                NailModel::getTexturedModelData
        );
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.SCYTHE,
                ScytheModel::getTexturedModelData
        );

        // 2. 注册渲染器：将实体类型和粒子渲染逻辑关联起来
        EntityRendererRegistry.register(
                ModEntities.DARK_GOD,
                DarkGodRenderer::new
        );
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.GODDESS,GoddessModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.GODDESS, GoddessRenderer::new);

    }
}