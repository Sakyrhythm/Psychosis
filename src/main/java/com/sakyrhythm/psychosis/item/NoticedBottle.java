package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageType; // 导入 DamageType
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;

public class NoticedBottle extends Item {

    // 【无需修改】静态注册表键：Shadow 伤害
    private static final RegistryKey<DamageType> SHADOW_DAMAGE_KEY = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "shadow")
    );
    // 静态注册表键：Dark 效果
    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );

    // 构造函数
    public NoticedBottle(Item.Settings settings) {
        super(settings);
    }

    // 物品使用完毕时触发
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger(serverPlayerEntity, stack);
            serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
        }

        if (!world.isClient) {

            // 获取瓶子放大器等级 (默认 0)
            Integer integer = (Integer)stack.getOrDefault(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, 0);

            // 计算自定义效果 "dark" 的最终等级 (瓶子等级 + 1)
            int darkEffectAmplifier = integer + 1;

            if (world instanceof ServerWorld serverWorld) {
                // 1. 闪电效果 (无伤害无火焰)
                BlockPos playerPos = user.getBlockPos();
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                lightning.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
                lightning.setCosmetic(true); // 设置为装饰性，无伤害无火焰
                serverWorld.spawnEntity(lightning);
                serverWorld.playSound(
                        null, playerPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundCategory.WEATHER, 5.0F, 0.8F + world.random.nextFloat() * 0.2F
                );
            }
            if (user instanceof IPlayerEntity playerInterface) {
                playerInterface.setNoticed(true);
            }

            // --- 新增功能：清空饱食度 ---
            if (user instanceof PlayerEntity player) {
                player.getHungerManager().setFoodLevel(0);
                player.getHungerManager().setSaturationLevel(0);
                player.getHungerManager().setExhaustion(0);
            }
            // -----------------------------

            // 2. 造成自定义 Shadow 伤害 (血量上限 - 1)
            float maxHealth = user.getMaxHealth();
            float damageAmount = maxHealth - 1.0F;
            if (damageAmount < 0.0F) {
                damageAmount = 0.0F;
            }

            // 【修改点 START】: 使用 Shadow 伤害源造成伤害
            RegistryEntry.Reference<DamageType> shadowDamageEntry = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(SHADOW_DAMAGE_KEY).orElse(null);

            if (shadowDamageEntry != null) {
                user.damage(user.getDamageSources().create(shadowDamageEntry.registryKey()), damageAmount);
            } else {
                // 如果 Shadow 伤害源未注册或未找到，可以考虑使用默认的通用伤害，或者保持原有逻辑作为回退
                // 为了保持修改目的，我们仅在找到时造成伤害，否则跳过
                // user.damage(user.getDamageSources().generic(), damageAmount); // 备用方案
            }
            // 【修改点 END】

            // 3. 应用状态效果
            RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
                    .getEntry(DARK_EFFECT_KEY)
                    .orElse(null);

            world.playSound((PlayerEntity)null, user.getBlockPos(), SoundEvents.ITEM_OMINOUS_BOTTLE_DISPOSE, user.getSoundCategory(), 1.0F, 1.0F);

            // 应用黑暗 (DARKNESS) 效果 (使用瓶子自带等级)
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, integer, false, false, true));

            // 应用自定义 "dark" 效果 (使用计算后的等级: integer + 1)
            if (darkEffectEntry != null) {
                user.addStatusEffect(new StatusEffectInstance(darkEffectEntry, StatusEffectInstance.INFINITE, darkEffectAmplifier, false, false, true));
            }
        }

        stack.decrementUnlessCreative(1, user);
        return stack;
    }

    // ... (其余方法保持不变)
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        Integer integer = (Integer)stack.getOrDefault(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, 0);

        int darkEffectAmplifierForTooltip = integer + 1;

        RegistryEntry<StatusEffect> darkEffectEntryForTooltip = Registries.STATUS_EFFECT
                .getEntry(DARK_EFFECT_KEY)
                .orElse(null);

        if (darkEffectEntryForTooltip != null) {
            List<StatusEffectInstance> list = List.of(new StatusEffectInstance(darkEffectEntryForTooltip, StatusEffectInstance.INFINITE, darkEffectAmplifierForTooltip, false, false, true));
            Objects.requireNonNull(tooltip);
            PotionContentsComponent.buildTooltip(list, tooltip::add, 1.0F, context.getUpdateTickRate());
        }
    }
}