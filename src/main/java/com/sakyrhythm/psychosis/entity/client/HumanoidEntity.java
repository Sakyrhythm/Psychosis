package com.sakyrhythm.psychosis.entity.client;

import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class HumanoidEntity extends PathAwareEntity {
    // 定义跟踪数据
    private static final TrackedData<String> SKIN_OWNER = DataTracker.registerData(
            HumanoidEntity.class,
            TrackedDataHandlerRegistry.STRING
    );

    public HumanoidEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    // 修正后的 initDataTracker (1.21+ 新API)
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SKIN_OWNER, ""); // 初始化默认值为空字符串
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world,
                                 LocalDifficulty difficulty,
                                 SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);

        if (world instanceof ServerWorld serverWorld) {
            List<ServerPlayerEntity> players = serverWorld.getServer().getPlayerManager().getPlayerList();
            if (!players.isEmpty()) {
                int index = serverWorld.getRandom().nextInt(players.size());
                ServerPlayerEntity target = players.get(index);
                this.dataTracker.set(SKIN_OWNER, target.getUuidAsString());
            }
        }
        return data;
    }

    public String getSkinOwner() {
        return this.dataTracker.get(SKIN_OWNER);
    }
}