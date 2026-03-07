package com.sakyrhythm.psychosis.block.entity;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.TentacleEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.List;

public class WhisperingShellBlockEntity extends BlockEntity {
    private int cooldown = 0;
    private int cleanupTimer = 0;

    public WhisperingShellBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.WHISPERING_SHELL_BE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, WhisperingShellBlockEntity be) {
        if (world.isClient) return;

        // 🎯 1. 定期清理无法追踪的标签 (每秒一次)
        if (++be.cleanupTimer >= 10) {
            be.cleanupTimer = 0;
            cleanupOrphanedTags(world, pos);
        }

        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }

        // 🎯 2. 扫描逻辑：修复玩家锁定
        Box scanBox = new Box(pos).expand(6.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, scanBox,
                e -> {
                    if (!e.isAlive() || e.isSpectator()) return false;
                    // 如果是玩家，排除创造模式
                    if (e instanceof PlayerEntity player && player.isCreative()) return false;
                    // 检查标签
                    return !e.getCommandTags().contains("has_tentacle");
                });

        if (!targets.isEmpty()) {
            be.cooldown = 0;
            for (LivingEntity target : targets) {
                target.addCommandTag("has_tentacle");
                spawnChain(world, pos, target);
            }
        }
    }

    /**
     * 🎯 安全机制：清理丢失触手的生物标签
     */
    private static void cleanupOrphanedTags(World world, BlockPos pos) {
        Box safetyBox = new Box(pos).expand(15.0);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, safetyBox,
                e -> e.getCommandTags().contains("has_tentacle"));

        for (LivingEntity entity : entities) {
            // 在附近寻找属于该实体的触手
            List<TentacleEntity> tentacles = world.getEntitiesByClass(TentacleEntity.class, safetyBox,
                    t -> entity.getUuid().equals(t.getTargetUuid()));

            // 如果附近没有触手在抓它，强制清除标签
            if (tentacles.isEmpty()) {
                entity.removeCommandTag("has_tentacle");
            }
        }
    }

    private static void spawnChain(World world, BlockPos pos, LivingEntity target) {
        Vec3d start = Vec3d.ofCenter(pos);
        double dist = start.distanceTo(target.getPos());

        int count = (int) Math.ceil(dist / 0.15) + 25;
        count = MathHelper.clamp(count, 45, 60);

        Entity last = null;
        for (int i = 0; i < count; i++) {
            TentacleEntity segment = new TentacleEntity(ModEntities.TENTACLE, world);
            segment.refreshPositionAndAngles(start.x, start.y, start.z, 0, 0);
            segment.setSegmentData(pos, target, i, count, last);
            world.spawnEntity(segment);
            last = segment;
        }
    }
}