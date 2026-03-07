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

    public WhisperingShellBlockEntity(BlockPos pos, BlockState state) {
        super(ModEntities.WHISPERING_SHELL_BE, pos, state);
    }

    // 在 WhisperingShellBlockEntity.java 中修改 tick 逻辑
    public static void tick(World world, BlockPos pos, BlockState state, WhisperingShellBlockEntity be) {
        if (world.isClient) return;

        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }

        Box scanBox = new Box(pos).expand(6.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, scanBox,
                e -> e.isAlive()
                        && !e.isSpectator()
                        && !e.getCommandTags().contains("has_tentacle") // 🎯 关键：检查标签
                        && !(e instanceof PlayerEntity && ((PlayerEntity)e).isCreative()));

        if (!targets.isEmpty()) {
            be.cooldown = 200; // 锁定后较长的冷却
            for (LivingEntity target : targets) {
                target.addCommandTag("has_tentacle"); // 🎯 关键：锁定目标
                spawnChain(world, pos, target);
            }
        }
    }

    private static void spawnChain(World world, BlockPos pos, LivingEntity target) {
        Vec3d start = Vec3d.ofCenter(pos);
        double dist = start.distanceTo(target.getPos());

        // 🎯 3倍加密：原本每 0.5 格一节，现在每 0.15 格一节
        // 这样 6 格的长度需要约 40 节，再加上 20 节用于致密缠绕
        int count = (int) Math.ceil(dist / 0.15) + 25;
        count = MathHelper.clamp(count, 45, 60); // 确保总节数在 45 到 60 之间

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