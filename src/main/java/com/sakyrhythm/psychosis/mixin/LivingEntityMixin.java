package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.ILivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ILivingEntity {
    @Shadow public abstract float getMaxHealth();
    @Shadow public abstract float getAbsorptionAmount();
    @Shadow public abstract float getHealth();
    @Shadow public abstract void setHealth(float health);
    @Shadow @Nullable public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);

    @Unique
    private static RegistryEntry<StatusEffect> darkEffectEntryCache = null;
    @Unique
    @Nullable
    private LivingEntity lastAttacker = null;

    @Unique
    private RegistryEntry.Reference<DamageType> shadowDamageEntry;
    @Unique
    private RegistryEntry.Reference<DamageType> darkDamageEntry;

    // ... (接口实现和 Shadow 字段不变) ...

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

    @Override
    public long psychopomp$getLastDarkDamageTime() {
        return this.psychopomp$lastDarkDamageTime;
    }

    @Override
    public void psychopomp$setLastDarkDamageTime(long ticks) {
        this.psychopomp$lastDarkDamageTime = ticks;
    }

    // --- 新增：限制抗性提升等级的 Mixin ---
    // 拦截 addStatusEffect 方法，修改传入的 StatusEffectInstance
    @ModifyVariable(
            method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private StatusEffectInstance limitResistanceLevel(StatusEffectInstance effectInstance) {
        // 目标限制等级：Resistance III (Amplifier = 2)
        final int MAX_RESISTANCE_AMPLIFIER = 2;

        // 检查施加的效果是否为 Resistance
        if (effectInstance.getEffectType().matches(StatusEffects.RESISTANCE)) {

            int currentAmplifier = effectInstance.getAmplifier();

            if (currentAmplifier > MAX_RESISTANCE_AMPLIFIER) {

                // 如果等级高于限制，则创建一个新的 StatusEffectInstance，等级降为 MAX_RESISTANCE_AMPLIFIER
                Psychosis.LOGGER.debug("Capping Resistance Effect from Amplifier {} to {}.", currentAmplifier, MAX_RESISTANCE_AMPLIFIER);

                // 返回一个新的 StatusEffectInstance，等级被限制
                return new StatusEffectInstance(
                        effectInstance.getEffectType(),
                        effectInstance.getDuration(),
                        MAX_RESISTANCE_AMPLIFIER,
                        effectInstance.isAmbient(),
                        effectInstance.shouldShowParticles(),
                        effectInstance.shouldShowIcon()
                );
            }
        }

        // 否则返回原始效果实例
        return effectInstance;
    }
    // --- 限制抗性提升等级的 Mixin 结束 ---


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
            this.psychosis_template_1_21$setLastAttacker(attacker);
        }
    }
    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"))
    private void forceClearRegenTime(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 1. 获取 ILivingEntity 接口实例
        if (entity instanceof ILivingEntity iLivingEntity) {

            // 2. 检查是否有 Divine 效果（防止误伤）
            RegistryEntry<StatusEffect> divineEffectEntry = entity.getWorld()
                    .getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                    .orElse(null);

            boolean hasDivineEffect = (divineEffectEntry != null && entity.hasStatusEffect(divineEffectEntry));

            // 3. 执行判断逻辑
            if (iLivingEntity.psychosis_template_1_21$getCBHurt() && !hasDivineEffect) {
                // 如果满足条件（有 cbhurt 且没有 Divine），则在伤害计算前强制将无敌帧设为 0
                // 这样即使原版逻辑依赖 timeUntilRegen，也会被忽略
                entity.timeUntilRegen = 0;

                // 提示：你可能需要确保 damage 内部的逻辑不会因为 timeUntilRegen=0 而提前退出
                // 幸运的是，LivingEntity#damage 方法通常是在内部判断 timeUntilRegen > 0 才跳过伤害的。
                // 强制设置为 0 应该能确保伤害处理继续。
            }
        }
    }

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
        long darktime = Math.max(0, psychopomp$getLastDarkDamageTime() - 1);
        psychopomp$setLastDarkDamageTime(darktime);

        LivingEntity player = (LivingEntity) (Object) this;
        RegistryEntry<StatusEffect> darkEffectEntry = player.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark")))
                .orElse(null);
        RegistryEntry<StatusEffect> divineEffectEntry = player.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "divine")))
                .orElse(null);

        if (darkEffectEntry != null && !player.hasStatusEffect(darkEffectEntry)) {
            ILivingEntity playerInterface = (ILivingEntity) player;
            playerInterface.psychosis_template_1_21$setCBHurt(false);
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamageAmountCombined(float originalAmount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        float currentDamage = originalAmount; // 使用 currentDamage 追踪伤害，最后它就是 finalDamage

        // --- 1. 标准效果减免/增强 (在 DivineEffect 之前计算) ---
        // 1.1. 抗性提升 (RESISTANCE) 减免
        StatusEffectInstance resistanceInstance = livingEntity.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistanceInstance != null) {
            int amplifier = resistanceInstance.getAmplifier();
            // 注意：这里的 amplifier 是读取玩家身上的效果，它已经被 limitResistanceLevel 限制在了 III 级（如果成功）。
            float resistanceReduction = 0.2f * (float)(amplifier + 1);
            resistanceReduction = Math.min(resistanceReduction, 1.0f);
            currentDamage *= (1.0f - resistanceReduction);
        }

        // 1.2. 自定义 Vulnerable 增强
        RegistryEntry<StatusEffect> vulnerableEffectEntry = livingEntity.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")))
                .orElse(null);

        if (vulnerableEffectEntry != null && livingEntity.hasStatusEffect(vulnerableEffectEntry)) {
            currentDamage *= 2.0f;
        }

        // 1.3. 自定义 Frenzy 增强 (并对攻击者造成反伤)
        if (source.getAttacker() instanceof LivingEntity attacker) {
            RegistryEntry<StatusEffect> frenzyEffectEntry = livingEntity.getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "frenzy")))
                    .orElse(null);

            if (frenzyEffectEntry != null && attacker.hasStatusEffect(frenzyEffectEntry)) {
                float damageBeforeFrenzy = originalAmount;
                currentDamage = damageBeforeFrenzy * 1.70f;
                float selfDamage = damageBeforeFrenzy * 0.30f;

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

        // --- 2. DivineEffect 伤害减免和限伤逻辑 ---
        RegistryEntry<StatusEffect> divineEffectEntry = livingEntity.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                .orElse(null);

        if (divineEffectEntry != null && livingEntity.hasStatusEffect(divineEffectEntry)) {
            // ... (DivineEffect 逻辑保持不变) ...

            // 获取自定义伤害类型引用
            if (darkDamageEntry == null) {
                darkDamageEntry = livingEntity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.DARK_DAMAGE)
                        .orElse(null);
            }
            if (shadowDamageEntry == null) {
                shadowDamageEntry = livingEntity.getWorld().getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(Psychosis.SHADOW_DAMAGE)
                        .orElse(null);
            }

            // a. 魔法伤害减免 80%
            if ((darkDamageEntry != null && source.isOf(darkDamageEntry.registryKey())) ||
                    (shadowDamageEntry != null && source.isOf(shadowDamageEntry.registryKey())) ||
                    source.isOf(DamageTypes.MAGIC)) {

                currentDamage *= (1.0f - 0.80f);
            }

            // 记录未被上限限制的伤害
            float damageBeforeCap = currentDamage;

            // b. 伤害上限基数计算 (Max Health + Absorption)
            float maxHealth = livingEntity.getMaxHealth();
            float absorption = livingEntity.getAbsorptionAmount();

            // c. 伤害上限：总生命基数的 90%
            float damageCap = (maxHealth + absorption) * 0.90f;

            // d. 🎯 特效触发逻辑 (核心修正)
            float healthBeforeDamage = livingEntity.getHealth() + absorption;

            // 检查 1：限制前的伤害是否会致命
            boolean willDieWithoutCap = (healthBeforeDamage - damageBeforeCap <= 0.001f);

            // 检查 2：限制后的伤害是否不再致命
            boolean survivesWithCap = (healthBeforeDamage - damageCap > 0.001f);

            // 特效触发条件：限制前会死 AND 限制后能活 (即限伤成功救命)
            if (willDieWithoutCap && survivesWithCap) {

                // 执行“神救一命”的特效和提示
                if (!livingEntity.getWorld().isClient) {
                    ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
                    BlockPos playerPos = livingEntity.getBlockPos();

                    // 特效：闪电、音效
                    LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                    lightning.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
                    lightning.setCosmetic(true);
                    serverWorld.spawnEntity(lightning);

                    serverWorld.playSound(
                            null, playerPos, net.minecraft.sound.SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                            net.minecraft.sound.SoundCategory.WEATHER, 0.2F, 0.8F + serverWorld.random.nextFloat() * 0.2F
                    );
                    serverWorld.playSound(
                            null, livingEntity.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_GLASS_BREAK,
                            net.minecraft.sound.SoundCategory.PLAYERS, 4.0F, 0.9F + serverWorld.random.nextFloat() * 0.2F
                    );
                }

                if (livingEntity instanceof ServerPlayerEntity serverPlayer) {
                    Text titleText = Text.translatable("title.psychosis.divine_cap_title").formatted(Formatting.RED);
                    Text subtitleText = Text.translatable("title.psychosis.divine_cap_subtitle").formatted(Formatting.GOLD);

                    serverPlayer.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 40, 10));
                    serverPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(subtitleText));
                    serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(titleText));
                }

                // 应用一系列状态效果
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false, false));
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 2, false, false, false));
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 0, false, false, true));

                // 移除 Dark 效果
                RegistryEntry<StatusEffect> darkEffectEntryCache = livingEntity.getWorld().getRegistryManager()
                        .get(RegistryKeys.STATUS_EFFECT)
                        .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark")))
                        .orElse(null);
                livingEntity.removeStatusEffect(darkEffectEntryCache);

                livingEntity.sendMessage(Text.translatable("limit_damage_save").formatted(Formatting.GOLD));
            }

            // e. 最终将伤害限制在 damageCap
            currentDamage = Math.min(damageBeforeCap, damageCap);
        }

        // 返回最终的伤害值 (可能是原值，也可能是限制后的值)
        return currentDamage;
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