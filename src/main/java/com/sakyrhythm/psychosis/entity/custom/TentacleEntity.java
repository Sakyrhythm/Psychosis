package com.sakyrhythm.psychosis.entity.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.Collections;
import java.util.UUID;

public class TentacleEntity extends Entity {
    private static final TrackedData<Integer> TARGET_ID = DataTracker.registerData(TentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<BlockPos> OWNER_POS = DataTracker.registerData(TentacleEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
    private static final TrackedData<Integer> SEGMENT_COUNT = DataTracker.registerData(TentacleEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private Vec3d[] segmentPositions = new Vec3d[0];
    private float[] segmentYaws = new float[0];
    private float[] segmentPitches = new float[0];
    private UUID cachedTargetUuid;
    private Vec3d capturePos; // 禁锢点

    public TentacleEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.ignoreCameraFrustum = true; // 忽略视锥剔除
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(TARGET_ID, -1);
        builder.add(OWNER_POS, BlockPos.ORIGIN);
        builder.add(SEGMENT_COUNT, 60);
    }

    public void setInitData(BlockPos pos, LivingEntity target, int count) {
        this.dataTracker.set(OWNER_POS, pos);
        this.dataTracker.set(TARGET_ID, target.getId());
        this.dataTracker.set(SEGMENT_COUNT, count);
        this.cachedTargetUuid = target.getUuid();
        this.capturePos = target.getPos();
        initArrays(count);
    }

    private void initArrays(int count) {
        int safeCount = Math.max(1, count);
        if (segmentPositions.length != safeCount) {
            segmentPositions = new Vec3d[safeCount];
            segmentYaws = new float[safeCount];
            segmentPitches = new float[safeCount];
            Vec3d start = Vec3d.ofCenter(this.dataTracker.get(OWNER_POS));
            for (int i = 0; i < safeCount; i++) {
                segmentPositions[i] = start;
                segmentYaws[i] = 0f;
                segmentPitches[i] = 0f;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        int count = this.dataTracker.get(SEGMENT_COUNT);
        if (count <= 0) return;
        initArrays(count);

        Entity targetRaw = getWorld().getEntityById(this.dataTracker.get(TARGET_ID));
        BlockPos ownerPos = this.dataTracker.get(OWNER_POS);

        // 消失判定
        if (!(targetRaw instanceof LivingEntity target) || !target.isAlive() || this.age > 450) {
            if (!getWorld().isClient && targetRaw instanceof LivingEntity t) {
                t.removeCommandTag("has_tentacle");
                t.removeStatusEffect(StatusEffects.SLOWNESS);
                t.removeStatusEffect(StatusEffects.DARKNESS);
            }
            this.discard();
            return;
        }

        this.cachedTargetUuid = target.getUuid();
        if (capturePos == null) capturePos = target.getPos();

        // 🎯 紧缩进度计算：从 60 tick 开始，到 220 tick 处决
        float tightenProgress = MathHelper.clamp((this.age - 60) / 160.0f, 0.0f, 1.0f);

        if (!getWorld().isClient) {
            immobilizeAndShake(target, tightenProgress);
            if (tightenProgress >= 1.0f) executeTarget(target);
        }

        // 链式计算所有身体节
        for (int i = 0; i < count; i++) {
            // 1. 缩放曲线：先快后慢
            float curve = 0.22f + 0.78f / (1.0f + 0.3f * i);
            // 缠绕部二次缩小
            if (i >= count - 40) curve *= (float) Math.pow(0.98, (i - (count - 40)));

            // 2. 间距 (考虑 1.4 倍模型拉伸)
            float spacing = 0.75f * curve * 1.4f * 0.85f;

            // 3. 目标点逻辑
            Vec3d targetPoint;
            if (i >= count - 40) {
                // 随着进度旋转加快
                double angle = i * 0.7 + (this.age * (0.05 + tightenProgress * 0.25));
                // 随着进度半径缩小
                double baseRadius = 0.45 + (count - i) * 0.005;
                double currentRadius = MathHelper.lerp(tightenProgress, baseRadius, 0.03);

                targetPoint = target.getPos().add(
                        Math.cos(angle) * currentRadius,
                        (i - (count - 40)) * (0.06 - tightenProgress * 0.03) + 0.1,
                        Math.sin(angle) * currentRadius
                );
            } else {
                targetPoint = target.getPos().add(0, target.getHeight() * 0.5, 0);
            }

            // 4. 更新位置
            if (i == 0) {
                segmentPositions[i] = Vec3d.ofCenter(ownerPos);
                this.updatePosition(segmentPositions[i].x, segmentPositions[i].y, segmentPositions[i].z);
            } else {
                Vec3d prevPos = segmentPositions[i - 1];
                Vec3d dir = targetPoint.subtract(prevPos);
                if (dir.lengthSquared() < 0.0001) dir = new Vec3d(0, 1, 0);
                segmentPositions[i] = prevPos.add(dir.normalize().multiply(spacing));
            }

            // 5. 更新旋转角度
            Vec3d lookDir = targetPoint.subtract(segmentPositions[i]);
            if (lookDir.lengthSquared() > 0.001) {
                segmentYaws[i] = (float) Math.toDegrees(MathHelper.atan2(-lookDir.x, lookDir.z));
                double horiz = MathHelper.sqrt((float) (lookDir.x * lookDir.x + lookDir.z * lookDir.z));
                segmentPitches[i] = (float) Math.toDegrees(MathHelper.atan2(-lookDir.y, horiz));
            }
        }
    }

    private void immobilizeAndShake(LivingEntity target, float progress) {
        // 🎯 高频小幅度震动强度
        float shakeIntensity = (progress > 0.1f) ? 0.02f + (progress * 0.08f) : 0f;
        double rx = (random.nextDouble() - 0.5) * shakeIntensity;
        double ry = (random.nextDouble() - 0.5) * shakeIntensity;
        double rz = (random.nextDouble() - 0.5) * shakeIntensity;

        if (target instanceof ServerPlayerEntity player) {
            // 🎯 玩家：通过强制传送实现视角高频抖动 + 硬禁锢 (防珍珠/TP)
            if (progress > 0.1f) {
                // 利用缓慢效果强制缩小 FOV
                int slownessLevel = (int) (progress * 15);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, slownessLevel, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 15, 0, false, false, false));
            }
            // 每一帧强制拉回 capturePos 并赋予视角偏移
            player.networkHandler.requestTeleport(capturePos.x + rx, capturePos.y + ry, capturePos.z + rz,
                    player.getYaw() + (float)rx * 90f, player.getPitch() + (float)ry * 90f, Collections.emptySet());
        } else {
            // 🎯 生物：硬禁锢
            target.requestTeleport(capturePos.x + rx, capturePos.y + ry, capturePos.z + rz);
            target.setVelocity(0, 0, 0);
            target.velocityModified = true;
        }
    }

    private void executeTarget(LivingEntity target) {
        if (getWorld() instanceof ServerWorld serverWorld) {
            // 原地特效
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1, target.getZ(), 60, 0.1, 0.4, 0.1, 0.05);
            serverWorld.playSound(null, target.getBlockPos(),
                    SoundEvents.ENTITY_GENERIC_DEATH, SoundCategory.HOSTILE, 1.0f, 0.5f);

            target.removeCommandTag("has_tentacle");

            if (target instanceof ServerPlayerEntity player) {
                // 🎯 处决玩家：传送到末地黑曜石平台
                ServerWorld endWorld = serverWorld.getServer().getWorld(World.END);
                if (endWorld != null) {
                    player.teleport(endWorld, 100.5, 49.0, 0.5, player.getYaw(), player.getPitch());
                    endWorld.playSound(null, new BlockPos(100, 49, 0), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 1.0f, 0.8f);
                    endWorld.playSound(null, new BlockPos(100, 49, 0), SoundEvents.BLOCK_BELL_USE, SoundCategory.HOSTILE, 1.0f, 0.8f);
                }
            } else {
                // 🎯 处决生物：直接移除
                target.discard();
            }
        }
        this.discard();
    }

    public UUID getTargetUuid() { return cachedTargetUuid; }
    public int getSegCount() { return segmentPositions.length; }
    public Vec3d getSegPos(int i) { return i < segmentPositions.length ? segmentPositions[i] : this.getPos(); }
    public float getSegYaw(int i) { return i < segmentYaws.length ? segmentYaws[i] : 0; }
    public float getSegPitch(int i) { return i < segmentPitches.length ? segmentPitches[i] : 0; }
    @Override public boolean collidesWith(Entity other) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("TargetUUID")) this.cachedTargetUuid = nbt.getUuid("TargetUUID");
        if (nbt.contains("capX")) this.capturePos = new Vec3d(nbt.getDouble("capX"), nbt.getDouble("capY"), nbt.getDouble("capZ"));
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (cachedTargetUuid != null) nbt.putUuid("TargetUUID", cachedTargetUuid);
        if (capturePos != null) {
            nbt.putDouble("capX", capturePos.x); nbt.putDouble("capY", capturePos.y); nbt.putDouble("capZ", capturePos.z);
        }
    }
}