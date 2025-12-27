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
        return true;
    }
    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        super.onApplied(entity, amplifier);
        if (entity instanceof ILivingEntity iEntity) {
            iEntity.psychosis_template_1_21$setCBHurt(true);
        }
    }
    private Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntryRef;
    private Optional<RegistryEntry.Reference<StatusEffect>> getStatusEffectEntry(LivingEntity entity, String id) {
        return entity.getWorld().getRegistryManager().get(RegistryKeys.STATUS_EFFECT).getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, id)));
    }
    private Optional<RegistryEntry.Reference<DamageType>> getDamageTypeEntry(LivingEntity entity) {
        return entity.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(Psychosis.DARK_DAMAGE);
    }
    public void giveVulnerableEffect(LivingEntity entity) {
        getStatusEffectEntry(entity, "vulnerable").ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }
    public void giveFrenzyEffect(LivingEntity entity) {
        getStatusEffectEntry(entity, "frenzy").ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }
    public void removeEffect(LivingEntity entity, String id) {
        getStatusEffectEntry(entity, id).ifPresent(entity::removeStatusEffect);
    }
    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getHealth() <= 0.0F) {
            return super.applyUpdateEffect(entity, amplifier);
        }
        Optional<RegistryEntry.Reference<DamageType>> currentDarkDamageEntry = getDamageTypeEntry(entity);
        if (entity instanceof IPlayerEntity playerInterface) {
            ServerWorld serverWorld = entity.getWorld() instanceof ServerWorld ? (ServerWorld) entity.getWorld() : null;
            int currentLock = playerInterface.getDark();
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
            if (amplifier >= 100) {
                boolean hasTotem = entity.isHolding(Items.TOTEM_OF_UNDYING);
                if (hasTotem) {
                    if (currentLock < 101) {
                        if (!playerInterface.getDarkMsg4Sent()) {
                            if (serverWorld != null) {
                                entity.sendMessage(Text.translatable("dark4_shadow").formatted(Formatting.DARK_RED));
                            }
                            playerInterface.setDarkMsg4Sent(true);
                        }
                        if (entity.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            entity.getMainHandStack().decrement(1);
                        } else if (entity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            entity.getOffHandStack().decrement(1);
                        }
                        removeEffect(entity, "dark");
                        playerInterface.setDark(0);
                        playerInterface.setDarkMsg1Sent(false);
                        playerInterface.setDarkMsg2Sent(false);
                        playerInterface.setDarkMsg3Sent(false);
                        playerInterface.setDarkMsg4Sent(false);
                    }
                } else {
                    if (currentLock < 101) {
                        Text message = Text.translatable("dark4").formatted(Formatting.GOLD);
                        if (!playerInterface.getDarkMsg4Sent()) {
                            if (serverWorld != null) {
                                entity.sendMessage(message);
                            }
                            playerInterface.setDarkMsg4Sent(true);
                        }
                        currentDarkDamageEntry.ifPresent(damageEntry -> {
                            DamageSource damageSource = new DamageSource(damageEntry);
                            entity.damage(damageSource, Float.MAX_VALUE);
                        });
                        removeEffect(entity, "dark");
                        playerInterface.setDark(101);
                    }
                }
            }
        } else {
            if (!entity.getWorld().isClient()) {
                long currentTick = entity.getWorld().getTime();
                if (entity instanceof ILivingEntity cooldownEntity) {
                    long lastDamageTime = cooldownEntity.psychopomp$getLastDarkDamageTime();
                    if (currentTick - lastDamageTime >= DAMAGE_INTERVAL_TICKS) {
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