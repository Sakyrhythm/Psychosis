package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.block.DarkPortalFrameBlock;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.entity.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class DarkGodEntity extends HostileEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("DarkGod");

    private static final float PHASE_TWO_THRESHOLD = 0.50F;
    private static final float EPSILON = 0.001F;

    // --- 动画常量 ---
    private static final int APPEARANCE_DURATION = 40;
    private static final double NORMAL_MOVEMENT_SPEED = 0.35;

    // --- 新常量：状态持续时间 ---
    private static final int PARTICLIZING_DURATION = 10;
    private static final int RECOVERY_DURATION = 40;

    private static final float SIDE_STEP_SPEED = 0.1F; // 左右移动的速度
    private static final int SIDE_STEP_DURATION = 20; // 左右移动方向持续的刻数
    private int sideStepTimer = 0; // 用于计时左右移动方向
    private int sideStepDirection = 1; // 1: 右移, -1: 左移
    private static final int HIT_PARTICLIZE_DURATION = 10; // 0.5 秒

    // --- 惩罚机制常量 ---
    private static final Identifier DARK_EFFECT_ID = Identifier.of("psychosis", "dark");
    private static final Identifier VULNERABLE_EFFECT_ID = Identifier.of("psychosis", "vulnerable");
    private static final int PUNISHMENT_RANGE = 20;
    private static final int MIN_DARK_LEVEL = 30;

    // --- 技能常量 (圆月斩) ---
    private static final int WINDUP_DURATION = 15;
    public static final int SWIPE_DURATION = 20;
    private static final int ATTACK_COOLDOWN = 40;
    private static final double DASH_SPEED_MULTIPLIER = 5.0;
    private static final int DASH_DURATION = 10;

    // 圆月斩常量 (替代半月斩常量)
    private static final float WHIRLWIND_AOE_RANGE = 5.0F;
    private BlockPos summoningBlockPos = null;
    private static final float PLAYER_HEALTH_PERCENT_WHIRLWIND_DAMAGE = 0.15F;
    private static final float FLAT_WHIRLWIND_DAMAGE = 5.0F;
    private static final int WHIRLWIND_DARK_INCREMENT_LEVEL = 5;

    // TrackedData 用于同步状态
    private static final TrackedData<Boolean> IS_APPEARING_TRACKED =
            DataTracker.registerData(DarkGodEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_PARTICLIZED_TRACKED =
            DataTracker.registerData(DarkGodEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public void setSummoningBlockPos(BlockPos pos) {
        this.summoningBlockPos = pos;
    }


    // 核心字段
    private enum BossState {
        APPEARING,
        PHASE_ONE
    }
    private BossState currentState = BossState.APPEARING;
    private final ServerBossBar bossBar;
    private int stateTimer = 0;
    public final RegistryEntry<StatusEffect> darkEffectEntry;
    public final RegistryEntry<StatusEffect> vulnerableEffectEntry;


    // *** 阶段和攻击状态字段 ***
    private boolean isPhaseTwo = false;
    private int attackCooldownTimer = 0;
    private int attackTimer = 0;
    private AttackState attackState = AttackState.IDLE;
    private PlayerEntity currentTarget = null;
    private Vec3d dashDirection = Vec3d.ZERO;
    private Set<Entity> hitEntities;


    // *** 攻击状态 ***
    public enum AttackState {
        IDLE,
        PARTICLIZING, // 变为粒子
        WINDUP,       // 技能前摇
        SWIPE,        // 圆月斩 (沿用名称，内部逻辑已变)
        DASHING,      // 冲刺
        RECOVERY,     // 攻击后强制实体化停留
        HIT_PARTICLIZE
    }

    // --- 动画状态字段 ---
    private int appearanceTicks = 0;


    // --- 构造函数 ---
    public DarkGodEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomName(Text.translatable("entity.psychosis.dark_god"));
        this.setCustomNameVisible(false);

        this.bossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        this.setPersistent();

        this.darkEffectEntry = world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                .getEntry(DARK_EFFECT_ID)
                .orElse(null);

        this.vulnerableEffectEntry = world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                .getEntry(VULNERABLE_EFFECT_ID)
                .orElse(null);

        if (!world.isClient) {
            this.setNoGravity(true);
            Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.0);
            this.hitEntities = new HashSet<>();
        }
    }
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);          // 先走原版死亡逻辑（掉落、经验、清空 BossBar 等）

        if (!this.getWorld().isClient) {
            // 1. 找到刷怪位置：优先用召唤方块，否则用自身位置
            BlockPos spawnPos = this.summoningBlockPos;

            // 2. 实例化 Goddess
            GoddessEntity goddess = new GoddessEntity(
                    ModEntities.GODDESS,   // ← 你的实体类型注册项
                    this.getWorld()
            );


            // 3. 设置位置 & 初始状态
            goddess.refreshPositionAndAngles(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY() + 1,
                    spawnPos.getZ() + 0.5,
                    this.random.nextFloat() * 360F,
                    0
            );
            goddess.setSummoningBlockPos(spawnPos);   // 把同一方块传给她，方便消失时关灯
            goddess.setCustomName(Text.translatable("entity.psychosis.goddess"));
            goddess.setCustomNameVisible(false);

            // 4. 立即播放入场动画（粒子爆发 + DASH_END）
            goddess.playIntro();        // 走你写好的入场逻辑

            // 5. 刷到世界
            this.getWorld().spawnEntity(goddess);

            System.out.println("[Server] DarkGod 死亡 → 召唤虚空女神于 " + spawnPos);
        }
    }


    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(IS_APPEARING_TRACKED, true);
        builder.add(IS_PARTICLIZED_TRACKED, false);
    }

    // --- 碰撞箱/渲染控制 ---
    protected boolean canStartTargeting() { return !this.isAppearing(); }
    public boolean collides() {
        return !this.isAppearing() && !this.isParticlized();
    }


    @Override
    public boolean isInvisible() {
        return super.isInvisible() || this.isParticlized() || this.isAppearing();
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        if (player.isSpectator()) { return false; }
        if (this.isParticlized() || this.isAppearing()) { return true; }
        return super.isInvisibleTo(player);
    }


    // --- 实体属性设置 ---
    public static DefaultAttributeContainer.Builder createDarkGodAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    // --- AI 任务初始化 ---
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BossAttackGoal(this));
        this.goalSelector.add(1, new BossMechanismGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    // --- 核心伤害处理 ---
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) {
            return false;
        }

        // 粒子化状态下免疫大部分外部伤害
        if (this.isParticlized() && !source.isOf(net.minecraft.entity.damage.DamageTypes.MAGIC)) {
            return false;
        }
        boolean isPlayerDamage = source.getAttacker() instanceof PlayerEntity;

        if (this.getAttackState() == AttackState.IDLE && amount > 0 && isPlayerDamage) {

            // 关键：存储攻击者作为潜在的IDLE目标（虽然这里不瞬移，但保持currentTarget惯例）
            this.currentTarget = (PlayerEntity) source.getAttacker();

            // 1. 设置状态和计时器
            this.attackState = AttackState.HIT_PARTICLIZE;
            this.attackTimer = HIT_PARTICLIZE_DURATION; // 10 刻

            // 2. 粒子化
            this.setParticlized(true);
            this.setVelocity(Vec3d.ZERO); // 停止任何移动

            // 3. Log
            System.out.println("[Server] 受击触发粒子化闪烁 (IDLE) -> HIT_PARTICLIZE (" + DarkGodEntity.HIT_PARTICLIZE_DURATION + " 刻)");
        }

        float healthBeforeDamage = this.getHealth();
        float maxHealth = this.getMaxHealth();
        float phaseTwoThreshold = maxHealth * PHASE_TWO_THRESHOLD;

        if (source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD) || amount >= maxHealth * 2) {
            return super.damage(source, amount);
        }

        boolean damaged = super.damage(source, amount);

        if (damaged) {
            float healthAfterDamage = this.getHealth();
            // 半血检查并进入二阶段
            if (!this.isPhaseTwo && healthAfterDamage <= phaseTwoThreshold + EPSILON && healthBeforeDamage > phaseTwoThreshold + EPSILON) {
                this.isPhaseTwo = true;
            }
        }

        return damaged;
    }

    // --- 主 Tick 逻辑 ---
    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            this.bossBar.setName(this.getDisplayName());

            boolean isAppearing = this.dataTracker.get(IS_APPEARING_TRACKED);

            // ⭐ 1. 检查玩家是否在追踪范围内 (立即消失逻辑)
            //    >> 仅在非出场状态下执行此检查
            if (!isAppearing) {
                PlayerEntity closestPlayer = this.getWorld().getClosestPlayer(this, this.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE));

                if (closestPlayer == null) {
                    this.disappear();
                    return; // 立即返回，阻止执行本 tick 后续所有逻辑
                }
            }


            if (isAppearing) {
                // 出场动画期间
                double targetY = this.getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                        (int)this.getX(), (int)this.getZ()) + 1.0;

                if (this.getY() < targetY - 0.1) {
                    this.addVelocity(0, 0.1, 0);
                    this.velocityModified = true;
                } else if (this.getY() > targetY + 0.1) {
                    this.addVelocity(0, -0.1, 0);
                    this.velocityModified = true;
                }

                this.appearanceTicks++;

                if (this.appearanceTicks >= APPEARANCE_DURATION) {
                    this.dataTracker.set(IS_APPEARING_TRACKED, false);
                    this.currentState = BossState.PHASE_ONE;

                    Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(NORMAL_MOVEMENT_SPEED);
                    this.setNoGravity(false); // 恢复重力
                }
            }

            // 后续逻辑 (只有在有玩家，或者仍在出场动画时才会执行到这里)

            if (this.attackCooldownTimer > 0) {
                this.attackCooldownTimer--;
            }
            if (!isAppearing() && this.getAttackState() == AttackState.IDLE && this.attackCooldownTimer > 0) {

                PlayerEntity closestPlayer = this.getWorld().getClosestPlayer(this, this.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE));

                if (closestPlayer != null) {
                    // 1. 面向玩家
                    this.lookAtEntity(closestPlayer, 10.0F, 10.0F);

                    // 2. 左右移动 (Side Step)
                    this.sideStepTimer--;

                    if (this.sideStepTimer <= 0) {
                        this.sideStepDirection = this.getWorld().random.nextBoolean() ? 1 : -1; // 随机决定左 (-1) 或右 (1)
                        this.sideStepTimer = SIDE_STEP_DURATION + this.getWorld().random.nextInt(10);
                    }

                    // 计算相对于 Boss 面向方向的垂直方向 (左右)
                    float yaw = this.getYaw();
                    double radians = (double) MathHelper.RADIANS_PER_DEGREE * yaw;

                    // 侧移向量 (z 轴的右方是 x 轴，因此是 sin)
                    double sideX = -MathHelper.cos((float) radians) * sideStepDirection * SIDE_STEP_SPEED;
                    double sideZ = -MathHelper.sin((float) radians) * sideStepDirection * SIDE_STEP_SPEED;

                    this.setVelocity(sideX, this.getVelocity().y, sideZ);
                    this.velocityModified = true;
                }
            }
            // 修正：确保在 IDLE 且冷却结束时，速度归零，否则会一直移动
            else if (this.getAttackState() == AttackState.IDLE && this.getVelocity().lengthSquared() > 1.0E-4) {
                this.setVelocity(Vec3d.ZERO);
            }

            if (this.attackState == AttackState.DASHING) {
                this.setVelocity(this.dashDirection.multiply(NORMAL_MOVEMENT_SPEED * DASH_SPEED_MULTIPLIER));
                this.performDashDamage();
            } else if (this.getAttackState() != AttackState.DASHING && this.getVelocity().lengthSquared() > 1.0E-4) {
                this.setVelocity(Vec3d.ZERO);
            }

            if (!isAppearing) {
                this.applyRangeAura();          // 范围光环惩罚 (Phase One & Two)

                if (this.getIsPhaseTwo()) {     // 二阶段半血检查
                    this.applyPhasePunishment();  // 二阶段永久惩罚
                }
            }
        }
    }

    // ⭐ 消失逻辑
    private void disappear() {
        if (this.getWorld().isClient()) return;
        if (this.summoningBlockPos != null) {
            World world = this.getWorld();
            BlockState blockState = world.getBlockState(this.summoningBlockPos);

            // 检查方块是否仍然是正确的 DARK_BLOCK 并且带有 EYE
            if (blockState.isOf(ModBlocks.DARK_BLOCK) && blockState.get(DarkPortalFrameBlock.EYE)) {

                // ⭐ 关键修正点: 确保您使用的 BlockState Property 是正确的。
                // 使用 with(...) 方法创建新的 BlockState
                BlockState newBlockState = blockState.with(DarkPortalFrameBlock.EYE, false);

                // 使用标记 3 来通知客户端和更新比较器 (如果方块仍在加载状态，这应该有效)
                boolean success = world.setBlockState(this.summoningBlockPos, newBlockState, 3);
                world.updateComparators(this.summoningBlockPos, ModBlocks.DARK_BLOCK);

                if (success) {
                    LOGGER.info("Removed Eye from summoning block at: {}", this.summoningBlockPos.toShortString());
                } else {
                    LOGGER.warn("Failed to remove Eye (setBlockState returned false) at: {}", this.summoningBlockPos.toShortString());
                }

            } else {
                LOGGER.warn("Summoning block was not found, modified, or EYE property was already false at: {}", this.summoningBlockPos.toShortString());
            }
        }
        // 停止 Boss Bar
        this.bossBar.clearPlayers();
        this.getWorld().playSound(
                null,
                this.getBlockPos(),
                net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                SoundCategory.HOSTILE,
                1.0F,
                0.5F
        );

        // 移除实体
        this.discard();
        LOGGER.info("Dark God disappeared due to no player in range (not APPEARING).");
    }

    // --- 机制 1: 范围性光环惩罚 ---
    public void applyRangeAura() {
        if (this.getWorld().isClient()) { return; }
        if (this.isParticlized()) { return; } // 粒子形态不施加光环

        Box effectRangeBox = this.getBoundingBox().expand(PUNISHMENT_RANGE);

        for (ServerPlayerEntity player : this.getWorld().getPlayers()
                .stream()
                .filter(p -> p instanceof ServerPlayerEntity)
                .map(p -> (ServerPlayerEntity) p)
                .toList()) {

            if (effectRangeBox.contains(player.getX(), player.getY(), player.getZ()) && player.isAlive()) {

                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DARKNESS,
                        200,
                        0,
                        true,
                        false,
                        true
                ));
            } else {
                if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
                    player.removeStatusEffect(StatusEffects.DARKNESS);
                }
            }
        }
    }

    // --- 机制 2: 阶段二永久惩罚 ---
    private void applyPhasePunishment() {
        // isPhaseTwo 检查已在 tick() 中完成

        if (this.getWorld().isClient() || this.darkEffectEntry == null || this.vulnerableEffectEntry == null) {
            return;
        }

        for (ServerPlayerEntity player : this.getWorld().getPlayers()
                .stream()
                .filter(p -> p instanceof ServerPlayerEntity)
                .map(p -> (ServerPlayerEntity) p)
                .toList()) {

            if (player.isAlive()) {
                // 1. 确保玩家拥有 MIN_DARK_LEVEL 的效果
                player.addStatusEffect(new StatusEffectInstance(
                        this.darkEffectEntry,
                        StatusEffectInstance.INFINITE,
                        MIN_DARK_LEVEL,
                        true,
                        true,
                        true
                ));

                // 2. 施加永久的 VULNERABLE 效果
                player.addStatusEffect(new StatusEffectInstance(
                        this.vulnerableEffectEntry, // 使用注册的 VULNERABLE 效果
                        StatusEffectInstance.INFINITE,
                        0, // 等级固定为 0
                        true,
                        true,
                        true
                ));
            }
        }
    }


    // --- BossBar/NBT/Getter/Setter ---
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) { super.onStartedTrackingBy(player); this.bossBar.addPlayer(player); }
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) { super.onStoppedTrackingBy(player); this.bossBar.removePlayer(player); }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("CurrentState", this.currentState.ordinal());
        nbt.putInt("StateTimer", this.stateTimer);
        nbt.putInt("AppearanceTicks", this.appearanceTicks);
        nbt.putBoolean("IsPhaseTwo", this.isPhaseTwo);
        nbt.putBoolean("IsParticlized", this.isParticlized());
        if (this.summoningBlockPos != null) {
            nbt.putLong("SummoningBlockPos", this.summoningBlockPos.asLong());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("SummoningBlockPos")) {
            this.summoningBlockPos = BlockPos.fromLong(nbt.getLong("SummoningBlockPos"));
        }
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("CurrentState")) {
            this.currentState = BossState.values()[nbt.getInt("CurrentState")];
        }
        this.stateTimer = nbt.getInt("StateTimer");
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }

        this.appearanceTicks = nbt.getInt("AppearanceTicks");
        this.isPhaseTwo = nbt.getBoolean("IsPhaseTwo");
        if (nbt.contains("IsParticlized")) {
            this.setParticlized(nbt.getBoolean("IsParticlized"));
        }

        if (this.appearanceTicks >= APPEARANCE_DURATION) {
            if (this.dataTracker.get(IS_APPEARING_TRACKED)) {
                this.dataTracker.set(IS_APPEARING_TRACKED, false);
                this.currentState = BossState.PHASE_ONE;
            }
        }

        if (!this.isAppearing() && Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).getBaseValue() < NORMAL_MOVEMENT_SPEED) {
            Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(NORMAL_MOVEMENT_SPEED);
        }
    }

    // --- 辅助 Getter/Setter ---
    public boolean isAppearing() {
        if (this.dataTracker == null) return true;
        return this.dataTracker.get(IS_APPEARING_TRACKED);
    }
    public boolean isParticlized() {
        if (this.dataTracker == null) return false;
        return this.dataTracker.get(IS_PARTICLIZED_TRACKED);
    }
    public void setParticlized(boolean particlized) {
        if (!this.getWorld().isClient()) {
            this.dataTracker.set(IS_PARTICLIZED_TRACKED, particlized);
        }
    }
    public float getAppearanceProgress(float tickDelta) {
        return Math.min(1.0f, (this.appearanceTicks + tickDelta) / (float)APPEARANCE_DURATION);
    }
    public boolean getIsPhaseTwo() { return this.isPhaseTwo; }
    public int getAttackCooldownTimer() { return this.attackCooldownTimer; }
    public AttackState getAttackState() { return this.attackState; }
    public int getAttackTimer() { return this.attackTimer; }

    public Box getAttackBox() {
        return this.getBoundingBox().expand(5.0);
    }

    // --- 冲刺伤害判定 ---
    private void performDashDamage() {
        if (this.getWorld().isClient()) return;

        // 【新常量】冲刺伤害和效果
        final float PLAYER_HEALTH_PERCENT_DASH_DAMAGE = 0.10F; // 10%
        final float FLAT_DASH_DAMAGE = 3.0F;
        final int DASH_DARK_INCREMENT_LEVEL = 5; // 假设冲刺也增加 5 级黑暗效果

        Box dashBox = this.getBoundingBox().expand(0.8, 0.8, 0.8);

        List<Entity> entities = this.getWorld().getOtherEntities(this, dashBox,
                entity -> entity.isAlive() && !(entity instanceof DarkGodEntity));

        for (Entity entity : entities) {
            if (this.hitEntities.contains(entity)) {
                continue;
            }

            this.hitEntities.add(entity);

            float damage;

            if (entity instanceof PlayerEntity player) {
                // 【伤害计算】：10% 玩家最大生命值 + 3.0
                damage = player.getMaxHealth() * PLAYER_HEALTH_PERCENT_DASH_DAMAGE + FLAT_DASH_DAMAGE;
            } else {
                // 非玩家实体：使用 Boss 自身的攻击伤害作为基础
                damage = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            }

            // 【确保伤害源正确】
            if (entity.damage(this.getDamageSources().mobAttack(this), damage)) {

                // 【施加黑暗效果】(仅对玩家，且增加等级)
                if (entity instanceof PlayerEntity player && this.darkEffectEntry != null) {
                    StatusEffectInstance currentDark = player.getStatusEffect(this.darkEffectEntry);

                    int currentAmplifier = currentDark != null ? currentDark.getAmplifier() : -1;
                    int newAmplifier = currentAmplifier + DASH_DARK_INCREMENT_LEVEL;

                    player.addStatusEffect(new StatusEffectInstance(
                            this.darkEffectEntry,
                            StatusEffectInstance.INFINITE, // 永久持续
                            newAmplifier,
                            true, true, true
                    ));
                }


                // 【击退逻辑】
                Vec3d knockbackDir = this.dashDirection.normalize();
                entity.addVelocity(knockbackDir.x * 1.5, 0.4, knockbackDir.z * 1.5);
                entity.velocityModified = true;
            }

        }
    }


    // --- 嵌套类：Boss 机制光环/阶段管理 ---
    private static class BossMechanismGoal extends Goal {

        private final DarkGodEntity boss;

        public BossMechanismGoal(DarkGodEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Control.LOOK));
        }

        @Override public boolean canStart() { return this.boss.isAlive() && !this.boss.isAppearing() && !this.boss.getWorld().isClient(); }
        @Override public boolean shouldContinue() { return this.boss.isAlive() && !this.boss.isAppearing(); }

        @Override
        public void tick() {
            // 核心修正：机制逻辑已移到 DarkGodEntity.tick() 中执行
        }
    }


    // --- 嵌套类：核心攻击 AI 任务 ---
    private static class BossAttackGoal extends Goal {

        private final DarkGodEntity boss;

        public BossAttackGoal(DarkGodEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override public boolean canStart() {
            return this.boss.isAlive() && !this.boss.isAppearing() &&
                    this.boss.getAttackState() == AttackState.IDLE &&
                    this.boss.getAttackCooldownTimer() <= 0;
        }

        @Override public boolean shouldContinue() {
            return this.boss.getAttackState() != AttackState.IDLE ||
                    this.boss.getAttackState() == AttackState.RECOVERY;
        }

        @Override
        public void stop() {
            this.boss.setVelocity(Vec3d.ZERO);
            this.boss.attackState = AttackState.IDLE;
            this.boss.currentTarget = null;
            this.boss.navigation.stop();
            Objects.requireNonNull(this.boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(NORMAL_MOVEMENT_SPEED);

            if (this.boss.hitEntities != null) {
                this.boss.hitEntities.clear();
            }

            // 确保在 IDLE (冷却期) 状态下强制为实体形态
            this.boss.setParticlized(false);
        }

        @Override
        public void start() {
            this.boss.attackCooldownTimer = ATTACK_COOLDOWN;
            // 确保 BossAttackGoal 启动时能找到目标
            this.boss.currentTarget = this.boss.getWorld().getClosestPlayer(this.boss, 40.0);

            if (this.boss.currentTarget != null) {
                this.boss.attackState = AttackState.PARTICLIZING;
                this.boss.attackTimer = PARTICLIZING_DURATION;
            } else {
                this.boss.attackState = AttackState.IDLE;
                this.boss.attackCooldownTimer = 0;
            }
        }

        @Override
        public void tick() {
            if (this.boss.currentTarget == null || !this.boss.currentTarget.isAlive()) {
                this.stop();
                return;
            }

            this.boss.lookAtEntity(this.boss.currentTarget, 30.0F, 30.0F);

            switch (this.boss.attackState) {
                case PARTICLIZING:
                    handleParticlizing();
                    break;
                case WINDUP:
                    handleWindup();
                    break;
                case SWIPE:
                    handleWhirlwind(); // 执行圆月斩逻辑
                    break;
                case DASHING:
                    handleDashing();
                    break;
                case RECOVERY:
                    handleRecovery();
                    break;
                case HIT_PARTICLIZE: // ⭐ 确保这里调用了 handleHitParticlize()
                    handleHitParticlize();
                    break;
                case IDLE:
                default:
                    break;
            }
        }

        // --- 状态处理：粒子化过渡 ---
        private void handleParticlizing() {
            if (this.boss.attackTimer == PARTICLIZING_DURATION) {
                this.boss.setParticlized(true);
            }
            this.boss.attackTimer--;
            if (this.boss.attackTimer <= 0) {
                this.boss.attackState = AttackState.WINDUP;
                this.boss.attackTimer = WINDUP_DURATION;
            }
        }

        // --- 状态处理：蓄力前摇 ---
        private void handleWindup() {
            this.boss.attackTimer--;

            if (this.boss.attackTimer <= 0) {
                double distance = this.boss.distanceTo(this.boss.currentTarget);

                net.minecraft.util.math.Box currentBox = this.boss.getBoundingBox();
                float entityWidth = (float)(currentBox.maxX - currentBox.minX);

                if (distance > 3.0 + entityWidth * 0.5) {
                    this.boss.attackState = DarkGodEntity.AttackState.DASHING;
                    this.boss.attackTimer = DASH_DURATION;

                    Vec3d targetPos = this.boss.currentTarget.getPos();
                    Vec3d bossPos = this.boss.getPos();
                    Vec3d dir = targetPos.subtract(bossPos);

                    this.boss.dashDirection = dir.normalize();

                    if (this.boss.hitEntities != null) {
                        this.boss.hitEntities.clear();
                    }
                } else {
                    this.boss.attackState = DarkGodEntity.AttackState.SWIPE;
                    this.boss.attackTimer = SWIPE_DURATION;
                }

                this.boss.navigation.stop();
            }
        }

        // --- 状态处理：圆月斩 (Whirlwind) ---
        private void handleWhirlwind() {
            this.boss.attackTimer--;

            int remainingTicks = SWIPE_DURATION - this.boss.attackTimer;

            // 5 刻时实体化（视觉效果）
            if (remainingTicks == 5) {
                this.boss.setParticlized(false);
            }

            // 伤害判定时间点 (例如在动画中点)
            if (remainingTicks == SWIPE_DURATION / 2) {
                // 执行圆月斩 AOE 逻辑
                performWhirlwindAttack();
            }

            if (this.boss.attackTimer <= 0) {
                this.boss.attackState = AttackState.RECOVERY;
                this.boss.attackTimer = RECOVERY_DURATION;
                this.boss.setParticlized(false);
            }
        }

        // *** 核心：圆月斩 AOE 逻辑 (360度，5格半径圆形) ***
        private void performWhirlwindAttack() {
            if (this.boss.getWorld().isClient()) return;

            World targetWorld = this.boss.getWorld();
            double x = this.boss.getX();
            double y = this.boss.getY();
            double z = this.boss.getZ();

            // --- 使用类常量 ---
            // 15%

            // 2. 定义 AOE 区域 (以 Boss 为中心)
            Box aoeBox = new Box(
                    x - WHIRLWIND_AOE_RANGE, y - 1.0, z - WHIRLWIND_AOE_RANGE,
                    x + WHIRLWIND_AOE_RANGE, y + 2.0, z + WHIRLWIND_AOE_RANGE
            );

            // 3. 遍历 AOE 范围内的实体
            targetWorld.getOtherEntities(this.boss, aoeBox, entity -> entity instanceof LivingEntity)
                    .forEach(entity -> {
                        if (entity != this.boss) {
                            LivingEntity targetEntity = (LivingEntity) entity;

                            float totalDamage;

                            // --- 伤害计算 (针对玩家的百分比伤害) ---
                            if (targetEntity instanceof PlayerEntity player) {
                                // 伤害 = 玩家最大生命值 * 15% + 5.0
                                totalDamage = player.getMaxHealth() * DarkGodEntity.PLAYER_HEALTH_PERCENT_WHIRLWIND_DAMAGE + DarkGodEntity.FLAT_WHIRLWIND_DAMAGE;
                            } else {
                                // 非玩家实体，使用 Boss 的基础攻击伤害 (保留原有逻辑的简洁性)
                                totalDamage = (float) this.boss.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                            }

                            // 4. 造成伤害
                            DamageSources damageSources = targetWorld.getDamageSources();
                            targetEntity.damage(damageSources.mobAttack(this.boss), totalDamage);

                            // --- 施加黑暗效果 (仅对玩家，且增加等级) ---
                            if (targetEntity instanceof PlayerEntity player && this.boss.darkEffectEntry != null) {
                                StatusEffectInstance currentDark = player.getStatusEffect(this.boss.darkEffectEntry);

                                // 计算新等级：当前等级 + 5
                                int currentAmplifier = currentDark != null ? currentDark.getAmplifier() : -1;
                                int newAmplifier = currentAmplifier + DarkGodEntity.WHIRLWIND_DARK_INCREMENT_LEVEL;

                                player.addStatusEffect(new StatusEffectInstance(
                                        this.boss.darkEffectEntry,
                                        StatusEffectInstance.INFINITE, // 永久持续
                                        newAmplifier,
                                        true, true, true
                                ));
                            }

                        }
                    });

            // 4. AOE 成功声音
            targetWorld.playSound(null, x, y, z,
                    net.minecraft.sound.SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                    net.minecraft.sound.SoundCategory.HOSTILE, 1.0F, 0.6F + targetWorld.random.nextFloat() * 0.4F);

            WhirlwindSlashEntity slash = new WhirlwindSlashEntity(targetWorld, this.boss);
            final double Y_OFFSET = 1.1f;
            slash.setScale(2.0F);
            slash.setPos(x, y + Y_OFFSET, z);
            targetWorld.spawnEntity(slash);

        }

        // --- 状态处理：冲刺 ---
        private void handleDashing() {
            this.boss.attackTimer--;

            if (this.boss.attackTimer > 0) {
                double distanceToTarget = this.boss.distanceTo(this.boss.currentTarget);
                if (distanceToTarget < 1.5) {
                    this.boss.attackTimer = 0;
                }
            }

            if (this.boss.attackTimer <= 0) {
                this.boss.setVelocity(Vec3d.ZERO);

                this.boss.attackState = AttackState.RECOVERY;
                this.boss.attackTimer = RECOVERY_DURATION;
                this.boss.setParticlized(false);
            }
        }

        // --- 状态处理：实体化恢复 ---
        private void handleRecovery() {
            this.boss.attackTimer--;

            if (this.boss.isParticlized()) {
                this.boss.setParticlized(false);
            }

            this.boss.setVelocity(Vec3d.ZERO);

            if (this.boss.attackTimer <= 0) {
                this.stop();
            }
        }
        private void handleHitParticlize() {
            this.boss.attackTimer--;

            // 确保在闪烁期间一直是粒子化
            if (!this.boss.isParticlized()) {
                this.boss.setParticlized(true);
            }

            // 强制速度归零，保持原位
            this.boss.setVelocity(Vec3d.ZERO);

            // ⭐ 粒子化结束点
            if (this.boss.attackTimer <= 0) {
                // 解除粒子化
                this.boss.setParticlized(false);

                // 回到 IDLE
                this.boss.attackState = AttackState.IDLE;

                // 重新启动冷却计时器，以保证 Boss 不会立即攻击
                if (this.boss.attackCooldownTimer <= 0) {
                    this.boss.attackCooldownTimer = DarkGodEntity.ATTACK_COOLDOWN;
                }

                // ⭐ 日志输出：回到 IDLE
                System.out.println("[Server] 受击粒子化闪烁结束: HIT_PARTICLIZE -> IDLE (保持原位)");
            }
        }
    }
}