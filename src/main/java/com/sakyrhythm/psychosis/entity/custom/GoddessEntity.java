package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.block.DarkPortalFrameBlock;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.networking.ModNetworking;
import com.sakyrhythm.psychosis.world.DarkBlockTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GoddessEntity extends HostileEntity {

    /* ============================ 动画 ============================ */
    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState ATTACK_ANIMATION = new AnimationState();
    public final AnimationState DASH_END = new AnimationState();

    /* ============================ 基本常量 ============================ */
    private static final float PHASE_TWO_THRESHOLD = 0.50F;
    private static final float EPSILON = 0.001F;
    // --- 护盾常量 ---
    private static final float SHIELD_HEALTH = 300.0F;
    private float currentShield = SHIELD_HEALTH;
    private int beamDelayTimer = -1;
    // --- 状态持续时间 ---
    private static final int APPEARANCE_DURATION = 40;
    private static final double NORMAL_MOVEMENT_SPEED = 0.35;
    private static final int RECOVERY_DURATION = 20;
    private static final int FADE_OUT_DURATION = 10;

    // --- Hexagram 计时器 ---
    private static final int HEXAGRAM_CYCLE_DURATION = 60; // 60 ticks = 3 seconds
    private int hexagramTimer = HEXAGRAM_CYCLE_DURATION;

    // --- 惩罚机制常量 ---
    private static final Identifier DARK_EFFECT_ID = Identifier.of("psychosis", "dark");
    private static final Identifier VULNERABLE_EFFECT_ID = Identifier.of("psychosis", "vulnerable");
    private static final Identifier FRENZY_EFFECT_ID = Identifier.of("psychosis", "frenzy");
    private static final int PUNISHMENT_RANGE = 20;
    // ⭐ 黑暗等级常量 (Amplifier)
    private static final int PRE_SHIELD_DARK_LEVEL = 30; // 护盾存在时：放大器 30
    private static final int POST_SHIELD_DARK_LEVEL = 70; // 护盾破裂时：放大器 70
    // --- 技能常量 (圆月斩) ---
    private static final int WINDUP_DURATION = 7;
    public static final int SWIPE_DURATION = 10;
    private static final int ATTACK_COOLDOWN = 10;

    // --- 圆月斩常量 ---
    private static final float WHIRLWIND_AOE_RANGE = 6.0F;
    private static final float WHIRLWIND_TRIGGER_DISTANCE = 5.0F;
    public static final float PLAYER_HEALTH_PERCENT_WHIRLWIND_DAMAGE = 0.30F;
    public static final float FLAT_WHIRLWIND_DAMAGE = 10.0F;
    public static final int WHIRLWIND_DARK_INCREMENT_LEVEL = 2; // 圆月斩每次命中叠加等级
    private static final double TELEPORT_BEHIND_DISTANCE = 2.0;
    private static final int WHIRLWIND_SLASH_DELAY = 12;

    // --- 受伤粒子化/连闪常量 (新增) ---
    private static final int DAMAGE_PARTICLE_DURATION = 4;
    private static final double DAMAGE_RETREAT_DISTANCE = 3.0; // 后撤3格
    private int rapidFlashLeft = 0; // 剩余连闪次数

    // --- 反击常量 ---
    private static final int COUNTER_ATTACK_RANGE = 20; // 反击锁敌范围
    private static final double TELEPORT_TO_BLOCK_DISTANCE = 6.0; // 传送回 BlockPos 的距离限制

    // ⭐ 召唤方块坐标 (用于粒子和闪避目标)
    public BlockPos summoningBlockPos = null;

    // TrackedData 用于同步状态
    private static final TrackedData<Boolean> IS_APPEARING_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_PARTICLIZED_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> HEXAGRAM_TYPE_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DAMAGE_PARTICLE_TIMER_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // 🔥 新增 Hexagram 计时器 TrackedData
    private static final TrackedData<Integer> HEXAGRAM_TIMER_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final TrackedData<Integer> ATTACK_STATE_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ATTACK_TIMER_TRACKED =
            DataTracker.registerData(GoddessEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public void setSummoningBlockPos(BlockPos pos) {
        this.summoningBlockPos = pos;
    }

    private static final RegistryKey<net.minecraft.entity.damage.DamageType> DARK_DAMAGE_KEY =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(Psychosis.MOD_ID, "dark"));
    private static final RegistryKey<net.minecraft.entity.damage.DamageType> SHADOW_DAMAGE_KEY =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(Psychosis.MOD_ID, "shadow"));

    // 核心字段
    private enum BossState {
        APPEARING,
        PHASE_ONE
    }

    private BossState currentState = BossState.APPEARING;

    // 🔥 双 BossBar 实例
    private final ServerBossBar shieldBossBar; // 护盾条 (白色)
    private final ServerBossBar healthBossBar; // 血量条 (红色)

    public final RegistryEntry<net.minecraft.entity.effect.StatusEffect> darkEffectEntry;
    public final RegistryEntry<net.minecraft.entity.effect.StatusEffect> vulnerableEffectEntry;
    public final RegistryEntry<net.minecraft.entity.effect.StatusEffect> frenzyEffectEntry;
    // 阶段和攻击状态字段
    private boolean isPhaseTwo = false;
    private int attackCooldownTimer = 0;
    private int attackTimer = 0;
    private AttackState attackState = AttackState.IDLE;
    @Nullable
    private PlayerEntity currentTarget = null;
    private Set<Entity> hitEntities;
    private int appearanceTicks = 0;

    // 攻击状态
    public enum AttackState {
        IDLE,
        WINDUP,           // 技能前摇
        FADE_OUT,         // 攻击瞬移 - 粒子化隐身
        ATTACK_TELEPORT,  // 攻击瞬移 - 实际瞬移和现身
        SWIPE,            // 圆月斩
        RECOVERY,         // 攻击后强制实体化停留
        IN,               // 入场动画
        OUT,              // 永久粒子化
        COUNTER_ATTACK    // 🔥 新增：反击动作状态 (三段斩)
    }

    // --- 八芒星类型 Enum ---
    public enum HexagramType {
        NONE,       // 0: 不显示 (默认)
        WHITE,      // 1: 正常 (Normal)
        PURPLE,     // 2: 魔法 (Magic)
        BLACK,      // 3: 黑暗 (Dark)
        GRAY        // 4: 阴影 (Shadow)
    }

    // --- 构造函数 ---
    public GoddessEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomName(Text.translatable("entity.psychosis.goddess"));
        this.setCustomNameVisible(false);
        // 护盾 = 白色，带名字
        this.shieldBossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.WHITE, BossBar.Style.PROGRESS);
        // 血量 = 红色，名字为空
        this.healthBossBar = new ServerBossBar(net.minecraft.text.Text.empty(), BossBar.Color.RED, BossBar.Style.PROGRESS);

        this.setPersistent();

        // 获取状态效果注册项
        this.darkEffectEntry = world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                .getEntry(DARK_EFFECT_ID)
                .orElse(null);

        this.vulnerableEffectEntry = world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                .getEntry(VULNERABLE_EFFECT_ID)
                .orElse(null);

        this.frenzyEffectEntry = world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                .getEntry(FRENZY_EFFECT_ID)
                .orElse(null);


        if (!world.isClient) {
            this.setNoGravity(true);
            Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.0);
            this.hitEntities = new HashSet<>();
        }
    }

    @Override
    public boolean isCustomNameVisible() {
        return super.isCustomNameVisible() && !Objects.equals(this.getCustomName(), Text.translatable("entity.psychosis.goddess"));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(IS_APPEARING_TRACKED, true);
        builder.add(IS_PARTICLIZED_TRACKED, false);
        builder.add(DAMAGE_PARTICLE_TIMER_TRACKED, 0);
        builder.add(ATTACK_STATE_TRACKED, AttackState.IDLE.ordinal());
        builder.add(ATTACK_TIMER_TRACKED, 0);
        builder.add(HEXAGRAM_TYPE_TRACKED, HexagramType.NONE.ordinal());
        builder.add(HEXAGRAM_TIMER_TRACKED, HEXAGRAM_CYCLE_DURATION); // 🔥 新增：Hexagram 计时器
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

    @Override
    protected void initGoals() {
    }
    public static DefaultAttributeContainer.Builder createGoddessBossAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 900.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    // --- 八芒星 Getter/Setter ---
    public HexagramType getHexagramType() {
        if (this.dataTracker == null) return HexagramType.NONE;
        return HexagramType.values()[this.dataTracker.get(HEXAGRAM_TYPE_TRACKED)];
    }
    @Override
    public boolean isCollidable() {
        // 允许穿过 Boss
        return false;
    }

    @Override
    public boolean isPushable() {
        // 防止被挤走
        return false;
    }

    @Override
    public boolean collidesWith(Entity other) {
        // 不产生物理碰撞计算
        return false;
    }
    @Override
    public void pushAway(Entity entity) {
        // ⭐ 核心：当有实体（玩家或生物）触碰到 Boss 碰撞箱时触发
        if (this.getWorld().isClient) return;

        // 如果正在出场、已经粒子化、或者正在处理连闪，则不重复触发
        if (this.isAppearing() || this.isParticlized() || this.rapidFlashLeft > 0) return;

        // 只在 IDLE (空闲) 或 RECOVERY (收招) 状态下触发闪避，防止中断攻击技能
        if (this.attackState == AttackState.IDLE || this.attackState == AttackState.RECOVERY) {
            // 设置连闪次数 (例如连闪 3 次)
            this.rapidFlashLeft = 3;
            // 立即执行第一次闪避
            this.triggerDamageParticle();

            System.out.println("DEBUG (Server): Boss touched by " + entity.getName().getString() + "! Triggering Rapid Flash.");
        }
    }

    public void setHexagramType(HexagramType type) {
        if (this.getWorld().isClient()) return;
        this.dataTracker.set(HEXAGRAM_TYPE_TRACKED, type.ordinal());

        // 仅在非 NONE 时发送一次状态码，用于一次性特效，持续渲染交给客户端 Mixin
        if (type != HexagramType.NONE) {
            this.getWorld().sendEntityStatus(this, (byte)90);
        }
    }

    // --- 随机选择 HexagramType ---
    private HexagramType getRandomHexagramType() {
        // 范围 [1, 4] 对应 WHITE, PURPLE, BLACK, GRAY
        int randomOrdinal = this.random.nextInt(HexagramType.values().length - 1) + 1;
        return HexagramType.values()[randomOrdinal];
    }

    // --- 核心伤害处理 ---
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) {
            return false;
        }

        // 检查是否需要刷新八芒星状态 (使用 CooldownTimer)
        if (this.isParticlized() && !isDamageSourceMagic(source)) {
            return false;
        }

        if (source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD) || amount >= this.getMaxHealth() * 2) {
            return super.damage(source, amount);
        }
        HexagramType requiredType = this.getHexagramType();

        // 🔥 修复点：反击触发条件不再检查攻击者是否为 PlayerEntity
        // 只检查 Hexagram 不为 NONE 且 Boss 处于 IDLE/WINDUP 状态
        // Note: Boss 只有在 IDLE/WINDUP 才会进入反击窗口，但这里不检查 state，由后续逻辑控制。
        if (requiredType != HexagramType.NONE) {
            // 1. 确定实际伤害类型
            boolean isMagic = isDamageSourceMagic(source);
            boolean isDark = isDamageSourceDark(source);
            boolean isShadow = isDamageSourceShadow(source);
            // 假设不是上述三者之一，就是 Normal
            boolean isNormal = !isMagic && !isDark && !isShadow;

            // 2. 检查伤害是否触发反击
            boolean triggerCounter = false;
            switch (requiredType) {
                case WHITE -> triggerCounter = isNormal;
                case PURPLE -> triggerCounter = isMagic;
                case BLACK -> triggerCounter = isDark;
                case GRAY -> triggerCounter = isShadow;
                default -> {} // NONE 状态在 if 条件中已排除
            }

            if (triggerCounter) {
                // 🔥 成功反击 - 启动多目标反击动作，并立即随机下一个 Hexagram
                // Note: 这里是反击成功的地方，不需要检查 attackState，直接执行即可
                this.triggerTripleSlash();

                // 成功应对后，立即随机切换到下一个 HexagramType
                HexagramType newType = this.getRandomHexagramType();
                while (newType == requiredType) {
                    newType = this.getRandomHexagramType();
                }
                this.setHexagramType(newType);
                this.hexagramTimer = HEXAGRAM_CYCLE_DURATION; // 重置计时器

                System.out.println("DEBUG (Server): --- COUNTER SUCCESSFUL (Universal Trigger) ---. Next Hexagram: " + newType.name());
                return false; // 吞噬伤害
            }
            else {
                // 反击失败 - Hexagram 保持不变，Boss 受到伤害
                System.out.println("DEBUG (Server): Counter Failed. Wrong Damage Type (Universal Trigger). Hexagram remains " + requiredType.name());
            }
        }

        // 护盾逻辑
        if (this.currentShield > 0) {
            float damageToShield = Math.min(amount, this.currentShield);
            this.currentShield -= damageToShield;
            float remainingDamage = amount - damageToShield;

            if (this.currentShield <= 0) {
                this.currentShield = 0;

                // 破盾瞬间设置 HexagramType，启动永久随机机制
                if (this.getHexagramType() == HexagramType.NONE) {
                    this.setHexagramType(this.getRandomHexagramType());
                }

                this.applyPhasePunishment();
                if (!this.getWorld().isClient()) {
                    this.updateBossBars();
                }
            }
            if (remainingDamage <= 0) {
                return true;
            }
            amount = remainingDamage;
        }

        float healthBeforeDamage = this.getHealth();
        boolean damaged = super.damage(source, amount);

        // 受伤粒子化逻辑
        if (damaged && source.getAttacker() instanceof LivingEntity &&
                !source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD)) {

            if (this.attackState == AttackState.IDLE || this.attackState == AttackState.RECOVERY) {
                // 不再传递攻击者，因为闪避是基于 BlockPos 的
                triggerDamageParticle();
            }
        }

        // 阶段二切换逻辑
        if (damaged) {
            float healthAfterDamage = this.getHealth();
            float maxHealth = this.getMaxHealth();
            float phaseTwoThreshold = maxHealth * PHASE_TWO_THRESHOLD;

            if (!this.isPhaseTwo && healthAfterDamage <= phaseTwoThreshold + EPSILON &&
                    healthBeforeDamage > phaseTwoThreshold + EPSILON) {
                this.isPhaseTwo = true;
            }

            // 确保在血量归零时调用 disappear()
            if (healthAfterDamage <= 0.001F) {
                this.disappear();
                return true;
            }
        }

        return damaged;
    }

    /* --------------------------------------------------------------------- */
    /* 伤害类型判断辅助方法                                                    */
    /* --------------------------------------------------------------------- */
    private boolean isDamageSourceDark(DamageSource src) {
        if (this.getWorld().isClient()) return false;
        return this.getWorld().getRegistryManager() // 使用 Boss 自身的世界
                .get(RegistryKeys.DAMAGE_TYPE)
                .getEntry(src.getType())
                .matchesKey(DARK_DAMAGE_KEY);
    }

    private boolean isDamageSourceShadow(DamageSource src) {
        if (this.getWorld().isClient()) return false;
        return this.getWorld().getRegistryManager() // 使用 Boss 自身的世界
                .get(RegistryKeys.DAMAGE_TYPE)
                .getEntry(src.getType())
                .matchesKey(SHADOW_DAMAGE_KEY);
    }
    private static boolean isDamageSourceMagic(DamageSource src) {
        return src.isOf(net.minecraft.entity.damage.DamageTypes.MAGIC);
    }

    /* --------------------------------------------------------------------- */
    /* 攻击和状态机方法                                                        */
    /* --------------------------------------------------------------------- */
    private void triggerTripleSlash() {
        // 1. 基本检查和常量定义
        if (this.getWorld().isClient() || !(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // --- 机制常量 ---
        final int BEAM_DELAY_TICKS = 14; // 使用上个版本确定的延迟

        // 2. 获取目标玩家和发射点
        Box searchBox = this.getBoundingBox().expand(COUNTER_ATTACK_RANGE);
        serverWorld.getPlayers(player ->
                player.isAlive() && searchBox.contains(player.getPos())
        );

        BlockPos blockLocation = this.getSummoningAnchorPos();
        if (blockLocation == null) {
            System.out.println("DEBUG (Server): Summoning Block position is null. Cannot launch slash.");
            return;
        }

        // 3. 设置 Boss 状态，播放动画和音效

        // 💥 播放 DASH_END 动画 (三段斩准备动作)
        this.attackState = AttackState.COUNTER_ATTACK;
        this.attackTimer = BEAM_DELAY_TICKS; // 计时器用于延迟执行伤害

        // 假设 stopAttackLogic() 已经执行了导航停止和速度清零
        this.getWorld().playSound(
                null,
                this.getBlockPos(),
                SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.HOSTILE,
                5.0F,
                0.5F
        );

        // 4. 启动延迟计时器 (取代您原有的同步循环和伤害逻辑)
        // 伤害和光束发射将在 tick() 方法中，在计时器归零时执行 executeBeamAndDamageLogic()
        this.beamDelayTimer = BEAM_DELAY_TICKS;

        // 5. 将光束发射和伤害惩罚逻辑移交给延迟执行方法 (executeBeamAndDamageLogic)

        System.out.println("DEBUG (Server): Counter triggered. Starting " + BEAM_DELAY_TICKS + " tick beam windup.");
    }

    // --- 受伤粒子化后撤 (闪避) ---
    private void triggerDamageParticle() {
        if (this.getWorld().isClient()) return;

        this.setParticlized(true);

        if (this.getSummoningAnchorPos() == null) {
            // 如果没有 BlockPos，退回到自身位置附近随机闪避
            double offX = (this.random.nextBoolean() ? 1 : -1) * DAMAGE_RETREAT_DISTANCE;
            double offZ = (this.random.nextBoolean() ? 1 : -1) * DAMAGE_RETREAT_DISTANCE;
            double offY = (this.random.nextBoolean() ? 1 : -1) * this.random.nextDouble();
            this.teleport(this.getX() + offX, this.getY() + offY, this.getZ() + offZ, true);
        } else {
            // ⭐ 修正闪避逻辑：以 summoningBlockPos 为中心计算闪避目标
            Vec3d targetCenter = Vec3d.ofBottomCenter(this.getSummoningAnchorPos());

            // 检查 Boss 是否距离 BlockPos 过远
            if (this.getPos().distanceTo(targetCenter) > TELEPORT_TO_BLOCK_DISTANCE) {
                // 如果太远，直接传送回 BlockPos 上方
                this.teleport(targetCenter.getX(), targetCenter.getY() + 1.0, targetCenter.getZ(), true);
            } else {
                // 如果在范围内，进行基于 BlockPos 的随机闪避
                int dirDice = this.random.nextInt(4);
                double offX = 0, offZ = 0;

                // 基于 BlockPos 坐标，随机向四周闪避 DAMAGE_RETREAT_DISTANCE
                switch (dirDice) {
                    case 0 -> offZ = -DAMAGE_RETREAT_DISTANCE;
                    case 1 -> offZ = DAMAGE_RETREAT_DISTANCE;
                    case 2 -> offX = -DAMAGE_RETREAT_DISTANCE;
                    case 3 -> offX = DAMAGE_RETREAT_DISTANCE;
                }

                double tx = targetCenter.getX() + offX;
                double tz = targetCenter.getZ() + offZ;
                double ty = targetCenter.getY() + 1.0; // 保持在 BlockPos 上方高度

                this.teleport(tx, ty, tz, true);
            }
        }

        this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.2f, 1);

        this.dataTracker.set(DAMAGE_PARTICLE_TIMER_TRACKED, DAMAGE_PARTICLE_DURATION);
        this.attackState = AttackState.IDLE;
        this.attackTimer = 0;
    }

    private void finishDamageParticle() {
        if (this.getWorld().isClient()) return;
        this.setParticlized(false);
        this.getWorld().sendEntityStatus(this, (byte) 82);
        this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE,
                6.0F, 0.8F + this.getWorld().random.nextFloat() * 0.2F);
    }

    private void stopAttackLogic() {
        this.setVelocity(Vec3d.ZERO);
        this.attackState = AttackState.IDLE;
        this.currentTarget = null;
        this.navigation.stop();
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(NORMAL_MOVEMENT_SPEED);
        if (this.hitEntities != null) this.hitEntities.clear();
        this.dataTracker.set(IS_PARTICLIZED_TRACKED, false);
    }

    private void startAttackLogic() {
        this.attackCooldownTimer = ATTACK_COOLDOWN;
        this.currentTarget = this.getWorld().getClosestPlayer(this, 40.0);
        if (this.currentTarget != null) {
            this.attackState = AttackState.WINDUP;
            this.attackTimer = WINDUP_DURATION;
            this.dataTracker.set(IS_PARTICLIZED_TRACKED, false);
        } else {
            this.attackState = AttackState.IDLE;
            this.attackCooldownTimer = 0;
        }
    }
    public BlockPos getSummoningAnchorPos() {
        System.out.print(summoningBlockPos);
        if (this.summoningBlockPos == null) {
            // 如果是 null，尝试查找最近的 Dark Block
            BlockPos foundPos = this.findNearestDarkBlock();
            System.out.print(foundPos);
            if (foundPos != null) {
                // 找到备用位置，更新字段（如果需要长期使用）
                this.summoningBlockPos = foundPos;
                return foundPos;
            }

            // 如果找不到任何 Dark Block，则返回 Boss 实体当前的位置作为最低保障
            return this.getBlockPos();
        }
        return this.summoningBlockPos;
    }
    private BlockPos findNearestDarkBlock() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            DarkBlockTracker tracker = DarkBlockTracker.get(serverWorld);

            // 使用 tracker 的 findClosest 方法，该方法现在已移除距离限制
            Optional<BlockPos> targetPosOptional = tracker.findClosest(this.getBlockPos());
            return targetPosOptional.orElse(null);
        }

        return null;
    }

    private void resetSummoningBlock() {
        BlockPos pos = this.getSummoningAnchorPos();
        if (pos != null) {
            World world = this.getWorld();
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isOf(ModBlocks.DARK_BLOCK) && blockState.get(DarkPortalFrameBlock.EYE)) {
                BlockState newBlockState = blockState.with(DarkPortalFrameBlock.EYE, false);
                world.setBlockState(pos, newBlockState, 3);
                world.updateComparators(pos, ModBlocks.DARK_BLOCK);
            }
        }
    }

    // --- T I C K 主循环 ---
    @Override
    public void tick() {
        super.tick();
        this.hurtTime = 0;
        getSummoningAnchorPos();

        if (!this.getWorld().isClient()) {
            if (!this.isAppearing()) {
                PlayerEntity closestPlayer = this.getWorld().getClosestPlayer(this, this.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE));

                if (closestPlayer == null) {
                    this.resetSummoningBlock();
                    this.disappear();
                    return;
                }
            }
            if (this.beamDelayTimer > 0) {
                this.beamDelayTimer--;
                if (this.beamDelayTimer == 0) {
                    this.executeBeamAndDamageLogic(); // <-- 必须存在此调用
                }
            }
            if (this.age % 20 == 0) {
                System.out.println("DEBUG (GLOBAL): State=" + this.attackState.name() +
                        ", Shield=" + this.currentShield +
                        ", Hexagram=" + this.getHexagramType().name() +
                        ", Timer=" + this.dataTracker.get(ATTACK_TIMER_TRACKED) +
                        ", HexTimer=" + this.hexagramTimer);
            }
            if (this.isParticlized()) {
                boolean shouldBeParticlized =
                        this.attackState == AttackState.FADE_OUT ||
                                this.dataTracker.get(DAMAGE_PARTICLE_TIMER_TRACKED) > 0;

                if (!shouldBeParticlized) {
                    System.out.println("DEBUG (Server): [SECURITY CHECK] Boss Particlized state stuck! Forcing finishDamageParticle.");
                    finishDamageParticle();
                    if (this.attackState != AttackState.IDLE) stopAttackLogic();
                }
            }

            // 连闪
            if (rapidFlashLeft > 0) {
                rapidFlashLeft--;
                triggerDamageParticle();
            }

            if (this.currentShield <= 0.001F) {
                this.hexagramTimer--;
                this.dataTracker.set(HEXAGRAM_TIMER_TRACKED, this.hexagramTimer);

                if (this.hexagramTimer <= 0) {
                    HexagramType oldType = this.getHexagramType();
                    HexagramType newType = this.getRandomHexagramType();

                    // 确保新的类型和旧的类型不同，或者当前是 NONE (破盾时)
                    while (oldType == newType) {
                        newType = this.getRandomHexagramType();
                    }

                    this.setHexagramType(newType);
                    this.hexagramTimer = HEXAGRAM_CYCLE_DURATION;
                    System.out.println("DEBUG (Server): Hexagram random change to " + newType.name());
                }
            }


            // 3. Boss 条更新逻辑
            updateBossBars();

            // 4. 出场逻辑
            if (this.isAppearing()) {
                doSpawnBurstParticles();
                this.playIntro();
                return;
            }


            if (this.attackCooldownTimer > 0) this.attackCooldownTimer--;
            if (this.getVelocity().lengthSquared() > 1.0E-4) this.setVelocity(Vec3d.ZERO);

            this.applyRangeAura();
            this.applyPhasePunishment();

            if ((this.currentTarget == null || !this.currentTarget.isAlive()) && this.attackState != AttackState.IDLE) {
                stopAttackLogic();
                return;
            }
            if (this.currentTarget != null) this.getLookControl().lookAt(this.currentTarget, 30, 30);

            // 5. 攻击状态机
            switch (this.attackState) {
                case IDLE -> {
                    if (this.attackCooldownTimer <= 0) {
                        startAttackLogic();
                    }
                }
                case WINDUP -> handleWindup();
                case FADE_OUT -> handleFadeOut();
                case ATTACK_TELEPORT -> handleAttackTeleport();
                case SWIPE -> handleWhirlwind();
                case RECOVERY -> handleRecovery();
                case IN -> handleIntro();
                case OUT -> {}
                case COUNTER_ATTACK -> handleCounterAttack(); // 🔥 处理反击动作
            }

            this.dataTracker.set(ATTACK_STATE_TRACKED, this.attackState.ordinal());
            this.dataTracker.set(ATTACK_TIMER_TRACKED, this.attackTimer);

            // 6. 受伤粒子化计时器
            int dmgTimer = this.dataTracker.get(DAMAGE_PARTICLE_TIMER_TRACKED);
            if (dmgTimer > 0) {
                dmgTimer--;
                this.dataTracker.set(DAMAGE_PARTICLE_TIMER_TRACKED, dmgTimer);
                if (dmgTimer == 0) finishDamageParticle();
            }
        }
    }
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source); // 执行原版死亡逻辑（掉落等）

        if (!this.getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            BlockPos deathPos = this.getBlockPos();
            serverWorld.syncWorldEvent(2006, deathPos, 0);
            // 1. 执行自定义死亡特效
            this.spawnEpicDeathEffects(serverWorld);

            // 2. 向附近玩家发送大标题 (Title)
            this.sendDeathTitleToPlayers(serverWorld);

            // 3. 重置召唤方块
            this.resetSummoningBlock();

            // 4. 清理 Boss 条
            this.shieldBossBar.clearPlayers();
            this.healthBossBar.clearPlayers();
        }
    }
    private void sendDeathTitleToPlayers(ServerWorld world) {
        Text titleText = Text.translatable("title.psychosis.goddess_defeated").formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
        Text subtitleText = Text.translatable("subtitle.psychosis.goddess_defeated").formatted(Formatting.GRAY, Formatting.ITALIC);
        for (ServerPlayerEntity player : world.getPlayers(p -> p.distanceTo(this) < 64)) {
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 60, 20));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(subtitleText));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(titleText));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false));
        }
    }
    private void spawnEpicDeathEffects(ServerWorld world) {
        Vec3d pos = this.getPos().add(0, 1.5, 0);
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 5.0F, 0.5F);
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.0F, 0.2F);
        world.spawnParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 5, 0.1, 0.1, 0.1, 0.1);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 100, 0.5, 0.5, 0.5, 0.2);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 150, 1.0, 1.0, 1.0, 0.15);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 200, 2.0, 2.0, 2.0, 0.05);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 1);
    }
    private void executeBeamAndDamageLogic() {
        if (this.getWorld().isClient() || !(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // --- 常量定义 ---
        final int TARGET_RANGE = COUNTER_ATTACK_RANGE;
        final double NARROW_BEAM_EXPAND = 0.1; // 极细光束的碰撞箱扩展量

        Optional<RegistryEntry.Reference<DamageType>> darkDamageEntry = serverWorld.getRegistryManager()
                .get(RegistryKeys.DAMAGE_TYPE)
                .getEntry(Psychosis.DARK_DAMAGE);

        // 重新获取目标列表 (以防玩家跑出范围)
        Box searchBox = this.getBoundingBox().expand(TARGET_RANGE);
        Set<PlayerEntity> currentTargets = new HashSet<>(serverWorld.getPlayers(player ->
                player.isAlive() && searchBox.contains(player.getPos())
        ));

        // --- 核心发射和伤害循环 ---
        for (PlayerEntity target : currentTargets) {

            Vec3d targetPos = target.getEyePos();
            BlockPos blockLocation = this.getSummoningAnchorPos();

            if (blockLocation == null) continue;

            // 光束起点和终点计算
            Vec3d startPosCenter = Vec3d.ofCenter(blockLocation);
            Vec3d beamOrigin = startPosCenter.add(0.0, 0.1, 0.0);
            Vec3d directionToTarget = targetPos.subtract(beamOrigin).normalize();
            Vec3d beamEnd = beamOrigin.add(directionToTarget.multiply(TARGET_RANGE));


            // 1. 粒子视觉效果 (发送 ModNetworking 包)
            // ⚠️ 确保 ModNetworking.sendBeamPacket 方法存在
            ModNetworking.sendBeamPacket(serverWorld, beamOrigin, beamEnd);


            // 2. 伤害和效果逻辑 (服务器端 Raycast 射线检测)
            List<Entity> hitEntities = serverWorld.getOtherEntities(
                    this,
                    // 💥 极细光束宽度
                    new Box(beamOrigin, beamEnd).expand(NARROW_BEAM_EXPAND),
                    entity -> entity.isAlive() && entity instanceof LivingEntity && entity.distanceTo(this) <= (double) TARGET_RANGE
            );

            for (Entity hitEntity : hitEntities) {
                if (hitEntity instanceof PlayerEntity player) {
                    player.addStatusEffect(new StatusEffectInstance(
                            this.darkEffectEntry,
                            StatusEffectInstance.INFINITE,
                            100,
                            true, true, true
                    ));
                } else if (hitEntity instanceof LivingEntity livingTarget) {
                    // 如果击中非 IPlayerEntity 的其他生物（如小怪、动物）
                    darkDamageEntry.ifPresent(damageEntry -> {
                        DamageSource damageSource = new DamageSource(damageEntry);
                        livingTarget.damage(damageSource, Float.MAX_VALUE);
                    });
                }
            }
        }

        // 动画播放完毕后，重置 Boss 动画状态
        this.stopAttackLogic(); // ⚠️ 确保 resetAnimationState() 存在
    }

    // 🔥 反击动作处理：仅计时，完成后回到 IDLE
    private void handleCounterAttack() {
        this.attackTimer--;

        if (this.attackTimer <= 0) {
            // 反击动作结束，回到 IDLE
            stopAttackLogic();
        }
    }

    // 在 GoddessEntity 的 updateBossBars 方法中
    private void updateBossBars() {
        if (this.isAppearing() || this.getAttackState() == AttackState.IN) {
            this.shieldBossBar.setVisible(false);
            this.healthBossBar.setVisible(false);
            return;
        }

        // ⭐ 名字分配：护盾负责显示名字
        this.shieldBossBar.setName(this.getDisplayName());
        this.shieldBossBar.setColor(BossBar.Color.WHITE); // 对应护盾贴图

        // ⭐ 血量名字设为空格，颜色设为红色
        this.healthBossBar.setName(Text.literal(" "));
        this.healthBossBar.setColor(BossBar.Color.RED);   // 对应血量贴图

        this.shieldBossBar.setPercent(this.currentShield / SHIELD_HEALTH);
        this.healthBossBar.setPercent(this.getHealth() / this.getMaxHealth());

        this.shieldBossBar.setVisible(true);
        this.healthBossBar.setVisible(true);
    }

    // --- 状态处理逻辑 ---
    private void doSpawnBurstParticles() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        Vec3d pos = this.getPos().add(0, this.getHeight() / 2f, 0);
        sw.spawnParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 1);
        sw.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 80, 0.5, 0.5, 0.5, 0.15);
        sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 60, 0.8, 0.8, 0.8, 0.12);
    }

    public void playIntro() {
        if (getWorld().isClient() || attackState == AttackState.IN) return;
        stopAttackLogic();
        setNoGravity(true);
        attackState = AttackState.IN;
        attackTimer = 100;
        setParticlized(true);
        dataTracker.set(IS_APPEARING_TRACKED, false);
    }

    private void handleIntro() {
        this.attackTimer--;

        if (this.attackTimer > 0) {
            if (getWorld() instanceof ServerWorld sw) {
                Vec3d pos = this.getPos().add(0, this.getHeight() / 2f, 0);
                sw.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.5, 0.5, 0.5, 0.01);
            }
            return;
        }

        setParticlized(false);
        getWorld().sendEntityStatus(this, (byte) 82);
        attackState = AttackState.IDLE;
        setNoGravity(false);
    }

    public void setOutState() {
        if (getWorld().isClient()) return;
        stopAttackLogic();
        attackState = AttackState.OUT;
        setParticlized(true);
    }

    private void handleWindup() {
        this.attackTimer--;

        if (this.attackTimer <= 0) {
            double distance = this.distanceTo(this.currentTarget);
            Box currentBox = this.getBoundingBox();
            float entityWidth = (float)(currentBox.maxX - currentBox.minX);

            if (distance <= WHIRLWIND_TRIGGER_DISTANCE + entityWidth * 0.5) {
                this.attackState = AttackState.SWIPE;
                this.attackTimer = SWIPE_DURATION;
            } else {
                this.attackState = AttackState.FADE_OUT;
                this.attackTimer = FADE_OUT_DURATION;
                this.setParticlized(true);
                this.handleTeleportAndParticlize(this.currentTarget);
                this.getWorld().playSound(
                        null,
                        this.getBlockPos(),
                        SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                        SoundCategory.HOSTILE,
                        1.0F,
                        1.0F
                );
            }

            this.navigation.stop();

            if (this.hitEntities != null) {
                this.hitEntities.clear();
            }
        }
    }

    private void handleFadeOut() {
        this.attackTimer--;

        if (this.attackTimer <= 0) {

            this.setParticlized(false);

            this.getWorld().playSound(
                    null,
                    this.getBlockPos(),
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                    SoundCategory.HOSTILE,
                    4.0F,
                    0.8F + this.getWorld().random.nextFloat() * 0.2F
            );

            this.attackState = AttackState.SWIPE;
            this.attackTimer = SWIPE_DURATION;
        }
    }

    private void handleAttackTeleport() {
        this.attackTimer--;

        if (this.attackTimer <= 0) {
            this.attackState = AttackState.SWIPE;
            this.attackTimer = SWIPE_DURATION;
        }
    }

    private void handleWhirlwind() {
        this.attackTimer--;

        int remainingTicks = SWIPE_DURATION - this.attackTimer;

        if (remainingTicks == SWIPE_DURATION / 2) {
            performWhirlwindAttack();
        }

        if (remainingTicks == SWIPE_DURATION / 2 + WHIRLWIND_SLASH_DELAY) {
            spawnWhirlwindSlash();
        }

        if (this.attackTimer <= 0) {
            this.attackState = AttackState.RECOVERY;
            this.attackTimer = RECOVERY_DURATION;
        }
    }

    private void handleTeleportAndParticlize(PlayerEntity target) {
        if (this.getWorld().isClient() || target == null) return;

        this.setVelocity(Vec3d.ZERO);

        float yaw = target.getYaw();
        double radians = (double)MathHelper.RADIANS_PER_DEGREE * yaw;
        double forwardX = -MathHelper.sin((float)radians);
        double forwardZ = MathHelper.cos((float)radians);
        double targetX = target.getX() - forwardX * TELEPORT_BEHIND_DISTANCE;
        double targetY = target.getY();
        double targetZ = target.getZ() - forwardZ * TELEPORT_BEHIND_DISTANCE;

        this.teleport(targetX, targetY, targetZ, true);

        this.getWorld().playSound(
                null,
                this.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.HOSTILE,
                1.0F,
                1.0F
        );

        this.setParticlized(true);

        this.attackState = AttackState.FADE_OUT;
        this.attackTimer = FADE_OUT_DURATION;
    }

    private void performWhirlwindAttack() {
        if (this.getWorld().isClient()) return;

        World targetWorld = this.getWorld();
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        Box aoeBox = new Box(
                x - WHIRLWIND_AOE_RANGE, y - 1.0, z - WHIRLWIND_AOE_RANGE,
                x + WHIRLWIND_AOE_RANGE, y + 2.0, z + WHIRLWIND_AOE_RANGE
        );

        targetWorld.getOtherEntities(this, aoeBox, entity -> entity instanceof LivingEntity)
                .forEach(entity -> {
                    if (entity != this) {
                        LivingEntity targetEntity = (LivingEntity) entity;

                        float totalDamage;
                        if (targetEntity instanceof PlayerEntity player) {
                            totalDamage = player.getMaxHealth() * GoddessEntity.PLAYER_HEALTH_PERCENT_WHIRLWIND_DAMAGE + GoddessEntity.FLAT_WHIRLWIND_DAMAGE;
                        } else {
                            totalDamage = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 2.0F;
                        }

                        targetEntity.damage(targetWorld.getDamageSources().mobAttack(this), totalDamage);

                        if (targetEntity instanceof PlayerEntity player && this.darkEffectEntry != null) {
                            StatusEffectInstance currentDark = player.getStatusEffect(this.darkEffectEntry);

                            int currentAmplifier = currentDark != null ? currentDark.getAmplifier() : -1;
                            int newAmplifier = currentAmplifier + GoddessEntity.WHIRLWIND_DARK_INCREMENT_LEVEL;

                            player.addStatusEffect(new StatusEffectInstance(
                                    this.darkEffectEntry,
                                    StatusEffectInstance.INFINITE,
                                    newAmplifier,
                                    true, true, true
                            ));
                        }
                    }
                });

        targetWorld.playSound(null, x, y, z,
                net.minecraft.sound.SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                net.minecraft.sound.SoundCategory.HOSTILE, 0.4F, 0.6F + targetWorld.random.nextFloat() * 0.4F);
        // 假设 WhirlwindSlashEntity 已正确定义
        // ⚠️ 注意：您需要确保 WhirlwindSlashEntity 可以在您的项目中被调用
        WhirlwindSlashEntity slash = new WhirlwindSlashEntity(targetWorld, this);
        final double Y_OFFSET = 2.5f;
        slash.setScale(2.0F);
        slash.setPos(x, y + Y_OFFSET, z);
        targetWorld.spawnEntity(slash);

        spawnWhirlwindParticles();
    }

    private void spawnWhirlwindParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        double x = this.getX();
        double y = this.getY() + 1.0;
        double z = this.getZ();
        float radius = WHIRLWIND_AOE_RANGE;
        int particleCount = 60;

        serverWorld.spawnParticles(
                ParticleTypes.SWEEP_ATTACK,
                x, y, z,
                10,
                radius * 0.5, y * 0.05, radius * 0.5,
                0.0
        );

        for (int i = 0; i < particleCount / 2; i++) {
            double angle = this.random.nextDouble() * 2 * Math.PI;
            double r = this.random.nextDouble() * radius;
            double px = x + r * Math.cos(angle);
            double pz = z + r * Math.sin(angle);
            double py = y + this.random.nextDouble() * 1.5 - 0.5;

            serverWorld.spawnParticles(
                    ParticleTypes.SOUL,
                    px, py, pz,
                    1,
                    0.1, 0.1, 0.1,
                    0.2
            );
        }
    }

    private void handleRecovery() {
        this.attackTimer--;

        if (this.isParticlized()) {
            this.setParticlized(false);
        }

        this.setVelocity(Vec3d.ZERO);

        if (this.attackTimer <= 0) {
            stopAttackLogic();
        }
    }

    private void spawnWhirlwindSlash() {
        if (this.getWorld().isClient()) return;
        this.setParticlized(true);
    }

    private void disappear() {
        if (this.getWorld().isClient()) return;
        if (this.getSummoningAnchorPos() != null) {
            World world = this.getWorld();
            BlockState blockState = world.getBlockState(this.getSummoningAnchorPos());
            if (blockState.isOf(ModBlocks.DARK_BLOCK) && blockState.get(DarkPortalFrameBlock.EYE)) {
                BlockState newBlockState = blockState.with(DarkPortalFrameBlock.EYE, false);
                world.setBlockState(this.getSummoningAnchorPos(), newBlockState, 3);
                world.updateComparators(this.getSummoningAnchorPos(), ModBlocks.DARK_BLOCK);
            }
        }
        this.resetSummoningBlock();
        this.shieldBossBar.clearPlayers();
        this.healthBossBar.clearPlayers();
        this.getWorld().playSound(null, this.getBlockPos(),
                net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                SoundCategory.HOSTILE, 0.2F, 0.5F);
        this.discard();
    }

    // 机制 1: 范围性光环惩罚 (暗影效果)
    public void applyRangeAura() {
        if (this.getWorld().isClient() || this.isParticlized()) return;

        Box effectRangeBox = this.getBoundingBox().expand(PUNISHMENT_RANGE);

        for (ServerPlayerEntity player : this.getWorld().getPlayers()
                .stream()
                .filter(p -> p instanceof ServerPlayerEntity)
                .map(p -> (ServerPlayerEntity) p)
                .toList()) {

            if (effectRangeBox.contains(player.getX(), player.getY(), player.getZ()) && player.isAlive()) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.GLOWING,
                        200,
                        0,
                        true,
                        false,
                        true
                ));
            } else {
                if (player.hasStatusEffect(StatusEffects.GLOWING)) {
                    player.removeStatusEffect(StatusEffects.GLOWING);
                }
            }
        }
    }

    // 机制 2: 阶段惩罚 (永久黑暗/易伤/狂暴)
    private void applyPhasePunishment() {
        if (this.getWorld().isClient() || this.darkEffectEntry == null || this.vulnerableEffectEntry == null || this.frenzyEffectEntry == null) {
            return;
        }

        int darkAmplifier = this.currentShield > 0 ? PRE_SHIELD_DARK_LEVEL : POST_SHIELD_DARK_LEVEL;
        boolean isShieldBroken = this.currentShield <= 0;
        int vulnerableAmplifier = 0;
        int frenzyAmplifier = 0;

        for (ServerPlayerEntity player : this.getWorld().getPlayers()
                .stream()
                .filter(p -> p instanceof ServerPlayerEntity)
                .map(p -> (ServerPlayerEntity) p)
                .toList()) {

            if (player.isAlive()) {
                StatusEffectInstance currentDark = player.getStatusEffect(this.darkEffectEntry);
                int currentDarkAmplifier = currentDark != null ? currentDark.getAmplifier() : -1;
                if (darkAmplifier > currentDarkAmplifier) {
                    player.addStatusEffect(new StatusEffectInstance(
                            this.darkEffectEntry,
                            StatusEffectInstance.INFINITE,
                            darkAmplifier,
                            true,
                            true,
                            true
                    ));
                }
                if (!player.hasStatusEffect(vulnerableEffectEntry)) {
                    player.addStatusEffect(new StatusEffectInstance(
                            this.vulnerableEffectEntry,
                            StatusEffectInstance.INFINITE,
                            vulnerableAmplifier,
                            true,
                            true,
                            true
                    ));
                }
                if (isShieldBroken) {
                    if (!player.hasStatusEffect(frenzyEffectEntry)) {
                        player.addStatusEffect(new StatusEffectInstance(
                                this.frenzyEffectEntry,
                                StatusEffectInstance.INFINITE,
                                frenzyAmplifier,
                                true,
                                true,
                                true
                        ));
                    }
                } else {
                    player.removeStatusEffect(this.frenzyEffectEntry);
                }
            }
        }
    }

    // BossBar/NBT/Getter/Setter
