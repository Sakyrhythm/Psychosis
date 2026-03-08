package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FlatDartProjectile extends PersistentProjectileEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlatDart");

    // --- 常量 ---
    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );
    private static final float BASE_DAMAGE = 6.0F; // 基础伤害
    private static final float DAMAGE_PER_DARK_LEVEL = 2.0F; // 每级黑暗效果提供的额外伤害
    private static final int MAX_LIFE_TICKS = 60; // 存活时间 1 秒

    // --- 状态 ---
    private final Set<Entity> hitEntities = new HashSet<>(); // 记录已击中实体，实现单次伤害穿透
    private int lifeTime = 0;

    // 用于 Boss Counter 逻辑（未在 onEntityHit 中使用，但保留接口）
    private boolean isBossCounter = false;
    private float counterDamagePercent = 0.0F;
    private int counterDarkAmplifier = 0;
    private LivingEntity bossOwner = null;

    // 效果入口缓存
    RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
            .getEntry(DARK_EFFECT_KEY)
            .orElse(null);

    // =================================================================================
    // 构造函数
    // =================================================================================

    // 必须的 EntityType 构造函数
    public FlatDartProjectile(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.initProjectile();
    }

    // 用于投掷的构造函数
    public FlatDartProjectile(World world, LivingEntity shooter, ItemStack launcher, Hand hand) {
        this(ModEntities.FLAT_DART, world);
        this.setOwner(shooter);
        // launcherStack 和 handUsed 可以在需要时保留，目前未使用，故省略赋值
    }

    private void initProjectile() {
        this.setNoGravity(true);
        this.setInvisible(true);
        this.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
    }

    // =================================================================================
    // 核心逻辑：tick() 方法 (穿透实现)
    // =================================================================================

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            // 步骤 1: 计时销毁
            if (++lifeTime >= MAX_LIFE_TICKS) {
                this.discard();
                return;
            }

            Vec3d currentPos = this.getPos();
            Vec3d velocity = this.getVelocity();
            Vec3d nextPos = currentPos.add(velocity);

            // 步骤 2: 手动检测并处理碰撞
            EntityHitResult entityHit = this.findHitEntity(currentPos, nextPos);

            if (entityHit != null) {
                Entity target = entityHit.getEntity();
                // 只处理第一次击中该实体的情况
                if (!hitEntities.contains(target)) {
                    this.onEntityHit(entityHit);
                    hitEntities.add(target);
                }
            }

            // 步骤 3: 更新位置 (实现穿透)
            this.setPos(nextPos.x, nextPos.y, nextPos.z);
        }
    }

    // 自定义实体碰撞检测方法 (检测未击中过的实体)
    @Nullable
    protected EntityHitResult findHitEntity(Vec3d currentPos, Vec3d nextPos) {
        Vec3d delta = nextPos.subtract(currentPos);
        // 搜索范围略微扩大，以捕获边缘碰撞
        Box searchBox = this.getBoundingBox().stretch(delta).expand(1.0);

        // 查找范围内的所有实体，并排除投射物拥有者和已经击中过的实体
        List<Entity> list = this.getWorld().getOtherEntities(this, searchBox, entity ->
                this.canHit(entity) && !hitEntities.contains(entity)
        );

        EntityHitResult closestHit = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (Entity entity : list) {
            // 使用 raycast 或包围盒相交来判断是否命中
            Optional<Vec3d> hitVec = entity.getBoundingBox().raycast(currentPos, nextPos);

            if (hitVec.isPresent()) {
                double distSq = currentPos.squaredDistanceTo(hitVec.get());
                if (distSq < closestDistanceSq) {
                    closestDistanceSq = distSq;
                    closestHit = new EntityHitResult(entity, hitVec.get());
                }
            }
            // 简化：如果实体在投射物运动轨迹上的包围盒内，也视为命中（处理近距离或大体积目标）
            else if (entity.getBoundingBox().intersects(this.getBoundingBox())) {
                if (currentPos.squaredDistanceTo(entity.getPos()) < closestDistanceSq) {
                    closestHit = new EntityHitResult(entity);
                    closestDistanceSq = 0.0; // 内部命中，设为最近
                }
            }
        }
        return closestHit;
    }

    // =================================================================================
    // 碰撞处理：onEntityHit (剑气伤害与效果逻辑)
    // =================================================================================

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld().isClient()) return;

        Entity target = entityHitResult.getEntity();
        LivingEntity shooter = (LivingEntity) this.getOwner();

        // 确保目标是 LivingEntity 且拥有者存在
        if (target instanceof LivingEntity livingTarget && shooter != null) {

            float finalDamage = BASE_DAMAGE;

            // 1. 伤害增强逻辑：如果射击者是玩家且带有 'dark' 效果
            if (shooter instanceof PlayerEntity playerShooter && darkEffectEntry != null) {
                StatusEffectInstance effectInstance = playerShooter.getStatusEffect(darkEffectEntry);

                if (effectInstance != null) {
                    int effectLevel = effectInstance.getAmplifier() + 1; // 等级 = Amplifier + 1
                    finalDamage += effectLevel * DAMAGE_PER_DARK_LEVEL;
                    LOGGER.info("Dark Power (Lvl {}). Damage increased to {}." , effectLevel, finalDamage);
                }
            }

            // 2. 施加伤害
            // 使用 Arrow 伤害源，但来源是剑气（投射物），归属于射击者
            DamageSource src = this.getDamageSources().arrow(this, shooter);
            livingTarget.damage(src, finalDamage);

            // 3. 施加黑暗纠缠效果（如果目标没有）
            if (darkEffectEntry != null && !livingTarget.hasStatusEffect(darkEffectEntry)) {
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
            }
        }
    }

    // =================================================================================
    // 其他方法
    // =================================================================================
    @Override
    public boolean isNoClip() {
        return true; // 始终穿墙
    }

    @Override
    protected void onBlockHit(net.minecraft.util.hit.BlockHitResult blockHitResult) {
        // 完全覆盖方块碰撞，什么都不做（剑气穿墙）
        // 不调用 super.onBlockHit()
    }

    // 覆盖 canHit，允许击中 LivingEntity (且非拥有者)
    @Override
    protected boolean canHit(Entity entity) {
        if (entity == this.getOwner()) {
            return false;
        }
        // 确保剑气能击中所有活着的实体
        return entity.isAlive() && (entity instanceof LivingEntity || super.canHit(entity));
    }

    public void setupBossCounter(LivingEntity boss, float damagePercent, int darkAmplifier) {
        this.isBossCounter = true;
        this.bossOwner = boss;
        this.counterDamagePercent = damagePercent;
        this.counterDarkAmplifier = darkAmplifier;
        // 注意：将 owner 设置为 boss 可能会影响 canHit 检查，如果 boss 是射击者，请谨慎设置
        this.setOwner(boss);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        // 返回剑气代表的物品
        return new ItemStack(ModItems.VOID_ESSENCE); // 或您 ModItems.VOID_ESSENCE
    }
}