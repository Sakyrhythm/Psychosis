package com.sakyrhythm.psychosis.entity.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.UUID;

public class TentacleEntity extends Entity {
    private static final TrackedData<Integer> INDEX = DataTracker.registerData(TentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private UUID targetUuid;
    private Entity parent;
    private UUID parentUuid;
    private BlockPos ownerPos = BlockPos.ORIGIN;
    private int maxSegments = 15;

    public TentacleEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(INDEX, 0);
    }

    public void setSegmentData(BlockPos pos, Entity target, int index, int max, Entity parent) {
        this.ownerPos = pos;
        this.targetUuid = target.getUuid();
        this.dataTracker.set(INDEX, index);
        this.maxSegments = max;
        this.parent = parent;
        if (parent != null) this.parentUuid = parent.getUuid();
    }

    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        LivingEntity target = (LivingEntity) ((ServerWorld)getWorld()).getEntity(targetUuid);
        int index = getSegmentIndex();

        if (target == null || !target.isAlive() || this.age > 400) {
            if (target != null) target.removeCommandTag("has_tentacle");
            this.discard();
            return;
        }

        if (index > 0 && parent == null) {
            parent = ((ServerWorld)getWorld()).getEntity(parentUuid);
            if (parent == null) { this.discard(); return; }
        }

        // 🎯 1. 激进的缩放曲线：前几节迅速变细
        // n=0: 1.0 | n=1: 0.77 | n=2: 0.62 | n=5: 0.41 | n=20+: 0.25左右
        float segmentScale = 0.22f + 0.78f / (1.0f + 0.3f * index);

        // 🎯 2. 消除间隔的关键：间距 = 模型高度(0.75) * 缩放
        // 为了让连接处有一点点重叠（显得肉感），乘以 0.95
        float spacing = 0.75f * segmentScale * 0.95f;

        // 🎯 3. 目标点逻辑（3倍加密缠绕）
        Vec3d targetPoint;
        // 假设总共 50 节，最后 30 节都在缠绕
        if (index >= maxSegments - 30) {
            // 螺旋参数：随着 index 增加，角度变化极快，半径缩小
            double angle = index * 0.8; // 缩小步进，让模型紧密挨着
            double radius = 0.35 + (maxSegments - index) * 0.01;
            targetPoint = target.getPos().add(
                    Math.cos(angle) * radius,
                    (index - (maxSegments - 30)) * 0.08 + 0.1, // 每一节只上升 0.08，极度致密
                    Math.sin(angle) * radius
            );

            // 强力控制
            target.setVelocity(0, 0, 0);
            target.velocityModified = true;
        } else {
            // 伸向目标的中间节
            targetPoint = target.getPos().add(0, target.getHeight() * 0.5, 0);
        }

        // 🎯 4. 坐标更新：彻底修复第一二节间隔
        if (index == 0) {
            // 第一节固定在中心，但稍微向目标移动 0.1 格，防止在方块中心显得太死板
            this.updatePosition(ownerPos.getX() + 0.5, ownerPos.getY() + 0.5, ownerPos.getZ() + 0.5);
        } else {
            Vec3d parentPos = parent.getPos();
            Vec3d dir = targetPoint.subtract(parentPos);
            if (dir.lengthSquared() < 0.0001) dir = new Vec3d(0, 1, 0);

            // 强制根据 spacing 摆放位置，不留缝隙
            Vec3d newPos = parentPos.add(dir.normalize().multiply(spacing));
            this.updatePosition(newPos.x, newPos.y, newPos.z);
        }

        // 🎯 5. 角度更新
        Vec3d lookDir = targetPoint.subtract(this.getPos());
        if (lookDir.lengthSquared() > 0.001) {
            this.setYaw((float) Math.toDegrees(MathHelper.atan2(-lookDir.x, lookDir.z)));
            double horiz = MathHelper.sqrt((float) (lookDir.x * lookDir.x + lookDir.z * lookDir.z));
            this.setPitch((float) Math.toDegrees(MathHelper.atan2(-lookDir.y, horiz)));
        }
    }

    public int getSegmentIndex() { return this.dataTracker.get(INDEX); }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ownerPos = BlockPos.fromLong(nbt.getLong("op"));
        this.dataTracker.set(INDEX, nbt.getInt("idx"));
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putLong("op", ownerPos.asLong());
        nbt.putInt("idx", getSegmentIndex());
    }
}