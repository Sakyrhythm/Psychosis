package com.sakyrhythm.psychosis.event;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.TentacleEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PlayerHeightHandler {

    private static final int CHECK_INTERVAL = 10;
    private static final int MAX_Y = 30;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                if (!Psychosis.isTheOcean(world)) continue;

                long time = world.getTime();
                if (time % CHECK_INTERVAL != 0) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    checkAndSpawnTentacle(world, player);
                }
            }
        });
    }

    private static void checkAndSpawnTentacle(ServerWorld world, ServerPlayerEntity player) {
        // 最简单的检查
        if (player.getY() <= MAX_Y) return;
        if (player.getCommandTags().contains("has_tentacle")) return;

        // 生成触手
        spawnGiantTentacleFromBelow(world, player);
    }

    private static void spawnGiantTentacleFromBelow(ServerWorld world, ServerPlayerEntity player) {
        double currentY = player.getY();

        // 计算触手生成位置
        double spawnY = currentY - 40;
        if (spawnY < world.getBottomY() + 10) {
            spawnY = world.getBottomY() + 10;
        }

        BlockPos spawnPos = new BlockPos((int)player.getX(), (int)spawnY, (int)player.getZ());

        // 生成触手
        TentacleEntity tentacle = new TentacleEntity(ModEntities.TENTACLE, world);
        tentacle.setPosition(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);

        // 计算触手长度
        double distance = Math.abs(currentY - spawnY) + 20;
        int segmentCount = (int) Math.ceil(distance / 0.25) + 60;
        segmentCount = MathHelper.clamp(segmentCount, 150, 250);

        tentacle.setInitData(spawnPos, player, segmentCount);
        tentacle.addCommandTag("giant_tentacle");

        world.spawnEntity(tentacle);
        player.addCommandTag("has_tentacle");

        // 简单音效
        world.playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 3.0f, 0.3f);
    }
}