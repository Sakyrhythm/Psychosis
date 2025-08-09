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

import static javax.swing.UIManager.getString;

public class DarkEffect  extends StatusEffect{
    private RegistryEntry.Reference<DamageType> darkDamageEntry; // Stored here

    Boolean dark = true;
    public DarkEffect() {
        super(
                StatusEffectCategory.HARMFUL, // 药水效果是有益的还是有害的
                0x45283C); // 显示的颜色
    }

    // 这个方法在每个 tick 都会调用，以检查是否应应用药水效果
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // 在我们的例子中，为了确保每一 tick 药水效果都会被应用，我们只要这个方法返回 true 就行了。
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

    // 这个方法在应用药水效果时会被调用，所以我们可以在这里实现自定义功能。
    @Override
    public boolean applyUpdateEffect( LivingEntity entity, int amplifier) {
        if (amplifier==0){
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld, Text.translatable("dark1").formatted(Formatting.DARK_GRAY));
                IPlayerEntity playerInterface = (IPlayerEntity) entity;
                playerInterface.setDark(0);
            }
            dark=false;
        }
        if (amplifier==1){
            dark=true;
        }
        if (amplifier==30){
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld, Text.translatable("dark2").formatted(Formatting.DARK_PURPLE));
                giveVulnerableEffect(entity);
            }
            dark=false;
        }
        if (amplifier==31){
            dark=true;
        }
        if (amplifier > 30 && entity instanceof IPlayerEntity) {
            IPlayerEntity playerInterface = (IPlayerEntity) entity;
            playerInterface.setDark(50);
        }
        if (amplifier==70){
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld,Text.translatable("dark3").formatted(Formatting.RED));
                giveFrenzyEffect(entity);
            }
            dark=false;
        }
        if (amplifier==71){
            dark=true;
        }
        if (amplifier > 70 && entity instanceof IPlayerEntity) {
            IPlayerEntity playerInterface = (IPlayerEntity) entity;
            playerInterface.setDark(100);
        }
        if (amplifier < 70 && entity instanceof IPlayerEntity) {
            IPlayerEntity playerInterface = (IPlayerEntity) entity;
            playerInterface.setDark(0);
        }
        if (amplifier>=100) {
            if (darkDamageEntry == null) { // Only try to get it if it's not set yet
                darkDamageEntry = entity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.DARK_DAMAGE)
                        .orElse(null);
            }
            DamageSource damageSource = new DamageSource(darkDamageEntry);
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                PlayerEntity.sendMessageToAllPlayers(dark, serverWorld, Text.translatable("dark4").formatted(Formatting.GOLD));
            }
            entity.damage(damageSource, Float.MAX_VALUE);
        }

        return super.applyUpdateEffect(entity, amplifier);
    }
}