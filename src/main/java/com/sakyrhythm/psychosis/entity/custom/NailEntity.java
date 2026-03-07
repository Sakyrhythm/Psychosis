package com.sakyrhythm.psychosis.entity.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NailEntity extends Entity {
    private static final TrackedData<Integer> SPAWN_DELAY = DataTracker.registerData(NailEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private Vec3d targetPos;
    private LivingEntity targetEntity;
    private int lifeTicks = 0;
    private int deathTicks = 0;
    private boolean hasHit = false;
    private final float nailLength = 1.2f;
    private final Set<Integer> hitEntities = new HashSet<>();

    public NailEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setTargetEntity(LivingEntity entity) {
        this.targetEntity = entity;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPAWN_DELAY, 35);
    }

    public int getSpawnDelay() {
        return this.dataTracker.get(SPAWN_DELAY);
    }

    @Override
    public void tick() {
        if (hasHit) {
            this.setVelocity(Vec3d.ZERO);
            if (!this.getWorld().isClient()) {
                deathTicks++;
                if (deathTicks > 2) this.discard();
            }
            return;
        }

        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();
        this.prevPitch = this.getPitch();
        this.prevYaw = this.getYaw();

        int delay = getSpawnDelay();

        // --- 阶段 1: 追踪预警 ---
        if (delay > 0) {
            if (!this.getWorld().isClient()) {
                if (delay == 35) {
                    this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.5f, 0.5f);
                }
                this.dataTracker.set(SPAWN_DELAY, delay - 1);
            }
            if (targetEntity != null && targetEntity.isAlive()) {
                BlockPos ground = findGroundUnder(targetEntity);
                if (ground != null) this.targetPos = new Vec3d(ground.getX() + 0.5, ground.getY() + 1.3, ground.getZ() + 0.5);
            }
            if (this.getWorld().isClient()) {
                for (int i = 0; i < 2; i++) {
                    double rx = (this.random.nextDouble() - 0.5) * 0.5;
                    double ry = (this.random.nextDouble() - 0.5) * 0.5;
                    double rz = (this.random.nextDouble() - 0.5) * 0.5;
                    this.getWorld().addParticle(ParticleTypes.DRAGON_BREATH, this.getX() + rx, this.getY() + ry, this.getZ() + rz, 0, 0, 0);
                }
            }
            return;
        }

        // --- 阶段 2: 运动处理 ---
        if (targetPos != null) {
            Vec3d dir = targetPos.subtract(this.getPos()).normalize();
            double speed;
            if (lifeTicks < 15) {
                speed = 0.01;
                if (this.getWorld().isClient()) {
                    this.getWorld().addParticle(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                    if (lifeTicks % 5 == 0) this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.HOSTILE, 1.0f, 2.0f);
                }
            } else {
                if (lifeTicks == 15) {
                    this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0f, 1.2f);
                    if (this.getWorld().isClient()) this.getWorld().addParticle(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                }
                int t = lifeTicks - 15;
                speed = Math.min(0.1 + (0.07 * t * t), 12.0);
            }
            this.setVelocity(dir.multiply(speed));
            updateRotationFromDir(dir);
        }

        // --- 阶段 3: 复合碰撞判定 (关键修改) ---
        Vec3d velocity = this.getVelocity();
        if (lifeTicks > 15 && !this.getWorld().isClient()) {
            performCollisionDetection(velocity);
        }

        this.move(MovementType.SELF, velocity);
        lifeTicks++;

        // 粒子逻辑
        if (this.getWorld().isClient() && lifeTicks > 15) {
            for(int i = 0; i < 6; i++) {
                double ox = (this.random.nextDouble() - 0.5) * 0.3;
                double oy = (this.random.nextDouble() - 0.5) * 0.3;
                double oz = (this.random.nextDouble() - 0.5) * 0.3;
                this.getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0, 0);
                this.getWorld().addParticle(ParticleTypes.GLOW, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0, 0);
            }
        }

        // 落地检测
        if (!this.getWorld().isClient()) {
            if (this.horizontalCollision || this.verticalCollision || this.isOnGround() || (targetPos != null && this.getPos().distanceTo(targetPos) < 0.8) || lifeTicks > 250) {
                this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1.0f, 1.8f);
                this.hasHit = true;
            }
        }
    }

    private void performCollisionDetection(Vec3d velocity) {
        Vec3d start = this.getPos();
        Vec3d end = start.add(velocity);

        // 1. 射线检测 (精准打击)
        EntityHitResult entityHit = ProjectileUtil.raycast(this, start, end,
                this.getBoundingBox().stretch(velocity).expand(0.5),
                entity -> !entity.isSpectator() && entity.isAlive() && entity.canHit(),
                start.squaredDistanceTo(end));

        if (entityHit != null) {
            onTargetHit(entityHit.getEntity());
        }

        // 2. 扫掠碰撞箱检测 (解决“周围一圈”判定)
        // stretch(velocity) 包含移动路径，expand(0.8) 增加周围判定半径
        Box sweepBox = this.getBoundingBox().stretch(velocity).expand(0.8);
        List<Entity> nearbyEntities = this.getWorld().getOtherEntities(this, sweepBox,
                entity -> entity instanceof LivingEntity && entity.isAlive() && !hitEntities.contains(entity.getId()));

        for (Entity entity : nearbyEntities) {
            onTargetHit(entity);
        }

        // 3. 地形检测
        BlockHitResult blockHit = this.getWorld().raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            this.hasHit = true;
        }
    }

    private void onTargetHit(Entity entity) {
        if (entity instanceof LivingEntity living && !hitEntities.contains(living.getId())) {
            DamageSource source = this.getDamageSources().thrown(this, null);
            if (living.damage(source, 10.0f)) {
                hitEntities.add(living.getId());
                this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 2.0f, 1.5f);

                // 如果是 Boss，钉子会留下（停止运动）
                if (living instanceof ScytheBossEntity) {
                    this.hasHit = true;
                }
            }
        }
    }

    private void updateRotationFromDir(Vec3d dir) {
        double hLength = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        this.setYaw((float) (MathHelper.atan2(dir.x, dir.z) * (180.0 / Math.PI)));
        this.setPitch((float) (MathHelper.atan2(dir.y, hLength) * (180.0 / Math.PI)));
    }

    private BlockPos findGroundUnder(Entity entity) {
        World world = entity.getWorld();
        BlockPos startPos = entity.getBlockPos();
        for (int i = 0; i < 25; i++) {
            BlockPos checkPos = startPos.down(i);
            if (world.getBlockState(checkPos).isFullCube(world, checkPos)) return checkPos;
        }
        return null;
    }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { this.lifeTicks = nbt.getInt("LifeTicks"); this.hasHit = nbt.getBoolean("HasHit"); }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { nbt.putInt("LifeTicks", this.lifeTicks); nbt.putBoolean("HasHit", this.hasHit); }
    @Override public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) { return new EntitySpawnS2CPacket(this, entry); }
}