package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.item.DarkSwordItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;

import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DarkDartProjectile extends PersistentProjectileEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("DarkDart");

    private static final float FIXED_DAMAGE = 15.0F;
    private static final double MAX_DISTANCE = 400.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final float EXPLOSION_POWER = 2.0F;
    private static final float INSTANT_SPEED = 1000.0F; // 保持高速，依赖Tick处理命中

    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );
    RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
            .getEntry(DARK_EFFECT_KEY)
            .orElse(null);


    private ItemStack launcherStack = ItemStack.EMPTY;
    private Hand handUsed = Hand.MAIN_HAND;

    // 构造函数 1
    public DarkDartProjectile(EntityType<? extends DarkDartProjectile> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.setInvisible(true);
        this.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
    }

    // 构造函数 2 (核心逻辑)
    public DarkDartProjectile(World world, LivingEntity shooter, ItemStack launcher, Hand hand) {

        super(ModEntities.DARK_DART_PROJECTILE, shooter, world, ItemStack.EMPTY, launcher);

        this.launcherStack = launcher.copy();
        this.handUsed = hand;

        Vec3d lookVec = shooter.getRotationVector();

        this.setPosition(shooter.getX() + lookVec.getX() * 0.05,
                shooter.getEyeY() - 0.1,
                shooter.getZ() + lookVec.getZ() * 0.05);

        this.setNoGravity(true);
        this.setInvisible(true);
        this.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;

        // *** 核心：执行预检查，根据结果决定是否发射超高速实体 ***
        if (!this.getWorld().isClient() && this.getOwner() != null) {
            if (performPreCheckRaycast(lookVec)) {
                // 检查通过：设置高速，依赖 Tick 进行碰撞检测
                this.setVelocity(lookVec.getX() * INSTANT_SPEED,
                        lookVec.getY() * INSTANT_SPEED,
                        lookVec.getZ() * INSTANT_SPEED);
                this.velocityDirty = true;
                LOGGER.info("Dart Check Passed. Launched at high speed.");
            } else {
                // 检查失败（虚空/远距离）：不设置速度，立即销毁，避免卡顿
                this.setVelocity(0, 0, 0);
                this.discard();
                LOGGER.info("Dart Check Failed (Void/Max Distance). Discarded immediately.");
            }
        }
    }

    /**
     * 预检查 Raycast：检查路径上是否有目标（实体或距离内的方块）。
     */
    private boolean performPreCheckRaycast(Vec3d lookVec) {
        final Vec3d currentPos = this.getPos();
        Vec3d maxDest = currentPos.add(lookVec.multiply(MAX_DISTANCE));

        // 1. 块碰撞 Raycasting (检查距离内的方块)
        RaycastContext blockContext = new RaycastContext(
                currentPos,
                maxDest,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this
        );
        BlockHitResult blockHit = this.getWorld().raycast(blockContext);
        double blockHitDistSq = blockHit.getPos().squaredDistanceTo(currentPos);

        // 2. 实体碰撞检查 (检查实体)
        EntityHitResult entityHit = findEntityHit(currentPos, maxDest);
        double entityHitDistSq = entityHit != null ? entityHit.getPos().squaredDistanceTo(currentPos) : Double.MAX_VALUE;

        // 判定条件：
        // 1. 命中实体 (entityHit != null)
        // 2. 命中方块且方块距离在最大距离内 (blockHitDistSq < MAX_DISTANCE_SQUARED)
        if (entityHit != null || blockHitDistSq < MAX_DISTANCE_SQUARED) {
            // 确保最近的命中不是方块，并且实体比方块更近 (如果都存在)
            if (entityHit != null && entityHitDistSq < blockHitDistSq) {
                return true; // 实体是最近的目标
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                return true; // 方块是最近的目标
            }
        }

        // 未命中任何实体，且方块命中点在最大距离之外 (即命中虚空终点)
        return false;
    }


    // *** findEntityHit 方法（保持不变，使用贴脸修正） ***
    private EntityHitResult findEntityHit(Vec3d start, Vec3d end) {
        final Vec3d finalStart = start;

        if (start.equals(end)) return null;

        double distance = start.distanceTo(end);
        Vec3d direction = end.subtract(start).normalize();

        final Box searchBox = this.getBoundingBox().stretch(direction.multiply(distance)).expand(1.0);

        final Entity[] closestEntity = {null};
        final double[] closestDistanceSq = {Double.MAX_VALUE};
        final Box projectileBox = this.getBoundingBox();

        this.getWorld().getOtherEntities(this, searchBox, entity ->
                entity != this.getOwner() && !entity.isSpectator() && entity.isCollidable()
        ).forEach(entity -> {
            Optional<Vec3d> hitOptional = entity.getBoundingBox().raycast(finalStart, end);
            double distSq = Double.MAX_VALUE;

            if (hitOptional.isPresent()) {
                Vec3d hitPos = hitOptional.get();
                distSq = hitPos.squaredDistanceTo(finalStart);

            } else if (entity.getBoundingBox().intersects(projectileBox)) {
                // 贴脸修正
                distSq = 0.0001;
            }

            if (distSq < closestDistanceSq[0]) {
                closestDistanceSq[0] = distSq;
                closestEntity[0] = entity;
            }
        });

        if (closestEntity[0] != null) {
            if (closestDistanceSq[0] < 0.001) {
                return new EntityHitResult(closestEntity[0], finalStart);
            }
            return new EntityHitResult(closestEntity[0], finalStart.add(direction.multiply(Math.sqrt(closestDistanceSq[0]))));
        }

        return null;
    }
    // *** end findEntityHit ***


    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) return;

        // 检查速度是否被设置（即 Pre-Check 通过）
        if (this.getVelocity().lengthSquared() < 1.0) {
            // 如果速度为零，说明 Pre-Check 失败，实体应该已经被 discard()
            return;
        }

        // 1. 距离检查：强制最大距离限制 (如果 Tick 中仍存在)
        if (this.age > 0 && this.getOwner() != null) {
            double distanceSq = this.getOwner().squaredDistanceTo(this);

            // 如果超出最大距离，或者超过 1 个 Tick 仍未命中，强制销毁 (依赖 Pre-Check 减轻了负担)
            if (distanceSq > MAX_DISTANCE_SQUARED || this.age >= 2) {
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 5, 0.1, 0.1, 0.1, 0.0);
                }
                this.discard();
            }
        }

        // 注意：命中检测由 super.tick() 内部处理
    }

    // *** 修正后的 onEntityHit 方法 ***
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld().isClient()) return;
        Entity target = entityHitResult.getEntity();

        if (target instanceof LivingEntity livingTarget && this.getOwner() instanceof PlayerEntity shooter) {

            float finalDamage = FIXED_DAMAGE;

            // --- 伤害增强逻辑 ---

            // 1. 通过注册表获取药水效果实例
            net.minecraft.entity.effect.StatusEffect darkPowerEffect =
                    net.minecraft.registry.Registries.STATUS_EFFECT.get(DARK_EFFECT_KEY);

            if (darkPowerEffect != null && shooter.hasStatusEffect(darkEffectEntry)) {

                net.minecraft.entity.effect.StatusEffectInstance effectInstance = shooter.getStatusEffect(darkEffectEntry);

                if (effectInstance != null) {
                    // 等级从 0 开始计数，因此 level = amplifier + 1
                    int amplifier = effectInstance.getAmplifier();
                    int effectLevel = amplifier + 1;

                    // 计算增强伤害：每层药水效果，伤害值 + 3.0F
                    finalDamage += effectLevel * 3.0F;
                    LOGGER.info("Dark Power detected (Level {}). Damage increased to {}." , effectLevel, finalDamage);
                }
            }
            // --- 结束伤害增强逻辑 ---


            DamageSource src = this.getDamageSources().arrow(this, shooter);

            // 使用计算后的最终伤害值
            livingTarget.damage(src, finalDamage);

            if (!this.launcherStack.isEmpty()) {
                DarkSwordItem.saveAnchor(
                        this.launcherStack,
                        shooter,
                        this.handUsed,
                        this.getWorld(),
                        target.getPos(),
                        target.getId()
                );
            }
        }
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getWorld().isClient()) return;

        if (this.getOwner() instanceof PlayerEntity shooter) {
            Vec3d hitPos = blockHitResult.getPos();
            double hitDistanceSq = hitPos.squaredDistanceTo(this.getOwner().getPos());

            if (hitDistanceSq >= MAX_DISTANCE_SQUARED) {
                this.discard();
                return;
            }

            BlockPos groundCheckPos = BlockPos.ofFloored(hitPos).down();
            boolean hasSolidGroundBelow = this.getWorld().getBlockState(groundCheckPos).isSolidBlock(this.getWorld(), groundCheckPos);

            if (hasSolidGroundBelow) {
                if (!this.launcherStack.isEmpty()) {
                    DarkSwordItem.saveAnchor(
                            this.launcherStack,
                            shooter,
                            this.handUsed,
                            this.getWorld(),
                            hitPos,
                            -1
                    );
                }
            } else {
                this.getWorld().createExplosion(
                        this,
                        hitPos.getX(),
                        hitPos.getY(),
                        hitPos.getZ(),
                        EXPLOSION_POWER,
                        World.ExplosionSourceType.NONE
                );
            }
        }
        this.discard();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.IRON_SWORD);
    }

    @Override
    protected void onHit(LivingEntity target) {
        // 强制实现 PersistentProjectileEntity 的要求
    }
}