// 在 GoddessEntity.java 中修改此方法
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.healthBossBar.addPlayer(player);
        this.shieldBossBar.addPlayer(player);
        this.updateBossBars();
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.shieldBossBar.removePlayer(player);
        this.healthBossBar.removePlayer(player);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("CurrentState", this.currentState.ordinal());
        nbt.putInt("AppearanceTicks", this.appearanceTicks);
        nbt.putBoolean("IsPhaseTwo", this.isPhaseTwo);
        nbt.putBoolean("IsParticlized", this.isParticlized());
        nbt.putFloat("CurrentShield", this.currentShield);

        nbt.putInt("AttackState", this.attackState.ordinal());
        nbt.putInt("AttackTimer", this.attackTimer);
        nbt.putInt("AttackCooldownTimer", this.attackCooldownTimer);

        nbt.putInt("DamageParticleTimer", this.dataTracker.get(DAMAGE_PARTICLE_TIMER_TRACKED));
        nbt.putInt("HexagramTimer", this.hexagramTimer);

        if (this.getSummoningAnchorPos() != null) {
            nbt.putLong("SummoningBlockPos", this.getSummoningAnchorPos().asLong());
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

        if (this.hasCustomName()) {
            this.shieldBossBar.setName(this.getDisplayName());
            this.healthBossBar.setName(this.getDisplayName());
        }

        this.appearanceTicks = nbt.getInt("AppearanceTicks");
        this.isPhaseTwo = nbt.getBoolean("IsPhaseTwo");
        if (nbt.contains("IsParticlized")) {
            this.setParticlized(nbt.getBoolean("IsParticlized"));
        }
        if (nbt.contains("CurrentShield")) {
            this.currentShield = nbt.getFloat("CurrentShield");
        }
        if (!this.getWorld().isClient()) {
            this.updateBossBars();
        }

        if (nbt.contains("AttackState")) {
            this.attackState = AttackState.values()[nbt.getInt("AttackState")];
        }
        this.attackTimer = nbt.getInt("AttackTimer");
        this.attackCooldownTimer = nbt.getInt("AttackCooldownTimer");

        if (nbt.contains("HexagramTimer")) {
            this.hexagramTimer = nbt.getInt("HexagramTimer");
        }

        if (nbt.contains("DamageParticleTimer")) {
            this.dataTracker.set(DAMAGE_PARTICLE_TIMER_TRACKED, nbt.getInt("DamageParticleTimer"));
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

    // 辅助 Getter/Setter
    public boolean isAppearing() {
        if (this.dataTracker == null) return true;
        return this.dataTracker.get(IS_APPEARING_TRACKED);
    }

    public boolean isParticlized() {
        if (this.dataTracker == null) return false;
        return this.dataTracker.get(IS_PARTICLIZED_TRACKED);
    }

    public void setParticlized(boolean particlized) {
        final byte PARTICLIZE_STATUS = 80;
        final byte UNPARTICLIZE_STATUS = 81;

        if (!this.getWorld().isClient()) {
            this.dataTracker.set(IS_PARTICLIZED_TRACKED, particlized);

            if (particlized) {
                this.getWorld().sendEntityStatus(this, PARTICLIZE_STATUS);
            } else {
                this.getWorld().sendEntityStatus(this, UNPARTICLIZE_STATUS);
            }
        }
    }

    @Override
    public void handleStatus(byte status) {
        final byte PARTICLIZE_STATUS = 80;
        final byte UNPARTICLIZE_STATUS = 81;
        final byte DASH_END_STATUS = 82;
        // 90 仅用于一次性通知，持续渲染已转移到客户端 Mixin

        World world = this.getWorld();
        double x = this.getX();
        double y = this.getY() + this.getHeight() / 2.0;
        double z = this.getZ();

        if (status == PARTICLIZE_STATUS || status == UNPARTICLIZE_STATUS) {
            SimpleParticleType particle = (status == PARTICLIZE_STATUS) ? ParticleTypes.END_ROD : ParticleTypes.SWEEP_ATTACK;

            for (int i = 0; i < 32; ++i) {
                double d0 = this.random.nextGaussian() * 0.02;
                double d1 = this.random.nextGaussian() * 0.02;
                double d2 = this.random.nextGaussian() * 0.02;
                world.addParticle(particle, x, y, z, d0, d1, d2);
            }
        } else if (status == DASH_END_STATUS) {
            for (int i = 0; i < 48; ++i) {
                double d0 = this.random.nextGaussian() * 0.03;
                double d1 = this.random.nextGaussian() * 0.03;
                double d2 = this.random.nextGaussian() * 0.03;
                world.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, d0, d1, d2);
            }
        } else {
            super.handleStatus(status);
        }
    }

    public AttackState getAttackState() {
        if (this.dataTracker == null) return AttackState.IDLE;
        return AttackState.values()[this.dataTracker.get(ATTACK_STATE_TRACKED)];
    }

    public Box getAttackBox() {
        return this.getBoundingBox().expand(WHIRLWIND_AOE_RANGE);
    }
}