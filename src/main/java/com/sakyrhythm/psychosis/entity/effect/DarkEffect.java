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
    private Optional<RegistryEntry.Reference<DamageType>> darkDamageEntry = Optional.empty();
    private Optional<RegistryEntry.Reference<StatusEffect>> vulnerableEffectEntry = Optional.empty();
    private Optional<RegistryEntry.Reference<StatusEffect>> frenzyEffectEntry = Optional.empty();

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
        if (entity instanceof IPlayerEntity playerInterface) {
            playerInterface.setDark(0);
        }
    }

    private void initializeEntries(LivingEntity entity) {
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
        vulnerableEffectEntry.ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }

    public void giveFrenzyEffect(LivingEntity entity) {
        frenzyEffectEntry.ifPresent(entry -> entity.addStatusEffect(new StatusEffectInstance(entry, StatusEffectInstance.INFINITE, 0, false, true, true)));
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        initializeEntries(entity);
        if (entity instanceof IPlayerEntity playerInterface) {

            int currentLock = playerInterface.getDark();
            if (amplifier >= 0 && currentLock < 1) {
                if (entity.getWorld() instanceof ServerWorld) {
                    entity.sendMessage( Text.translatable("dark1").formatted(Formatting.DARK_GRAY));
                }
                playerInterface.setDark(1);
                currentLock = 1;
            }
            if (amplifier >= 29 && currentLock < 50) {
                if (entity.getWorld() instanceof ServerWorld) {
                    entity.sendMessage( Text.translatable("dark2").formatted(Formatting.DARK_PURPLE));
                    giveVulnerableEffect(entity);
                }
                playerInterface.setDark(50);
                currentLock = 50;
            }
            if (amplifier >= 69 && currentLock < 100) {
                if (entity.getWorld() instanceof ServerWorld) {
                    entity.sendMessage( Text.translatable("dark3").formatted(Formatting.RED));
                    giveFrenzyEffect(entity);
                }
                playerInterface.setDark(100);
            }
            if (amplifier >= 100) {
                if (entity.getWorld() instanceof ServerWorld) {
                    entity.sendMessage( Text.translatable("dark4").formatted(Formatting.GOLD));
                }
                darkDamageEntry.ifPresent(damageEntry -> {
                    DamageSource damageSource = new DamageSource(damageEntry);
                    entity.damage(damageSource, Float.MAX_VALUE);
                });
                playerInterface.setDark(101);
            }
            else if (amplifier > 69) {
                playerInterface.setDark(100);
            }
            else if (amplifier > 29) {
                playerInterface.setDark(50);
            }
            else if (amplifier > 0) {
                playerInterface.setDark(1);
            }
            else if (amplifier < 0) {
                playerInterface.setDark(0);
            }

        }
        else {
            if (!entity.getWorld().isClient()) {
                long currentTick = entity.getWorld().getTime();

                if (entity instanceof ILivingEntity cooldownEntity) {
                    long lastDamageTime = cooldownEntity.psychopomp$getLastDarkDamageTime();

                    if (currentTick - lastDamageTime >= DAMAGE_INTERVAL_TICKS) {

                        darkDamageEntry.ifPresent(damageEntry -> {

                            float damageAmount = (float) Math.pow(2, amplifier);

                            LivingEntity attacker = cooldownEntity.psychosis_template_1_21$getLastAttacker();

                            DamageSource damageSource = new DamageSource(damageEntry, attacker);
                            entity.damage(damageSource, damageAmount);

                            cooldownEntity.psychopomp$setLastDarkDamageTime(currentTick);
                        });
                    }
                }
            }
        }

        return super.applyUpdateEffect(entity, amplifier);
    }
}