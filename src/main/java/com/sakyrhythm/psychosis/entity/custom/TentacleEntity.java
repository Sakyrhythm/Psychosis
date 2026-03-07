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
        this.noClip = true; // 优化：关闭碰撞计算
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

    // 🎯 必须添加的 Getter 供 BlockEntity 检查使用
    public UUID getTargetUuid() { return this.targetUuid; }
    public int getMaxSegments() { return this.maxSegments; }

    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        // 🎯 修复：如果 targetUuid 为空，尝试从 NBT 恢复后获取
        if (targetUuid == null) return;
        LivingEntity target = (LivingEntity) ((ServerWorld)getWorld()).getEntity(targetUuid);
        int index = getSegmentIndex();

        // 消失判定
        if (target == null || !target.isAlive() || this.age > 400) {
            if (target != null) target.removeCommandTag("has_tentacle");
            this.discard();
            return;
        }

        if (index > 0 && parent == null) {
            parent = ((ServerWorld)getWorld()).getEntity(parentUuid);
            if (parent == null) { this.discard(); return; }
        }

        float segmentScale = 0.22f + 0.78f / (1.0f + 0.3f * index);
        float spacing = 0.75f * segmentScale * 0.95f;

        Vec3d targetPoint;
        if (index >= maxSegments - 30) {
            double angle = index * 0.8;
            double radius = 0.35 + (maxSegments - index) * 0.01;
            targetPoint = target.getPos().add(
                    Math.cos(angle) * radius,
                    (index - (maxSegments - 30)) * 0.08 + 0.1,
                    Math.sin(angle) * radius
            );
            target.setVelocity(0, 0, 0);
            target.velocityModified = true;
        } else {
            targetPoint = target.getPos().add(0, target.getHeight() * 0.5, 0);
        }

        if (index == 0) {
            this.updatePosition(ownerPos.getX() + 0.5, ownerPos.getY() + 0.5, ownerPos.getZ() + 0.5);
        } else {
            Vec3d parentPos = parent.getPos();
            Vec3d dir = targetPoint.subtract(parentPos);
            if (dir.lengthSquared() < 0.0001) dir = new Vec3d(0, 1, 0);
            Vec3d newPos = parentPos.add(dir.normalize().multiply(spacing));
            this.updatePosition(newPos.x, newPos.y, newPos.z);
        }

        Vec3d lookDir = targetPoint.subtract(this.getPos());
        if (lookDir.lengthSquared() > 0.001) {
            this.setYaw((float) Math.toDegrees(MathHelper.atan2(-lookDir.x, lookDir.z)));
            double horiz = MathHelper.sqrt((float) (lookDir.x * lookDir.x + lookDir.z * lookDir.z));
            this.setPitch((float) Math.toDegrees(MathHelper.atan2(-lookDir.y, horiz)));
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ownerPos = BlockPos.fromLong(nbt.getLong("op"));
        this.dataTracker.set(INDEX, nbt.getInt("idx"));
        this.maxSegments = nbt.getInt("max_seg");
        if (nbt.containsUuid("target")) this.targetUuid = nbt.getUuid("target");
    }
    public int getSegmentIndex() { return this.dataTracker.get(INDEX); }
    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putLong("op", ownerPos.asLong());
        nbt.putInt("idx", getSegmentIndex());
        nbt.putInt("max_seg", maxSegments);
        if (targetUuid != null) nbt.putUuid("target", targetUuid);
    }
}