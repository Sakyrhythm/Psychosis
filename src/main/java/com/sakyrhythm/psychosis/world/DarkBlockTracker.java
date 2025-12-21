package com.sakyrhythm.psychosis.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DarkBlockTracker extends PersistentState {

    private static final String ID = "dark_block_tracker";
    // LOCATE_BLOCK_RADIUS 现在仅用于 DarkEyeItem 中的错误消息显示
    private static final int LOCATE_BLOCK_RADIUS = 2000;
    private Set<BlockPos> darkBlockPositions = new HashSet<>();

    public DarkBlockTracker() {
        super();
    }

    // --- 序列化 (写入 NBT) ---
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        NbtList posList = new NbtList();
        for (BlockPos pos : this.darkBlockPositions) {
            NbtCompound posNbt = new NbtCompound();
            posNbt.putLong("Pos", pos.asLong());
            posList.add(posNbt);
        }
        nbt.put("Positions", posList);
        return nbt;
    }

    // --- 反序列化 (从 NBT 读取) ---
    public static DarkBlockTracker createFromNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        DarkBlockTracker tracker = new DarkBlockTracker();
        NbtList posList = nbt.getList("Positions", NbtList.COMPOUND_TYPE);
        for (int i = 0; i < posList.size(); i++) {
            NbtCompound posNbt = posList.getCompound(i);
            tracker.darkBlockPositions.add(BlockPos.fromLong(posNbt.getLong("Pos")));
        }
        return tracker;
    }

    // --- API 方法 ---
    public Set<BlockPos> getAllPositions() {
        return this.darkBlockPositions;
    }

    public void addPosition(BlockPos pos) {
        this.darkBlockPositions.add(pos.toImmutable());
        this.markDirty();
    }

    public void removePosition(BlockPos pos) {
        this.darkBlockPositions.remove(pos);
        this.markDirty();
    }

    // --- 获取实例的方法 ---
    public static DarkBlockTracker get(ServerWorld world) {
        // 创建 Type 对象，使用兼容的 PersistentState.Type 签名
        PersistentState.Type<DarkBlockTracker> type = new PersistentState.Type<>(
                // 1. Supplier<T> (默认构造函数)
                DarkBlockTracker::new,
                // 2. BiFunction<NbtCompound, WrapperLookup, T> (读取工厂)
                DarkBlockTracker::createFromNbt,
                // 3. DataFixTypes (数据修复类型): 使用 null 或 DataFixTypes.CHUNK
                null
        );

        // 调用 getOrCreate(Type, ID) 签名
        return world.getPersistentStateManager().getOrCreate(type, ID);
    }

    // --- 定位最近方块（定位物品使用）---
    public Optional<BlockPos> findClosest(BlockPos playerPos) {
        if (this.darkBlockPositions.isEmpty()) {
            return Optional.empty();
        }

        BlockPos closestPos = null;
        double minDistanceSq = Double.MAX_VALUE;

        // long maxDistanceSq = (long)LOCATE_BLOCK_RADIUS * LOCATE_BLOCK_RADIUS; // <-- 已注释/移除距离限制

        for (BlockPos targetPos : this.darkBlockPositions) {
            double distanceSq = targetPos.getSquaredDistance(playerPos);
            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                closestPos = targetPos;
            }
        }

        // 移除距离检查，实现无限距离定位
        /*
        if (minDistanceSq > maxDistanceSq) {
            return Optional.empty();
        }
        */

        return Optional.ofNullable(closestPos);
    }
}