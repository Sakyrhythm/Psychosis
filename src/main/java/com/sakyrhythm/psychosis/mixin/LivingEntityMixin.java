package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.config.ModConfig;
import com.sakyrhythm.psychosis.entity.custom.DarkGodEntity;
import com.sakyrhythm.psychosis.entity.custom.GoddessEntity;
import com.sakyrhythm.psychosis.interfaces.ILivingEntity;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
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
    @Shadow
    protected abstract float applyArmorToDamage(DamageSource source, float amount);

    @Unique
    private static RegistryEntry<StatusEffect> darkEffectEntryCache = null;
    @Unique
    @Nullable
    private LivingEntity lastAttacker = null;

    @Unique
    private RegistryEntry.Reference<DamageType> shadowDamageEntry;
    @Unique
    private RegistryEntry.Reference<DamageType> darkDamageEntry;

    // --- DarkGod 专有常量 ---
    @Unique
    private static final float BOSS_MAX_DAMAGE_PERCENT_NORMAL = 0.20F; // 20% 正常伤害上限
    @Unique
    private static final float BOSS_MAX_DAMAGE_PERCENT_DIVINE = 0.50F; // 50% divine效果伤害上限
    @Unique
    private static final float BOSS_PHASE_TWO_THRESHOLD = 0.50F; // 50% 血量切换阶段/惩罚触发点
    @Unique
    private static final Identifier BOSS_DIVINE_EFFECT_ID = Identifier.of("psychosis", "divine");
    @Unique
    private static final float EPSILON = 0.001F; // 浮点数容差
    @Unique
    private static final float GODDESS_MAX_DAMAGE_PERCENT_NORMAL = 0.01F; // Goddess 基础限伤 1%
    @Unique
    private static final float GODDESS_MAX_DAMAGE_PERCENT_DIVINE = 0.30F; // Goddess Divine 限伤 30%

    @Unique
    private static final Identifier RAIN_SLOWNESS_ID = Identifier.of("psychosis", "rain_slowness");
    @Unique
    private static final EntityAttributeModifier RAIN_SLOWNESS_MODIFIER = new EntityAttributeModifier(
            RAIN_SLOWNESS_ID,
            -0.15, // 减速 15%，数值可调 (Slowness I 约等于 -0.15)
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
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

    // --- 限制抗性提升等级的 Mixin ---
    @ModifyVariable(
            method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private StatusEffectInstance limitResistanceLevel(StatusEffectInstance effectInstance) {
        final int MAX_RESISTANCE_AMPLIFIER = 2;

        if (effectInstance.getEffectType().matches(StatusEffects.RESISTANCE)) {
            int currentAmplifier = effectInstance.getAmplifier();
            if (currentAmplifier > MAX_RESISTANCE_AMPLIFIER) {
                Psychosis.LOGGER.debug("Capping Resistance Effect from Amplifier {} to {}.", currentAmplifier, MAX_RESISTANCE_AMPLIFIER);
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
        return effectInstance;
    }
    // --- 限制抗性提升等级的 Mixin 结束 ---

    // *** NEW: 在 damage 方法的最开始检查 DarkGodEntity 的出场状态，并阻止伤害 ***
    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelDamageIfDarkGodAppearing(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof DarkGodEntity darkGod) {

            if (darkGod.isAppearing()) {

                if (!entity.getWorld().isClient()) {
                    Psychosis.LOGGER.debug("DarkGod Cap: Damage blocked by Mixin during appearance.");
                }

                // 强制生命值保护，防止某些 Mixin 钩子在 damage 内部绕过
                if (darkGod.getHealth() < darkGod.getMaxHealth()) {
                    darkGod.setHealth(darkGod.getMaxHealth());
                }

                cir.setReturnValue(false); // 设置返回值为 false
                cir.cancel();             // 取消原方法的执行
            }
        }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void psychosis$applyRainSlowness(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        World world = entity.getWorld();
        if (world.isClient) return;

        EntityAttributeInstance speedAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr == null) return;

        // 判定条件
        boolean shouldSlow = false;
        if (ModConfig.enableRainSlowness && world.isRaining()) {
            BlockPos pos = entity.getBlockPos();
            boolean isExposedInRain = world.isSkyVisible(pos) &&
                    world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() &&
                    world.getBiome(pos).value().getPrecipitation(pos) == Biome.Precipitation.RAIN;

            boolean hasUmbrella = entity.getMainHandStack().isOf(ModItems.UMBRELLA)
                    || entity.getOffHandStack().isOf(ModItems.UMBRELLA);

            if (isExposedInRain && !hasUmbrella) {
                shouldSlow = true;
            }
        }

        // 应用或移除修改器
        if (shouldSlow) {
            if (!speedAttr.hasModifier(RAIN_SLOWNESS_ID)) {
                speedAttr.addTemporaryModifier(RAIN_SLOWNESS_MODIFIER);
                // 可选：给玩家发送一条非常隐晦的消息，或者完全保持安静
                // entity.sendMessage(Text.literal("雨水打湿了你的衣服...").formatted(Formatting.GRAY), true);
            }
        } else {
            if (speedAttr.hasModifier(RAIN_SLOWNESS_ID)) {
                speedAttr.removeModifier(RAIN_SLOWNESS_ID);
            }
        }
    }


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

        if (entity instanceof ILivingEntity iLivingEntity) {
            RegistryEntry<StatusEffect> divineEffectEntry = entity.getWorld()
                    .getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                    .orElse(null);

            boolean hasDivineEffect = (divineEffectEntry != null && entity.hasStatusEffect(divineEffectEntry));

            if (iLivingEntity.psychosis_template_1_21$getCBHurt() && !hasDivineEffect) {
                entity.timeUntilRegen = 0;
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

        if (darkEffectEntry != null && !player.hasStatusEffect(darkEffectEntry)) {
            ILivingEntity playerInterface = (ILivingEntity) player;
            playerInterface.psychosis_template_1_21$setCBHurt(false);
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamageAmountCombined(float originalAmount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        float currentDamage = originalAmount;

        // --- 1. 标准效果减免/增强 (玩家自身或通用 LivingEntity 减免) ---

        // 1.1. 抗性提升 (RESISTANCE) 减免
        StatusEffectInstance resistanceInstance = livingEntity.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistanceInstance != null) {
            int amplifier = resistanceInstance.getAmplifier();
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
                currentDamage = originalAmount * 1.70f;
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

        // --- 2. DivineEffect 伤害减免和限伤逻辑 (玩家/通用保命机制) ---
        RegistryEntry<StatusEffect> divineEffectEntry = livingEntity.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                .orElse(null);

        // --- DivineEffect 逻辑 (50% 基础减伤 + 80% 被动限伤) ---
        if (divineEffectEntry != null && livingEntity.hasStatusEffect(divineEffectEntry)) {

            // 1. 基础减伤：特定类型降低 50%
            boolean isMagic = source.isOf(net.minecraft.entity.damage.DamageTypes.MAGIC) ||
                    source.isOf(net.minecraft.entity.damage.DamageTypes.INDIRECT_MAGIC);

            boolean isDark = (darkDamageEntry != null && source.isOf(darkDamageEntry.registryKey()));
            boolean isShadow = (shadowDamageEntry != null && source.isOf(shadowDamageEntry.registryKey()));

            // 如果是其中一种，先进行 50% 基础减伤
            if (isMagic || isDark || isShadow) {
                currentDamage *= 0.50f;
            }
                // 3. 执行限伤：强制将伤害降至最大生命值的 80% (确保能留下一口气)
            float currentTotalHealth = livingEntity.getHealth() + livingEntity.getAbsorptionAmount();

            float postArmorDamage = ((LivingEntityMixin)(Object)this)
                    .applyArmorToDamage(source, currentDamage);
            float damageCap = livingEntity.getMaxHealth() * 0.80f;
            float cappedDamage = Math.min(postArmorDamage, damageCap);
            boolean originalFatal = postArmorDamage >= currentTotalHealth;
            boolean cappedFatal = cappedDamage >= currentTotalHealth;

            currentDamage = cappedDamage;

            if (originalFatal && !cappedFatal) {

                    if (!livingEntity.getWorld().isClient) {
                        ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
                        BlockPos playerPos = livingEntity.getBlockPos();

                        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                        lightning.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
                        lightning.setCosmetic(true);
                        serverWorld.spawnEntity(lightning);

                        serverWorld.playSound(
                                null, playerPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                                SoundCategory.WEATHER, 0.2F, 0.8F + serverWorld.random.nextFloat() * 0.2F
                        );

                        serverWorld.playSound(
                                null, playerPos, SoundEvents.BLOCK_GLASS_BREAK,
                                SoundCategory.PLAYERS, 4.0F, 0.9F + serverWorld.random.nextFloat() * 0.2F
                        );
                    }

                    if (livingEntity instanceof ServerPlayerEntity serverPlayer) {
                        Text titleText = Text.translatable("title.psychosis.divine_cap_title").formatted(Formatting.RED);
                        Text subtitleText = Text.translatable("title.psychosis.divine_cap_subtitle").formatted(Formatting.GOLD);

                        serverPlayer.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 40, 10));
                        serverPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(subtitleText));
                        serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(titleText));
                    }

                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false, false));
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 2, false, false, false));
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 0, false, false, true));

                    RegistryEntry<StatusEffect> darkEffectEntry = livingEntity.getWorld().getRegistryManager()
                            .get(RegistryKeys.STATUS_EFFECT)
                            .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark")))
                            .orElse(null);

                    if (darkEffectEntry != null) {
                        livingEntity.removeStatusEffect(darkEffectEntry);
                    }

                    livingEntity.sendMessage(Text.translatable("limit_damage_save").formatted(Formatting.GOLD));
                }
            }

        if (livingEntity instanceof GoddessEntity goddess) {

            float maxHealth = goddess.getMaxHealth();
            float damageBeforeBossCap = currentDamage;
            float dynamicMaxDamagePercent = GODDESS_MAX_DAMAGE_PERCENT_NORMAL; // 默认为 1%

            // 获取实际造成伤害的实体
            LivingEntity attacker = (source.getAttacker() instanceof LivingEntity) ? (LivingEntity) source.getAttacker() : null;

            // 检查攻击者是否是玩家，并且是否带有 divine 效果
            if (attacker instanceof PlayerEntity playerAttacker) {

                RegistryEntry<StatusEffect> bossDivineEffectEntry = goddess.getWorld().getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                        .getEntry(BOSS_DIVINE_EFFECT_ID)
                        .orElse(null);

                // 【核心修改】：如果攻击者有 divine 效果，则限伤提升至 30%
                if (bossDivineEffectEntry != null && playerAttacker.hasStatusEffect(bossDivineEffectEntry)) {
                    dynamicMaxDamagePercent = GODDESS_MAX_DAMAGE_PERCENT_DIVINE; // 0.30F
                    Psychosis.LOGGER.debug("Goddess Cap: Attacker ({}) has divine. Limit 30%.", playerAttacker.getName().getString());
                }
            }

            // 【应用限伤】
            final float maxDamage = maxHealth * dynamicMaxDamagePercent;
            currentDamage = Math.min(damageBeforeBossCap, maxDamage);

            if (currentDamage < damageBeforeBossCap - EPSILON) {
                Psychosis.LOGGER.debug("Goddess damage capped: Original {} -> Final {} ({}%)",
                        damageBeforeBossCap, currentDamage, dynamicMaxDamagePercent * 100);
            }

        } else if (livingEntity instanceof DarkGodEntity darkGod) {
            // DarkGodEntity 的原有逻辑 (保持不变)
            float maxHealth = darkGod.getMaxHealth();
            if (darkGod.getHealth() > maxHealth * BOSS_PHASE_TWO_THRESHOLD) {
                float dynamicMaxDamagePercent = BOSS_MAX_DAMAGE_PERCENT_NORMAL;
                LivingEntity attacker = (source.getAttacker() instanceof LivingEntity) ? (LivingEntity) source.getAttacker() : null;

                if (attacker instanceof PlayerEntity playerAttacker) {
                    RegistryEntry<StatusEffect> bossDivineEffectEntry = darkGod.getWorld().getRegistryManager().get(RegistryKeys.STATUS_EFFECT)
                            .getEntry(BOSS_DIVINE_EFFECT_ID)
                            .orElse(null);

                    if (bossDivineEffectEntry != null && playerAttacker.hasStatusEffect(bossDivineEffectEntry)) {
                        dynamicMaxDamagePercent = BOSS_MAX_DAMAGE_PERCENT_DIVINE; // 50%
                    }
                }

                final float maxDamage = maxHealth * dynamicMaxDamagePercent;
                currentDamage = Math.min(currentDamage, maxDamage);
            }
        }


        // 4. 返回最终的伤害值
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