package com.sakyrhythm.psychosis.entity.effect;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import com.sakyrhythm.psychosis.interfaces.ILivingEntity;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class DarkEffect extends StatusEffect {
    private Optional<RegistryEntry.Reference<DamageType>> darkDamageEntry = Optional.empty();
    private Optional<RegistryEntry.Reference<StatusEffect>> vulnerableEffectEntry = Optional.empty();
    private Optional<RegistryEntry.Reference<StatusEffect>> frenzyEffectEntry = Optional.empty();

    Boolean dark = true;

    // 定义伤害间隔：0.5 秒 * 20 刻/秒 = 10 刻
    private static final int DAMAGE_INTERVAL_TICKS = 10;

    public DarkEffect() {
        super(StatusEffectCategory.HARMFUL, 0x45283C);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // StatusEffect 的默认更新频率是 20 刻（1秒），
        // 通过返回 true 可以在每次 tick 时都调用 applyUpdateEffect，配合我们的冷却实现 0.5 秒伤害。
        return true;
    }

    // --- 核心修复 1: 生命周期方法 (用于 cbhurt 和无敌帧) ---

    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        super.onApplied(entity, amplifier);
        // 对所有 LivingEntity 实例设置 cbhurt = true (取消无敌帧)
        if (entity instanceof ILivingEntity iEntity) {
            iEntity.psychosis_template_1_21$setCBHurt(true);
        }
    }

    // --- 辅助方法: 注册表初始化 ---

    private void initializeEntries(LivingEntity entity) {
        // 只有当 Optional 为空时才尝试获取
        if (darkDamageEntry.isEmpty()) {
            darkDamageEntry = entity.getWorld().getRegistryManager()
                    .get(RegistryKeys.DAMAGE_TYPE)
                    .getEntry(Psychosis.DARK_DAMAGE);
        }
        if (vulnerableEffectEntry.isEmpty()) {
            vulnerableEffectEntry = entity.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")));
        }
        if (frenzyEffectEntry.isEmpty()) {
            frenzyEffectEntry = entity.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "frenzy")));
        }
    }

    public void giveVulnerableEffect(LivingEntity entity) {
        // 修复: 使用 ifPresent 施加效果
        vulnerableEffectEntry.ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }

    public void giveFrenzyEffect(LivingEntity entity) {
        // 修复: 使用 ifPresent 施加效果
        frenzyEffectEntry.ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }

    // --- 主更新逻辑 ---

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        initializeEntries(entity);
        if (entity instanceof IPlayerEntity playerInterface) {
            // ... (PlayerEntity 相关的逻辑保持不变)
            if (amplifier >= 100) {
                if (entity.getWorld() instanceof ServerWorld serverWorld) {
                    entity.sendMessage( Text.translatable("dark4").formatted(Formatting.GOLD));
                }
                darkDamageEntry.ifPresent(damageEntry -> {
                    DamageSource damageSource = new DamageSource(damageEntry);
                    entity.damage(damageSource, Float.MAX_VALUE);
                });
            } else if (amplifier == 71) {
                dark = true;
            } else if (amplifier > 70) {
                playerInterface.setDark(100);
            } else if (amplifier == 70) {
                if (entity.getWorld() instanceof ServerWorld serverWorld) {
                    entity.sendMessage( Text.translatable("dark3").formatted(Formatting.RED));
                    giveFrenzyEffect(entity);
                }

                dark = false;
            } else if (amplifier == 31) {
                dark = true;
            } else if (amplifier > 30) {
                playerInterface.setDark(50);
            } else if (amplifier == 30) {
                if (entity.getWorld() instanceof ServerWorld serverWorld) {
                    entity.sendMessage( Text.translatable("dark2").formatted(Formatting.DARK_PURPLE));
                    giveVulnerableEffect(entity);
                }
                dark = false;
            } else if (amplifier == 1) {
                dark = true;
            } else if (amplifier == 0) {
                if (entity.getWorld() instanceof ServerWorld serverWorld) {
                    entity.sendMessage( Text.translatable("dark1").formatted(Formatting.DARK_GRAY));
                    playerInterface.setDark(0);
                }
                dark = false;
            } else {
                playerInterface.setDark(0);
            }
        }
        else {
            // --- 伤害逻辑 (应用于非 PlayerEntity) ---
            if (!entity.getWorld().isClient()) {

                // 1. 获取当前游戏刻
                long currentTick = entity.getWorld().getTime();

                // 2. 尝试将实体转换为我们的冷却接口
                if (entity instanceof ILivingEntity cooldownEntity) {

                    long lastDamageTime = cooldownEntity.psychopomp$getLastDarkDamageTime(); // 调用 Mixin 注入的 getter

                    // 3. 检查是否已经过了足够的间隔 (0.5 秒 = 10 刻)
                    if (currentTick - lastDamageTime >= DAMAGE_INTERVAL_TICKS) {

                        darkDamageEntry.ifPresent(damageEntry -> { // damageEntry 是 RegistryEntry.Reference<DamageType>

                            float damageAmount = (float) Math.pow(2, amplifier);

                            LivingEntity attacker = null;
                            if (entity instanceof ILivingEntity iEntity) {
                                // 假设此方法返回上一个攻击者
                                attacker = iEntity.psychosis_template_1_21$getLastAttacker();
                            }

                            // 4. 造成伤害
                            DamageSource damageSource = new DamageSource(damageEntry, attacker);
                            entity.damage(damageSource, damageAmount);

                            // 5. 伤害造成后，更新时间戳为当前时间，重置冷却
                            cooldownEntity.psychopomp$setLastDarkDamageTime(currentTick); // 调用 Mixin 注入的 setter
                        });
                    }
                }
            }
        }

        return super.applyUpdateEffect(entity, amplifier);
    }
}