package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.item.DarkSwordItem;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
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
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public class FlatDartProjectile extends PersistentProjectileEntity {
    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );
    RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
            .getEntry(DARK_EFFECT_KEY)
            .orElse(null);
    private static final float FIXED_DAMAGE = 6.0F;

    private static final Logger LOGGER = LoggerFactory.getLogger("FlatDart");

    private int lifeTime = 0;
    private static final int MAX_LIFE_TICKS = 20;

    private ItemStack launcherStack = ItemStack.EMPTY;
    private Hand handUsed = Hand.MAIN_HAND;

    // 用于记录已经击中过哪些实体，防止重复伤害 (实现穿透但只伤害一次)
    private final java.util.Set<Entity> hitEntities = new java.util.HashSet<>();

    // 构造函数 1: 必须的 EntityType 构造函数
    public FlatDartProjectile(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);

        this.setNoGravity(true);
        this.setInvisible(true);
        this.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
    }

    // 构造函数 3: 满足 DarkSwordItem 调用的签名 (用于投掷)
    public FlatDartProjectile(World world, LivingEntity shooter, ItemStack launcher, Hand hand) {
        this(ModEntities.FLAT_DART, world);

        this.setOwner(shooter);
        this.launcherStack = launcher.copy();
        this.handUsed = hand;

        this.setNoGravity(true);
        this.setInvisible(true);
    }

    // =================================================================================
    // 核心逻辑：tick() 方法
    // (保持不变)
    // =================================================================================

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            // 步骤 1: 计时销毁逻辑
            lifeTime++;
            if (lifeTime >= MAX_LIFE_TICKS) {
                this.discard();
                return;
            }

            // 步骤 2: 获取下一刻的位置
            Vec3d velocity = this.getVelocity();
            Vec3d currentPos = this.getPos();
            Vec3d nextPos = currentPos.add(velocity);

            // 步骤 3: 手动检测实体碰撞
            EntityHitResult entityHit = this.findHitEntity(currentPos, nextPos);

            if (entityHit != null) {
                // 如果是第一次击中该实体，则处理伤害
                if (!hitEntities.contains(entityHit.getEntity())) {

                    // 【关键修正点】：直接调用我们重写的方法，而不是基类的 onHit(LivingEntity)
                    // 这样既能触发伤害逻辑，又避免了类型错误。
                    this.onEntityHit(entityHit);

                    hitEntities.add(entityHit.getEntity()); // 记录已击中
                }
            }

            // 步骤 4: 更新位置
            this.setPos(nextPos.x, nextPos.y, nextPos.z);
        }
    }

    // 自定义实体碰撞检测方法
    @Nullable
    protected EntityHitResult findHitEntity(Vec3d currentPos, Vec3d nextPos) {
        Vec3d delta = nextPos.subtract(currentPos);

        Box searchBox = this.getBoundingBox().stretch(delta).expand(1.0);

        // 查找范围内的所有实体
        List<Entity> list = this.getWorld().getOtherEntities(this, searchBox, this::canHit);

        EntityHitResult closestHit = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (Entity entity : list) {
            // 避免击中已经击中过的实体
            if (hitEntities.contains(entity)) {
                continue;
            }

            // 使用 raycast 检查投射物是否真正击中实体
            Optional<Vec3d> hitVec = entity.getBoundingBox().raycast(currentPos, nextPos);

            if (hitVec.isPresent()) {
                double distSq = currentPos.squaredDistanceTo(hitVec.get());
                if (distSq < closestDistanceSq) {
                    closestDistanceSq = distSq;
                    closestHit = new EntityHitResult(entity, hitVec.get());
                }
            } else if (entity.getBoundingBox().intersects(this.getBoundingBox())) {
                // 处理实体已经包含在投射物内部的情况
                double distSq = currentPos.squaredDistanceTo(entity.getPos());
                if (distSq < closestDistanceSq) {
                    closestDistanceSq = distSq;
                    closestHit = new EntityHitResult(entity);
                }
            }
        }

        return closestHit;
    }

    // 覆盖 canHit，允许击中 LivingEntity
    @Override
    protected boolean canHit(Entity entity) {
        if (entity == this.getOwner()) {
            return false;
        }
        return entity.isAlive() && (entity instanceof LivingEntity || super.canHit(entity));
    }


    // =================================================================================
    // 碰撞处理：onEntityHit (已修正施加效果逻辑)
    // =================================================================================


    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld().isClient()) return;
        Entity target = entityHitResult.getEntity();

        if (target instanceof LivingEntity livingTarget && this.getOwner() instanceof PlayerEntity shooter) {

            float finalDamage = FIXED_DAMAGE;

            // --- 伤害增强逻辑 ---

            // 1. 确保效果注册入口不为空
            if (darkEffectEntry != null) {

                // 2. 检查玩家是否拥有该效果
                if (shooter.hasStatusEffect(darkEffectEntry)) {

                    net.minecraft.entity.effect.StatusEffectInstance effectInstance = shooter.getStatusEffect(darkEffectEntry);

                    if (effectInstance != null) {
                        // 等级从 0 开始计数，因此 level = amplifier + 1
                        int amplifier = effectInstance.getAmplifier();
                        int effectLevel = amplifier + 1;

                        // 计算增强伤害：每层药水效果，伤害值 + 2.0F (与 Dart 保持一致，但这里是 2.0F)
                        finalDamage += effectLevel * 2.0F;
                        LOGGER.info("Dark Power detected (Level {}). Damage increased to {}." , effectLevel, finalDamage);
                    }
                }
            }

            // ==========================================================
            // 【修正逻辑】：条件施加黑暗纠缠效果
            // ==========================================================
            if (darkEffectEntry != null) {
                // 检查目标是否已经拥有该效果
                if (!livingTarget.hasStatusEffect(darkEffectEntry)) {
                    final int DURATION_TICKS = 10;
                    final int AMPLIFIER = 0;

                    livingTarget.addStatusEffect(new StatusEffectInstance(
                            darkEffectEntry,
                            DURATION_TICKS,
                            AMPLIFIER,
                            false,
                            false,
                            true
                    ));
                    LOGGER.info("Applied Dark Entanglement ({} ticks) to {} due to FlatDart hit.", DURATION_TICKS, livingTarget.getName().getString());
                } else {
                    LOGGER.info("{} already has Dark Entanglement. Skipping reapplication.", livingTarget.getName().getString());
                }
            }
            // ==========================================================

            DamageSource src = this.getDamageSources().arrow(this, shooter);
            livingTarget.damage(src, finalDamage);
        }
    }

    // =================================================================================
    // 碰撞处理：onBlockHit
    // =================================================================================

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        // 剑气击中方块后应销毁
        LOGGER.info("FlatDart hit block, discarding.");
        this.discard();
    }

    // =================================================================================
    // 其他方法
    // =================================================================================

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.VOID_ESSENCE);
    }
}