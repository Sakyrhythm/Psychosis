package com.sakyrhythm.psychosis.entity.ai.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Boss 机制目标：负责半血惩罚的持续施加和阶段切换信号的发出。
 * 此 Goal 适用于 DarkGodEntity 或任何暴露了 getMaxHealth() 和 getHealth() 方法的 HostileEntity。
 */
public class BossMechanismGoal extends Goal {

    private static final Logger LOGGER = LoggerFactory.getLogger("DarkGodMechanismGoal");

    // --- 实体属性和常量（需要从宿主实体获取） ---
    private final HostileEntity boss;
    private boolean phaseTwoTriggered = false;

    // 这些常量假定在宿主实体（DarkGodEntity）中定义，为了让 Goal 独立运行，在此重新定义。
    private static final float PHASE_TWO_THRESHOLD = 0.50F; // 50% 血量切换阶段/惩罚触发点
    private static final float EPSILON = 0.001F; // 浮点数容差
    private static final Identifier DARK_EFFECT_ID = Identifier.of("psychosis", "dark");
    private static final int PUNISHMENT_RANGE = 20;
    private static final int MIN_DARK_LEVEL = 30;
    private static final int INFINITE_DURATION = Integer.MAX_VALUE;


    // --- 构造函数 ---
    public BossMechanismGoal(HostileEntity boss) {
        this.boss = boss;
        // 设置控制域：Boss 持续看向目标
        this.setControls(EnumSet.of(Control.LOOK));
    }


    // --- Goal 基础方法 ---

    // 判断 AI 任务是否可以开始
    @Override
    public boolean canStart() {
        return this.boss.isAlive() && !this.boss.getWorld().isClient();
    }

    // 判断 AI 任务是否应该继续执行
    @Override
    public boolean shouldContinue() {
        return this.boss.isAlive();
    }

    // 每刻执行的逻辑
    @Override
    public void tick() {
        // 由于是独立文件，我们假设 Boss 实体（DarkGodEntity）自己处理 stateTimer 的增量。
        handlePhaseOne();
    }


    // --- 机制实现方法 ---

    /**
     * 持续施加惩罚，并在达到血线时发出阶段切换信号。
     */
    private void handlePhaseOne() {
        float maxHealth = this.boss.getMaxHealth();
        float currentHealth = this.boss.getHealth();
        float phaseTwoThreshold = maxHealth * PHASE_TWO_THRESHOLD;

        // 1. 阶段切换检查 (达到 50% 血线)
        if (currentHealth <= phaseTwoThreshold + EPSILON) {

            // 确保只通知一次 (通知外部系统进行实体替换)
            if (!phaseTwoTriggered) {
                LOGGER.warn("Dark God (Phase 1) Health <= 50%. Signaling Phase Two transition for external handling. Entity ID: {}", this.boss.getId());
                phaseTwoTriggered = true;
            }
        }

        // 2. 惩罚检查 (血量低于 50% 时持续施加惩罚)
        if (currentHealth <= phaseTwoThreshold) {
            // 由于 ENFORCEMENT_INTERVAL=1，这里直接调用，保证每刻施加
            enforceDarkAura();
        }
    }

    /**
     * 实现持续惩罚机制 (施加永久效果，只针对玩家)。
     */
    private void enforceDarkAura() {
        World world = this.boss.getWorld();
        if (world.isClient()) {
            return;
        }

        // 尝试获取 Dark Effect 的注册条目
        RegistryEntry<StatusEffect> darkEffectEntry = world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                .getEntry(DARK_EFFECT_ID)
                .orElse(null);

        if (darkEffectEntry == null) {
            return;
        }

        Box searchBox = this.boss.getBoundingBox().expand(PUNISHMENT_RANGE);

        // 目标筛选器只针对 PlayerEntity (玩家实体)
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                PlayerEntity.class,
                searchBox,
                (player) -> player.isAlive()
        );

        for (PlayerEntity player : nearbyPlayers) {
            // 施加/刷新永久效果
            player.addStatusEffect(new StatusEffectInstance(
                    darkEffectEntry,
                    INFINITE_DURATION,
                    MIN_DARK_LEVEL - 1,
                    true,               // 环境效果
                    true,               // 显示粒子
                    true                // 显示图标
            ));
        }
    }
}