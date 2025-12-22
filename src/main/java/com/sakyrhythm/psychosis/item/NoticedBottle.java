package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.PotionItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class NoticedBottle extends PotionItem {

    private static final RegistryKey<DamageType> SHADOW_DAMAGE_KEY = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );

    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );

    public NoticedBottle(Item.Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey());
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger(serverPlayerEntity, stack);
            serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
        }

        if (!world.isClient) {

            Integer integer = stack.getOrDefault(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, 0);
            RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
                    .getEntry(DARK_EFFECT_KEY)
                    .orElse(null);

            int darkEffectAmplifier = 0;

            if (darkEffectEntry != null) {
                StatusEffectInstance existingDarkEffect = user.getStatusEffect(darkEffectEntry);

                if (existingDarkEffect != null) {
                    darkEffectAmplifier = existingDarkEffect.getAmplifier() + 1;
                } else {
                    darkEffectAmplifier = 0;
                }
            }

            // --- 引入微妙模式（Subtle Mode）---
            // 如果新的等级 >= 100，则进入微妙模式，不触发雷电和伤害。
            boolean shouldBeSubtle = darkEffectAmplifier >= 100;

            // --- 雷电和雷声效果 (微妙模式下被跳过) ---
            if (!shouldBeSubtle && world instanceof ServerWorld serverWorld) {
                BlockPos playerPos = user.getBlockPos();
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                lightning.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
                lightning.setCosmetic(true);
                serverWorld.spawnEntity(lightning);
                serverWorld.playSound(
                        null, playerPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundCategory.WEATHER, 5.0F, 0.8F + world.random.nextFloat() * 0.2F
                );
            }

            // --- 辅助效果 (始终应用) ---
            if (user instanceof IPlayerEntity playerInterface) {
                playerInterface.setNoticed(true);
            }
            if (user instanceof PlayerEntity player) {
                player.getHungerManager().setFoodLevel(0);
                player.getHungerManager().setSaturationLevel(0);
                player.getHungerManager().setExhaustion(0);
            }

            // --- 伤害逻辑 (微妙模式下被跳过) ---
            if (!shouldBeSubtle) {
                float currentHealth = user.getMaxHealth();
                float currentAbsorption = user.getAbsorptionAmount();
                final float targetLife = 1.0F;
                float targetActualDamage = currentHealth + currentAbsorption - targetLife;
                if (targetActualDamage < 19.0F) {
                    targetActualDamage = 19.0F;
                }
                StatusEffectInstance resistanceEffect = user.getStatusEffect(StatusEffects.RESISTANCE);
                float damageMultiplier = 1.0F;
                if (resistanceEffect != null) {
                    int amplifier = resistanceEffect.getAmplifier();
                    amplifier = Math.min(2, Math.max(0, amplifier));
                    float reductionFactor = switch (amplifier) {
                        case 0 -> // Resistance I (Amplifier 0, 80% 剩余伤害)
                                0.80F;
                        case 1 -> // Resistance II (Amplifier 1, 60% 剩余伤害)
                                0.60F;
                        case 2 -> // Resistance III (Amplifier 2, 40% 剩余伤害)
                                0.40F;
                        default -> 1.0F;
                    };
                    damageMultiplier = 1.0F / reductionFactor;
                }
                float finalDamageAmount = targetActualDamage * damageMultiplier;
                finalDamageAmount += 0.001F;
                Psychosis.LOGGER.info(
                        "[{}] NoticedBottle nominal damage: {} (Multiplier: {}, D_Actual: {})",
                        user.getName().getString(),
                        finalDamageAmount,
                        damageMultiplier,
                        targetActualDamage
                );
                RegistryEntry.Reference<DamageType> shadowDamageEntry = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(SHADOW_DAMAGE_KEY).orElse(null);

                if (shadowDamageEntry != null) {
                    user.damage(user.getDamageSources().create(shadowDamageEntry.registryKey()), finalDamageAmount);
                }
            } else {
                Psychosis.LOGGER.info(
                        "[{}] Dark Effect Amplifier reached 100 or higher. Skipping NoticedBottle damage and loud effects.",
                        user.getName().getString()
                );
            }

            // --- 最终效果 (始终应用) ---
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_OMINOUS_BOTTLE_DISPOSE, user.getSoundCategory(), 1.0F, 1.0F);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, integer, false, false, true));
            if (darkEffectEntry != null) {
                user.addStatusEffect(new StatusEffectInstance(darkEffectEntry, StatusEffectInstance.INFINITE, darkEffectAmplifier, false, false, true));
            }
        }

        stack.decrementUnlessCreative(1, user);
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        RegistryEntry<StatusEffect> darkEffectEntryForTooltip = Registries.STATUS_EFFECT
                .getEntry(DARK_EFFECT_KEY)
                .orElse(null);

        if (darkEffectEntryForTooltip != null) {
            tooltip.add(Text.translatable("notice1")
                    .formatted(Formatting.RED));
            tooltip.add(Text.translatable("notice")
                    .formatted(Formatting.GOLD));
        }
    }
}