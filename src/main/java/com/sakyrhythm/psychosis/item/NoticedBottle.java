package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
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
import java.util.Objects;

public class NoticedBottle extends PotionItem {

    // 静态注册表键：Shadow 伤害
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

            // 获取瓶子放大器等级 (默认 0) - 用于 DARKNESS 效果
            Integer integer = (Integer)stack.getOrDefault(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, 0);

            // --- Dark 效果等级计算 START ---

            RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
                    .getEntry(DARK_EFFECT_KEY)
                    .orElse(null);

            int darkEffectAmplifier = 0; // 默认新等级为 0

            if (darkEffectEntry != null) {
                // 1. 检查玩家是否已有该效果
                StatusEffectInstance existingDarkEffect = user.getStatusEffect(darkEffectEntry);

                if (existingDarkEffect != null) {
                    // 如果存在，新等级 = 现有等级 + 1
                    darkEffectAmplifier = existingDarkEffect.getAmplifier() + 1;
                } else {
                    // 如果不存在，新等级 = 0
                    darkEffectAmplifier = 0;
                }
            }

            // --- Dark 效果等级计算 END ---

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

            // --- 清空饱食度 ---
            if (user instanceof PlayerEntity player) {
                player.getHungerManager().setFoodLevel(0);
                player.getHungerManager().setSaturationLevel(0);
                player.getHungerManager().setExhaustion(0);
            }
            // -------------------

            // 2. 造成自定义 Shadow 伤害 (血量上限 - 1)
            float maxHealth = user.getMaxHealth();
            float damageAmount = maxHealth - 1.0F;
            if (damageAmount < 0.0F) {
                damageAmount = 0.0F;
            }

            // 使用 Shadow 伤害源造成伤害
            RegistryEntry.Reference<DamageType> shadowDamageEntry = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(SHADOW_DAMAGE_KEY).orElse(null);

            if (shadowDamageEntry != null) {
                user.damage(user.getDamageSources().create(shadowDamageEntry.registryKey()), damageAmount);
            }

            // 3. 应用状态效果
            world.playSound((PlayerEntity)null, user.getBlockPos(), SoundEvents.ITEM_OMINOUS_BOTTLE_DISPOSE, user.getSoundCategory(), 1.0F, 1.0F);

            // 应用黑暗 (DARKNESS) 效果 (使用瓶子自带等级)
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, integer, false, false, true));

            // 应用自定义 "dark" 效果 (使用计算后的等级: darkEffectAmplifier)
            if (darkEffectEntry != null) {
                user.addStatusEffect(new StatusEffectInstance(darkEffectEntry, StatusEffectInstance.INFINITE, darkEffectAmplifier, false, false, true));
            }
        }

        stack.decrementUnlessCreative(1, user);
        return stack;
    }

    // 获取最大使用时间
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    // 获取使用动作
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    // 物品右键使用逻辑
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    // 物品工具提示
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        // 【关键修改：移除 super.appendTooltip(...)】
        // 移除这一行：super.appendTooltip(stack, context, tooltip, type);
        // 这样就阻止了父类 PotionItem 检测到空药水内容并显示“无效果”文本。

        // Tooltip 无法获取玩家身上的实时效果，因此只显示默认信息
        //int darkEffectAmplifierForTooltip = 0;

        // 从静态注册表安全获取效果条目
        RegistryEntry<StatusEffect> darkEffectEntryForTooltip = Registries.STATUS_EFFECT
                .getEntry(DARK_EFFECT_KEY)
                .orElse(null);

        // 从这里开始，您的代码逻辑将直接添加到 tooltip 中，不会有“无效果”的干扰。
        if (darkEffectEntryForTooltip != null) {
            //List<StatusEffectInstance> list = List.of(new StatusEffectInstance(darkEffectEntryForTooltip, StatusEffectInstance.INFINITE, darkEffectAmplifierForTooltip, false, false, true));
            //PotionContentsComponent.buildTooltip(list, tooltip::add, 1.0F, context.getUpdateTickRate());
            tooltip.add(Text.translatable("notice1")
                    .formatted(Formatting.RED));
            tooltip.add(Text.translatable("notice")
                    .formatted(Formatting.GOLD));
        }
    }
}