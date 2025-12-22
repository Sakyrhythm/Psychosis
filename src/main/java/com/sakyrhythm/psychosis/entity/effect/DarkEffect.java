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
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class DarkEffect extends StatusEffect {

    private static final int DAMAGE_INTERVAL_TICKS = 10;

    public DarkEffect() {
        super(StatusEffectCategory.HARMFUL, 0x45283C);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // 每 1 tick 检查一次效果
        return true;
    }

    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        super.onApplied(entity, amplifier);
        if (entity instanceof ILivingEntity iEntity) {
            iEntity.psychosis_template_1_21$setCBHurt(true);
        }
    }

    // 使用 Reference<StatusEffect> 替代裸 StatusEffect，以确保注册表兼容性
    private Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntryRef;

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

    public void removeEffect(LivingEntity entity, String id) {
        // 优化：使用动态获取的 Reference 进行移除，更符合注册表 API
        getStatusEffectEntry(entity, id).ifPresent(entity::removeStatusEffect);
    }

    // --- 主更新逻辑 ---

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        // 如果实体已死亡，跳过更新
        if (entity.getHealth() <= 0.0F) {
            return super.applyUpdateEffect(entity, amplifier);
        }

        Optional<RegistryEntry.Reference<DamageType>> currentDarkDamageEntry = getDamageTypeEntry(entity);

        if (entity instanceof IPlayerEntity playerInterface) {
            ServerWorld serverWorld = entity.getWorld() instanceof ServerWorld ? (ServerWorld) entity.getWorld() : null;
            int currentLock = playerInterface.getDark();

            // --- 阶段 1 (amplifier >= 0) ---
            if (amplifier >= 0) {
                if (currentLock < 1) {
                    if (!playerInterface.getDarkMsg1Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark1").formatted(Formatting.DARK_GRAY));
                        }
                        playerInterface.setDarkMsg1Sent(true);
                    }
                    playerInterface.setDark(1);
                    currentLock = 1;
                }
            }

            // --- 阶段 2 (amplifier >= 30) ---
            if (amplifier >= 30) {
                if (currentLock < 50) {
                    if (!playerInterface.getDarkMsg2Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark2").formatted(Formatting.DARK_PURPLE));
                            giveVulnerableEffect(entity);
                        }
                        playerInterface.setDarkMsg2Sent(true);
                    }
                    playerInterface.setDark(50);
                    currentLock = 50;
                }
            }

            // --- 阶段 3 (amplifier >= 70) ---
            if (amplifier >= 70) {
                if (currentLock < 100) {
                    if (!playerInterface.getDarkMsg3Sent()) {
                        if (serverWorld != null) {
                            entity.sendMessage(Text.translatable("dark3").formatted(Formatting.RED));
                            giveFrenzyEffect(entity);
                        }
                        playerInterface.setDarkMsg3Sent(true);
                    }
                    playerInterface.setDark(100);
                    currentLock = 100;
                }
            }

            // --- 阶段 4: 等级 >= 100 (致死阶段) ---
            if (amplifier >= 100) {

                // 检查不死图腾
                boolean hasTotem = entity.isHolding(Items.TOTEM_OF_UNDYING);

                if (hasTotem) {
                    // 1. 如果持有图腾：移除图腾物品，取消伤害，移除效果并重置状态

                    // 确保只触发一次消息和移除逻辑
                    if (currentLock < 101) {

                        if (!playerInterface.getDarkMsg4Sent()) {
                            if (serverWorld != null) {
                                // 发送特殊消息 (使用新的翻译键，表示图腾被移除)
                                entity.sendMessage(Text.translatable("dark4_shadow").formatted(Formatting.DARK_RED));
                            }
                            playerInterface.setDarkMsg4Sent(true);
                        }

                        // 🎯 核心逻辑: 移除图腾物品 (防止原版消耗)
                        // 检查主手
                        if (entity.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            entity.getMainHandStack().decrement(1);
                        }
                        // 检查副手
                        else if (entity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            entity.getOffHandStack().decrement(1);
                        }

                        // 🎯 取消伤害: 此时不调用 entity.damage()，图腾不会被原版逻辑消耗。

                        // 🎯 移除自身效果
                        removeEffect(entity, "dark");

                        // 重置 Dark 锁和消息发送状态
                        playerInterface.setDark(0);
                        playerInterface.setDarkMsg1Sent(false);
                        playerInterface.setDarkMsg2Sent(false);
                        playerInterface.setDarkMsg3Sent(false);
                        playerInterface.setDarkMsg4Sent(false);
                    }

                } else {
                    // 2. 否则（没有图腾）：直接致死

                    // 确保只触发一次致死逻辑
                    if (currentLock < 101) {
                        Text message = Text.translatable("dark4").formatted(Formatting.GOLD);

                        if (!playerInterface.getDarkMsg4Sent()) {
                            if (serverWorld != null) {
                                entity.sendMessage(message);
                            }
                            playerInterface.setDarkMsg4Sent(true);
                        }

                        // 造成致死伤害
                        currentDarkDamageEntry.ifPresent(damageEntry -> {
                            DamageSource damageSource = new DamageSource(damageEntry);
                            entity.damage(damageSource, Float.MAX_VALUE);
                        });

                        // 移除自身效果
                        removeEffect(entity, "dark");

                        playerInterface.setDark(101); // 设置最高锁 (防止死亡后逻辑重复触发)
                    }
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