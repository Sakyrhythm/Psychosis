package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.world.WaterColumnManager;
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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
    private Vec3d capturePos;
    private int captureStartTime = -1;
    private boolean isDragging = false;

    public TentacleEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.ignoreCameraFrustum = true;
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
        boolean isGiant = this.getCommandTags().contains("giant_tentacle");

        // 消失判定逻辑
        int maxAge = isGiant ? 800 : 450;
        if (!(targetRaw instanceof LivingEntity target) || !target.isAlive() || this.age > maxAge) {
            if (!getWorld().isClient && targetRaw instanceof LivingEntity t) {
                t.removeCommandTag("has_tentacle");
                t.removeStatusEffect(StatusEffects.SLOWNESS);
                t.removeStatusEffect(StatusEffects.DARKNESS);
            }
            this.discard();
            return;
        }

        this.cachedTargetUuid = target.getUuid();
        if (isGiant && captureStartTime == -1) captureStartTime = this.age;

        // --- 逻辑阶段处理 ---
        if (isGiant) {
            boolean isCapturePhase = this.age - captureStartTime < 60;
            if (isCapturePhase) {
                handleCapturePhase(target, MathHelper.clamp((this.age - captureStartTime) / 60.0f, 0.0f, 1.0f));
            } else {
                if (!isDragging) { isDragging = true; this.capturePos = target.getPos(); }
                float dragProgress = MathHelper.clamp((this.age - captureStartTime - 60) / 200.0f, 0.0f, 1.0f);
                handleDragPhase(target, dragProgress);
                if (dragProgress >= 1.0f) executeTarget(target, true);
            }
        } else {
            float tightenProgress = MathHelper.clamp((this.age - 60) / 160.0f, 0.0f, 1.0f);
            if (!getWorld().isClient) {
                immobilizeAndShakeOriginal(target, tightenProgress);
                if (tightenProgress >= 1.0f) executeTarget(target, false);
            }
        }

        // --- 核心：身体节计算（含大小动态变化） ---
        float globalScale = 1.0f;
        if (isGiant) {
            // 呼吸律动感
            float pulse = (float) Math.sin(this.age * 0.15f) * 0.07f;
            // 生长感 (刚出现的前2秒逐渐充盈)
            float growth = MathHelper.clamp(this.age / 40.0f, 0.7f, 1.0f);
            // 力量感 (拖拽时触手紧绷变粗)
            float exertion = isDragging ? 0.28f : 0.0f;
            globalScale = growth + pulse + exertion;
        }

        for (int i = 0; i < count; i++) {
            float baseCurve;
            if (isGiant) {
                float tipProgress = (float)i / count;
                // 应用缩放因子到基础曲线
                baseCurve = (0.15f + tipProgress * tipProgress * 0.45f) * globalScale;
            } else {
                baseCurve = 0.22f + (0.75f / (1.0f + 0.2f * i));
            }

            // 缠绕部分尖细化
            if (i >= count - (isGiant ? 60 : 40)) {
                float shrinkFactor = isGiant ? 0.988f : 0.98f;
                baseCurve *= (float) Math.pow(shrinkFactor, (i - (count - (isGiant ? 60 : 40))));
            }

            // 动态间距调整，防止粗细变化时模型重叠
            float spacing = isGiant ? 1.25f : 0.75f;
            spacing = spacing * baseCurve * 1.35f * (isGiant ? globalScale : 1.0f);

            Vec3d targetPoint;
            if (i >= count - (isGiant ? 60 : 40)) {
                double angleMultiplier = isGiant ? 1.2 : 0.7;
                double ageMultiplier = isGiant ? 0.08 : 0.05;
                double angle = i * angleMultiplier + (this.age * ageMultiplier);

                double baseRadius;
                if (isGiant) {
                    if (this.age - captureStartTime < 60) {
                        float captureProgress = MathHelper.clamp((this.age - captureStartTime) / 60.0f, 0.0f, 1.0f);
                        baseRadius = 2.0 - captureProgress * 1.0;
                    } else {
                        // 拖拽时紧缚，半径随呼吸律动
                        baseRadius = 0.9 + (globalScale - 1.0) * 0.2;
                    }
                } else {
                    baseRadius = 0.45;
                }

                double radiusPerSegment = isGiant ? 0.012 : 0.005;
                double currentRadius = baseRadius + (count - i) * radiusPerSegment;
                double heightFactor = isGiant ? 0.08 : 0.06;
                double heightOffset = (i - (count - (isGiant ? 60 : 40))) * heightFactor + 0.2;

                targetPoint = target.getPos().add(
                        Math.cos(angle) * currentRadius,
                        heightOffset,
                        Math.sin(angle) * currentRadius
                );
            } else {
                targetPoint = target.getPos().add(0, target.getHeight() * (isGiant ? 0.45 : 0.5), 0);
            }

            // 位置与旋转更新
            if (i == 0) {
                segmentPositions[i] = Vec3d.ofCenter(ownerPos);
                this.updatePosition(segmentPositions[i].x, segmentPositions[i].y, segmentPositions[i].z);
            } else {
                Vec3d prevPos = segmentPositions[i - 1];
                Vec3d dir = targetPoint.subtract(prevPos);
                if (dir.lengthSquared() < 0.0001) dir = new Vec3d(0, 1, 0);
                segmentPositions[i] = prevPos.add(dir.normalize().multiply(spacing));
            }

            Vec3d lookDir = targetPoint.subtract(segmentPositions[i]);
            if (lookDir.lengthSquared() > 0.001) {
                segmentYaws[i] = (float) Math.toDegrees(MathHelper.atan2(-lookDir.x, lookDir.z));
                double horiz = MathHelper.sqrt((float) (lookDir.x * lookDir.x + lookDir.z * lookDir.z));
                segmentPitches[i] = (float) Math.toDegrees(MathHelper.atan2(-lookDir.y, horiz));
            }
        }

        // 巨型触手粒子效果
        if (isGiant && getWorld().isClient && this.age % 5 == 0) {
            for (int i = 0; i < 10; i++) {
                int rndIdx = random.nextInt(count);
                Vec3d p = segmentPositions[rndIdx];
                getWorld().addParticle(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, (random.nextDouble()-0.5)*0.2, 0.1, (random.nextDouble()-0.5)*0.2);
            }
        }
    }

    private void immobilizeAndShakeOriginal(LivingEntity target, float progress) {
        float shake = (progress > 0.1f) ? 0.02f + (progress * 0.08f) : 0f;
        double rx = (random.nextDouble() - 0.5) * shake;
        double ry = (random.nextDouble() - 0.5) * shake;
        double rz = (random.nextDouble() - 0.5) * shake;

        if (target instanceof ServerPlayerEntity player) {
            if (progress > 0.1f) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, (int)(progress*15), false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 15, 0, false, false, false));
            }
            player.networkHandler.requestTeleport(capturePos.x + rx, capturePos.y + ry, capturePos.z + rz, player.getYaw() + (float)rx * 90, player.getPitch() + (float)ry * 90, Collections.emptySet());
        } else {
            target.requestTeleport(capturePos.x + rx, capturePos.y + ry, capturePos.z + rz);
            target.setVelocity(0, 0, 0);
            target.velocityModified = true;
        }
    }

    private void handleCapturePhase(LivingEntity target, float progress) {
        if (getWorld().isClient) return;
        if (target instanceof ServerPlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 5, false, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10, 2, false, false, false));
            if (capturePos == null) capturePos = player.getPos();
            Vec3d newPos = player.getPos().lerp(capturePos, 0.02 * progress);
            player.setVelocity(newPos.subtract(player.getPos()));
            player.velocityModified = true;
        } else {
            target.setVelocity(0, 0, 0);
            target.velocityModified = true;
        }
    }

    private void handleDragPhase(LivingEntity target, float progress) {
        if (getWorld().isClient) return;
        if (target instanceof ServerPlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 10, false, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20, 1, false, false, false));
            if (capturePos == null) capturePos = player.getPos();
            double pullDown = progress * 15.0;
            player.networkHandler.requestTeleport(capturePos.x, capturePos.y - pullDown, capturePos.z, player.getYaw(), player.getPitch(), Collections.emptySet());
            if (this.age % 10 == 0) ((ServerWorld)getWorld()).spawnParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        } else {
            target.requestTeleport(capturePos.x, capturePos.y - (progress * 15.0), capturePos.z);
            target.setVelocity(0, -0.5, 0);
            target.velocityModified = true;
        }
    }

    private void executeTarget(LivingEntity target, boolean isGiant) {
        if (getWorld() instanceof ServerWorld serverWorld) {
            Identifier oceanId = Identifier.of("psychosis", "the_ocean");
            if (!Psychosis.isTheOcean(serverWorld)) {
                ServerWorld oceanWorld = serverWorld.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, oceanId));
                if (oceanWorld != null) { executeInOceanWorld(oceanWorld, target, isGiant); return; }
            }
            executeInOceanWorld(serverWorld, target, isGiant);
        }
        this.discard();
    }

    private void executeInOceanWorld(ServerWorld oceanWorld, LivingEntity target, boolean isGiant) {
        oceanWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1, target.getZ(), isGiant ? 150 : 60, 1.0, 1.0, 1.0, 0.1);
        if (isGiant) oceanWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 1, target.getZ(), 5, 0, 0, 0, 0);
        oceanWorld.playSound(null, target.getBlockPos(), isGiant ? SoundEvents.ENTITY_WARDEN_DEATH : SoundEvents.ENTITY_GENERIC_DEATH, SoundCategory.HOSTILE, isGiant ? 3.0f : 1.0f, 0.5f);

        target.removeCommandTag("has_tentacle");
        if (target instanceof ServerPlayerEntity player) {
            double spawnY = isGiant ? WaterColumnManager.BOTTOM_WATER_TOP + 2 : WaterColumnManager.getDefaultSpawnY();
            player.teleport(oceanWorld, 0.5, spawnY, 0.5, player.getYaw(), player.getPitch());
            if (isGiant) player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 400, 1, false, false, false));
            oceanWorld.playSound(null, new BlockPos(0, (int)spawnY, 0), isGiant ? SoundEvents.ENTITY_WARDEN_ROAR : SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 1.5f, 0.6f);
        } else {
            target.discard();
        }
    }

    public UUID getTargetUuid() { return cachedTargetUuid; }
    public int getSegCount() { return segmentPositions.length; }
    public Vec3d getSegPos(int i) { return i < segmentPositions.length ? segmentPositions[i] : this.getPos(); }
    public float getSegYaw(int i) { return i < segmentYaws.length ? segmentYaws[i] : 0; }
    public float getSegPitch(int i) { return i < segmentPitches.length ? segmentPitches[i] : 0; }

    @Override public boolean collidesWith(Entity other) { return false; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("TargetUUID")) this.cachedTargetUuid = nbt.getUuid("TargetUUID");
        if (nbt.contains("capX")) this.capturePos = new Vec3d(nbt.getDouble("capX"), nbt.getDouble("capY"), nbt.getDouble("capZ"));
        this.captureStartTime = nbt.getInt("CaptureStartTime");
        this.isDragging = nbt.getBoolean("IsDragging");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (cachedTargetUuid != null) nbt.putUuid("TargetUUID", cachedTargetUuid);
        if (capturePos != null) {
            nbt.putDouble("capX", capturePos.x); nbt.putDouble("capY", capturePos.y); nbt.putDouble("capZ", capturePos.z);
        }
        nbt.putInt("CaptureStartTime", captureStartTime);
        nbt.putBoolean("IsDragging", isDragging);
    }
}