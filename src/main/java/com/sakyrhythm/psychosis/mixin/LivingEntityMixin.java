package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.ILivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs; // New import
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args; // New import

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ILivingEntity {
    @Unique
    @Mutable
    public boolean cbhurt;

    @Unique
    @Mutable
    public int timeUntilRegen;

    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void removeInvincibilityFrames(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cbhurt)this.timeUntilRegen = 0;
        else this.timeUntilRegen = 20;
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamageAmountCombined(float originalAmount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        float finalDamage = originalAmount;

        RegistryEntry<StatusEffect> vulnerableEffectEntry = livingEntity.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")))
                .orElse(null);

        if (vulnerableEffectEntry != null && livingEntity.hasStatusEffect(vulnerableEffectEntry)) {
            finalDamage *= 2.0f;
        }

        if (source.getAttacker() instanceof LivingEntity attacker) {
            RegistryEntry<StatusEffect> frenzyEffectEntry = livingEntity.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "frenzy")))
                    .orElse(null);

            if (frenzyEffectEntry != null && attacker.hasStatusEffect(frenzyEffectEntry)) {
                finalDamage = originalAmount * 1.70f;
                float selfDamage = originalAmount * 0.30f;

                RegistryEntry.Reference<DamageType> frenzyDamageTypeEntry = livingEntity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.FRENZY_DAMAGE)
                        .orElse(null);

                if (selfDamage > 0.0f && frenzyDamageTypeEntry != null) {
                    DamageSource selfDamageSource = new DamageSource(frenzyDamageTypeEntry);
                    attacker.damage(selfDamageSource, selfDamage);
                }
            }
        }

        return finalDamage;
    }

    @ModifyArgs(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V")
    )
    private void modifyKnockbackArgs(Args args, DamageSource source, float amount) {
        if ((Psychosis.FRENZY_DAMAGE != null && source.isOf(Psychosis.FRENZY_DAMAGE)) ||
                (Psychosis.DARK_DAMAGE != null && source.isOf(Psychosis.DARK_DAMAGE)) ||
                (Psychosis.SHADOW_DAMAGE != null && source.isOf(Psychosis.SHADOW_DAMAGE))) {
            args.set(0, 0.0); // power
            args.set(1, 0.0); // x-direction
            args.set(2, 0.0); // z-direction
        }
    }
}