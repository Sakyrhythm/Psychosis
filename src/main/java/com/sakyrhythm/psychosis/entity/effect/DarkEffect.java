package com.sakyrhythm.psychosis.entity.effect;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
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

public class DarkEffect  extends StatusEffect{
    private RegistryEntry.Reference<DamageType> darkDamageEntry; // Stored here

    Boolean dark = true;
    public DarkEffect() {
        super(
                StatusEffectCategory.HARMFUL,
                0x45283C);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
    public void giveVulnerableEffect(LivingEntity entity){
        RegistryEntry<StatusEffect> darkEffectEntry = entity.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")))
                .orElse(null);
        int effectDuration = StatusEffectInstance.INFINITE;
        entity.addStatusEffect(new StatusEffectInstance(
                darkEffectEntry, // Pass the RegistryEntry
                effectDuration,
                0,
                false,
                true,
                true
        ));
    }
    public void giveFrenzyEffect(LivingEntity entity){
        RegistryEntry<StatusEffect> darkEffectEntry = entity.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "frenzy")))
                .orElse(null);
        int effectDuration = StatusEffectInstance.INFINITE;

        // Use darkEffectEntry in the StatusEffectInstance constructor
        entity.addStatusEffect(new StatusEffectInstance(
                darkEffectEntry, // Pass the RegistryEntry
                effectDuration,
                0,
                false,
                true,
                true
        ));
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!(entity instanceof IPlayerEntity playerInterface)) {
            return false;
        }

        if (amplifier >= 100) {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(true, serverWorld, Text.translatable("dark4").formatted(Formatting.GOLD));
            }
            if (darkDamageEntry == null) {
                darkDamageEntry = entity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.DARK_DAMAGE)
                        .orElse(null);
            }
            if (darkDamageEntry != null) {
                DamageSource damageSource = new DamageSource(darkDamageEntry);
                entity.damage(damageSource, Float.MAX_VALUE);
            }
        } else if (amplifier == 71) {
            dark = true;
        } else if (amplifier > 70) {
            playerInterface.setDark(100);
        } else if (amplifier == 70) {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld, Text.translatable("dark3").formatted(Formatting.RED));
                giveFrenzyEffect(entity);
            }
            dark = false;
        } else if (amplifier == 31) {
            dark = true;
        } else if (amplifier > 30 && amplifier < 70) {
            playerInterface.setDark(50);
        } else if (amplifier == 30) {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld, Text.translatable("dark2").formatted(Formatting.DARK_PURPLE));
                giveVulnerableEffect(entity);
            }
            dark = false;
        } else if (amplifier == 1) {
            dark = true;
        } else if (amplifier == 0) {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld, Text.translatable("dark1").formatted(Formatting.DARK_GRAY));
                playerInterface.setDark(0);
            }
            dark = false;
        } else if (amplifier < 70 && amplifier != 30) {
            playerInterface.setDark(0);
        }

        return super.applyUpdateEffect(entity, amplifier);
    }
}