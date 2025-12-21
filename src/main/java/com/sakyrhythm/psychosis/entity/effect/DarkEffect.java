package com.sakyrhythm.psychosis.entity.effect;

import com.sakyrhythm.psychosis.Psychosis;
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

    // ⚠️ 移除缓存变量：darkDamageEntry, vulnerableEffectEntry, frenzyEffectEntry
    // private Optional<RegistryEntry.Reference<DamageType>> darkDamageEntry = Optional.empty();
    // private Optional<RegistryEntry.Reference<StatusEffect>> vulnerableEffectEntry = Optional.empty();
    // private Optional<RegistryEntry.Reference<StatusEffect>> frenzyEffectEntry = Optional.empty();

    private static final int DAMAGE_INTERVAL_TICKS = 10;

    public DarkEffect() {
        super(StatusEffectCategory.HARMFUL, 0x45283C);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        super.onApplied(entity, amplifier);
        if (entity instanceof ILivingEntity iEntity) {
            iEntity.psychosis_template_1_21$setCBHurt(true);
        }
    }

    // ⚠️ 移除 initializeEntries 方法

    // --- 辅助方法: 动态获取效果引用 ---
    private Optional<RegistryEntry.Reference<StatusEffect>> getStatusEffectEntry(LivingEntity entity, String id) {
        return entity.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, id)));
    }

    private Optional<RegistryEntry.Reference<DamageType>> getDamageTypeEntry(LivingEntity entity) {
        return entity.getWorld().getRegistryManager()
                .get(RegistryKeys.DAMAGE_TYPE)
                .getEntry(Psychosis.DARK_DAMAGE);
    }

    public void giveVulnerableEffect(LivingEntity entity) {
        getStatusEffectEntry(entity, "vulnerable").ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }

    public void giveFrenzyEffect(LivingEntity entity) {
        getStatusEffectEntry(entity, "frenzy").ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }

    // --- 主更新逻辑 ---

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        // 每次 Tick 时，动态获取最新的 DamageType 引用
        Optional<RegistryEntry.Reference<DamageType>> currentDarkDamageEntry = getDamageTypeEntry(entity);

        if (entity.getHealth() <= 0.0F) {
            return super.applyUpdateEffect(entity, amplifier);
        }

        if (entity instanceof IPlayerEntity playerInterface) {
            ServerWorld serverWorld = entity.getWorld() instanceof ServerWorld ? (ServerWorld) entity.getWorld() : null;

            int currentLock = playerInterface.getDark();

            // -------------------------------------------------------------
            // 🚨 关键修改：移除所有 'else' 关键字，实现累进解锁 (Cascading Unlock)
            // -------------------------------------------------------------

            // --- 阶段 4: 等级 >= 100 (致死阶段) ---
            if (amplifier >= 100) {
                // 锁的等级必须是 101 或更高
                if (currentLock < 101) {
                    if (!playerInterface.getDarkMsg4Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark4").formatted(Formatting.GOLD));
                        }
                        playerInterface.setDarkMsg4Sent(true);
                    }

                    // 造成致死伤害 (使用动态获取的引用)
                    currentDarkDamageEntry.ifPresent(damageEntry -> {
                        DamageSource damageSource = new DamageSource(damageEntry);
                        entity.damage(damageSource, Float.MAX_VALUE);
                    });
                    playerInterface.setDark(101); // 设置最高锁
                }
            }

            // --- 阶段 3: 等级 >= 70 ---
            // 药水等级满足 70 级 **且** 锁的等级必须低于 100 才能触发
            if (amplifier >= 70) {
                if (currentLock < 100) {
                    if (!playerInterface.getDarkMsg3Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark3").formatted(Formatting.RED));
                            giveFrenzyEffect(entity);
                        }
                        playerInterface.setDarkMsg3Sent(true);
                    }
                    playerInterface.setDark(100); // 设置 70 级阶段锁
                }
            }

            // --- 阶段 2: 等级 >= 30 ---
            // 药水等级满足 30 级 **且** 锁的等级必须低于 50 才能触发
            if (amplifier >= 30) {
                if (currentLock < 50) {
                    if (!playerInterface.getDarkMsg2Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark2").formatted(Formatting.DARK_PURPLE));
                            giveVulnerableEffect(entity);
                        }
                        playerInterface.setDarkMsg2Sent(true);
                    }
                    playerInterface.setDark(50); // 设置 30 级阶段锁
                }
            }

            // --- 阶段 1: 等级 >= 0 (基础阶段) ---
            // 药水等级满足 0 级 **且** 锁的等级必须低于 1 才能触发
            if (amplifier >= 0) {
                if (currentLock < 1) {
                    if (!playerInterface.getDarkMsg1Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark1").formatted(Formatting.DARK_GRAY));
                        }
                        playerInterface.setDarkMsg1Sent(true);
                    }
                    playerInterface.setDark(1); // 设置基础阶段锁
                }
            }
        } else {
            // --- 伤害逻辑 (应用于非 PlayerEntity) ---
            if (!entity.getWorld().isClient()) {
                long currentTick = entity.getWorld().getTime();

                if (entity instanceof ILivingEntity cooldownEntity) {
                    long lastDamageTime = cooldownEntity.psychopomp$getLastDarkDamageTime();

                    if (currentTick - lastDamageTime >= DAMAGE_INTERVAL_TICKS) {

                        // 使用动态获取的引用
                        currentDarkDamageEntry.ifPresent(damageEntry -> {
                            if (amplifier >= 0) {
                                float damageAmount = (float) Math.pow(2, amplifier);

                                LivingEntity attacker;
                                attacker = cooldownEntity.psychosis_template_1_21$getLastAttacker();

                                DamageSource damageSource = new DamageSource(damageEntry, attacker);
                                entity.damage(damageSource, damageAmount);

                                cooldownEntity.psychopomp$setLastDarkDamageTime(currentTick);
                            }
                        });
                    }
                }
            }
        }

        return super.applyUpdateEffect(entity, amplifier);
    }
}