package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.ILivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ILivingEntity {
    @Unique
    @Nullable
    private LivingEntity lastAttacker = null;

    @Unique
    private RegistryEntry.Reference<DamageType> shadowDamageEntry;
    @Unique
    private RegistryEntry.Reference<DamageType> darkDamageEntry;
    @Override
    public @Nullable LivingEntity psychosis_template_1_21$getLastAttacker() {
        return this.lastAttacker;
    }

    @Override
    public void psychosis_template_1_21$setLastAttacker(@Nullable LivingEntity attacker) {
        this.lastAttacker = attacker;
    }
    @Unique
    @Mutable
    public boolean cbhurt=false;

    @Override
    public boolean psychosis_template_1_21$getCBHurt() { return this.cbhurt; }

    @Override
    public void psychosis_template_1_21$setCBHurt(boolean value) { this.cbhurt = value; }
    @Unique
    private long psychopomp$lastDarkDamageTime = 0;

    // 2. 实现接口方法：获取时间戳
    @Override
    public long psychopomp$getLastDarkDamageTime() {
        return this.psychopomp$lastDarkDamageTime;
    }

    // 3. 实现接口方法：设置时间戳
    @Override
    public void psychopomp$setLastDarkDamageTime(long ticks) {
        this.psychopomp$lastDarkDamageTime = ticks;
    }

    @Unique
    @Mutable
    public int timeUntilRegen;

    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setAttacker(Lnet/minecraft/entity/LivingEntity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void setLastAttacker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            // 保存当前的攻击者
            this.psychosis_template_1_21$setLastAttacker(attacker);
        }
    }

    // --- 🎯 修改后的 modifyDamageCooldownSet：免疫黑暗纠缠无敌帧删除 ---
    @Redirect(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/LivingEntity;timeUntilRegen:I",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD,
                    ordinal = 0
            )
    )
    private void modifyDamageCooldownSet(LivingEntity entity, int value) {
        if (entity instanceof ILivingEntity iLivingEntity) {

            // 检查是否有 DivineEffect
            RegistryEntry<StatusEffect> divineEffectEntry = entity.getWorld()
                    .getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                    .orElse(null);

            boolean hasDivineEffect = (divineEffectEntry != null && entity.hasStatusEffect(divineEffectEntry));
            if (iLivingEntity.psychosis_template_1_21$getCBHurt() && !hasDivineEffect) {
                entity.timeUntilRegen = 0;
            } else if(hasDivineEffect) {
                entity.timeUntilRegen = 40;
            }
            else {
                entity.timeUntilRegen = value;
            }
        } else {
            entity.timeUntilRegen = value;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        long darktime = psychopomp$getLastDarkDamageTime();
        psychopomp$setLastDarkDamageTime(darktime-1);
        LivingEntity player = (LivingEntity) (Object) this;
        RegistryEntry<StatusEffect> darkEffectEntry = player.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark")))
                .orElse(null);

        if (darkEffectEntry != null && !player.hasStatusEffect(darkEffectEntry)) {
            ILivingEntity playerInterface = (ILivingEntity) player;
            playerInterface.psychosis_template_1_21$setCBHurt(false);
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamageAmountCombined(float originalAmount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        float finalDamage = originalAmount;

        // 1. 其他效果的原始逻辑 (vulnerable, frenzy)
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

        // 2. DivineEffect 伤害减免和伤害上限逻辑
        RegistryEntry<StatusEffect> divineEffectEntry = livingEntity.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                .orElse(null);

        if (divineEffectEntry != null && livingEntity.hasStatusEffect(divineEffectEntry)) {
            if (darkDamageEntry == null) {
                darkDamageEntry = livingEntity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.DARK_DAMAGE)
                        .orElse(null); // .orElse(null) works here because darkDamageEntry is RegistryEntry.Reference
                if (darkDamageEntry == null) {
                    return finalDamage;
                }
            }
            if (shadowDamageEntry == null) {
                shadowDamageEntry = livingEntity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.SHADOW_DAMAGE)
                        .orElse(null); // .orElse(null) works here because darkDamageEntry is RegistryEntry.Reference
                if (shadowDamageEntry == null) {
                    return finalDamage;
                }
            }
            // a. 魔法伤害减免 80%
            if (source.isOf(DamageTypes.MAGIC)||source.isOf(darkDamageEntry.registryKey())||source.isOf(shadowDamageEntry.registryKey())) {

                finalDamage *= (1.0f - 0.80f); // 20% 伤害
            }

            // b. 伤害上限：90% 最大生命值
            float maxHealth = livingEntity.getMaxHealth();
            float damageCap = maxHealth * 0.90f;

            // 最终伤害不超过伤害上限
            finalDamage = Math.min(finalDamage, damageCap);
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