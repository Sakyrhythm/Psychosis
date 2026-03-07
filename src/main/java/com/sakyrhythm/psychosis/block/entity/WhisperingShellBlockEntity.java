package com.sakyrhythm.psychosis.block.entity;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.TentacleEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.*;
// 🎯 修复：导入 Minecraft 的 Random 接口，不要导入 java.util.Random
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class WhisperingShellBlockEntity extends BlockEntity {
    private int cooldown = 0;
    private int cleanupTimer = 0;

    public WhisperingShellBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.WHISPERING_SHELL_BE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, WhisperingShellBlockEntity be) {
        // 客户端生成粒子
        if (world.isClient) {
            spawnParticles(world, pos);
            return;
        }

        // 服务端逻辑
        serverTick(world, pos, state, be);
    }

    private static void spawnParticles(World world, BlockPos pos) {
        // 🎯 修复：直接获取，不进行错误的强转
        Random random = world.getRandom();

        // 找到水面的基准位置
        BlockPos.Mutable surfacePos = pos.mutableCopy();
        while (world.getFluidState(surfacePos.up()).isStill()) {
            surfacePos.move(Direction.UP);
            if (surfacePos.getY() >= world.getTopY() || surfacePos.getY() - pos.getY() > 24) break;
        }

        if (!world.getBlockState(surfacePos.up()).isAir()) return;

        // ========== 密集气泡逻辑 ==========
        // 增加粒子数量以实现“特别密集”
        int baseCount = 25 + random.nextInt(15);
        double range = 4.5; // 大范围

        for (int i = 0; i < baseCount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * range * 2;
            double offsetZ = (random.nextDouble() - 0.5) * range * 2;

            BlockPos checkPos = BlockPos.ofFloored(
                    surfacePos.getX() + offsetX,
                    surfacePos.getY(),
                    surfacePos.getZ() + offsetZ
            );

            if (world.getFluidState(checkPos).isStill() && world.getBlockState(checkPos.up()).isAir()) {

                double px = (double) checkPos.getX() + random.nextDouble();
                double py = (double) checkPos.getY() + 0.95;
                double pz = (double) checkPos.getZ() + random.nextDouble();

                // 随机初速度，模拟剧烈沸腾
                double velY = 0.1 + random.nextDouble() * 0.2;
                world.addParticle(ParticleTypes.BUBBLE, px, py, pz, 0.0, velY, 0.0);

                if (random.nextFloat() < 0.25f) {
                    world.addParticle(ParticleTypes.BUBBLE_POP, px, py + 0.1, pz, 0.0, 0.01, 0.0);
                }

                if (random.nextFloat() < 0.15f) {
                    world.addParticle(ParticleTypes.SPLASH, px, py + 0.1, pz, 0.0, 0.05, 0.0);
                }
            }
        }

        // 中心爆发团逻辑：增加视觉核心的密度
        if (world.getTime() % 3 == 0) {
            for (int j = 0; j < 8; j++) {
                double r = random.nextDouble() * 1.2;
                double ang = random.nextDouble() * Math.PI * 2;
                world.addParticle(ParticleTypes.BUBBLE,
                        surfacePos.getX() + 0.5 + Math.cos(ang) * r,
                        surfacePos.getY() + 0.9,
                        surfacePos.getZ() + 0.5 + Math.sin(ang) * r,
                        0, 0.15, 0);
            }
        }
    }

    private static void serverTick(World world, BlockPos pos, BlockState state, WhisperingShellBlockEntity be) {
        if (world.getTime() % 20 == 0) cleanupOrphanedTags(world, pos);

        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }

        Box scanBox = new Box(pos).expand(6.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, scanBox,
                e -> e.isAlive() && !e.isSpectator()
                        && !(e instanceof PlayerEntity p && p.isCreative())
                        && !e.getCommandTags().contains("has_tentacle"));

        if (!targets.isEmpty()) {
            be.cooldown = 40; // 锁定后2秒冷却
            for (LivingEntity target : targets) {
                target.addCommandTag("has_tentacle");

                TentacleEntity tentacle = new TentacleEntity(ModEntities.TENTACLE, world);
                tentacle.updatePosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                double dist = Vec3d.ofCenter(pos).distanceTo(target.getPos());
                int count = (int) Math.ceil(dist / 0.15) + 30;
                tentacle.setInitData(pos, target, MathHelper.clamp(count, 50, 80));
                world.spawnEntity(tentacle);
            }
        }
    }

    private static void cleanupOrphanedTags(World world, BlockPos pos) {
        Box safetyBox = new Box(pos).expand(15.0);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, safetyBox,
                e -> e.getCommandTags().contains("has_tentacle"));

        for (LivingEntity entity : entities) {
            List<TentacleEntity> tentacles = world.getEntitiesByClass(TentacleEntity.class, safetyBox,
                    t -> entity.getUuid().equals(t.getTargetUuid()));

            if (tentacles.isEmpty()) {
                entity.removeCommandTag("has_tentacle");
            }
        }
    }
}