package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.EntityType;
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
// 导入静态 Registries 类
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

    // 使用静态字段定义 RegistryKey，安全可靠。
    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );

    public NoticedBottle(Item.Settings settings) {
        super(settings);
    }

    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger(serverPlayerEntity, stack);
            serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
        }

        if (!world.isClient) {
            if (world instanceof ServerWorld serverWorld) {
                // ... (闪电生成和声音逻辑不变)
                BlockPos playerPos = user.getBlockPos();
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, serverWorld);
                lightning.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
                serverWorld.spawnEntity(lightning);
                serverWorld.playSound(
                        null, playerPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundCategory.WEATHER, 5.0F, 0.8F + world.random.nextFloat() * 0.2F
                );
            }
            if (user instanceof IPlayerEntity playerInterface) {
                playerInterface.setNoticed(true);
            }

            // 【finishUsing 修复】使用静态 Registries 替代 world.getRegistryManager()
            RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
                    .getEntry(DARK_EFFECT_KEY)
                    .orElse(null);

            world.playSound((PlayerEntity)null, user.getBlockPos(), SoundEvents.ITEM_OMINOUS_BOTTLE_DISPOSE, user.getSoundCategory(), 1.0F, 1.0F);
            Integer integer = (Integer)stack.getOrDefault(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, 0);

            user.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, integer, false, false, true));

            if (darkEffectEntry != null) {
                user.addStatusEffect(new StatusEffectInstance(darkEffectEntry, StatusEffectInstance.INFINITE, integer, false, false, true));
            }
        }

        stack.decrementUnlessCreative(1, user);
        return stack;

    }

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

        // 【appendTooltip 修复】使用静态 Registries 替代 context.getRegistryManager()
        RegistryEntry<StatusEffect> darkEffectEntryForTooltip = Registries.STATUS_EFFECT
                .getEntry(DARK_EFFECT_KEY)
                .orElse(null);

        if (darkEffectEntryForTooltip != null) {
            List<StatusEffectInstance> list = List.of(new StatusEffectInstance(darkEffectEntryForTooltip, StatusEffectInstance.INFINITE, integer, false, false, true));
            Objects.requireNonNull(tooltip);
            PotionContentsComponent.buildTooltip(list, tooltip::add, 1.0F, context.getUpdateTickRate());
        }
    }
}