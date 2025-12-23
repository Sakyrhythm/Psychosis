package com.sakyrhythm.psychosis.item.custom;

import com.google.common.collect.ImmutableMap;
import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.item.ModArmorMaterials;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ModArmorItem extends ArmorItem {

    // 材质的 RegistryEntry，用于 hasCorrectArmorSet
    private static final RegistryEntry<ArmorMaterial> TARGET_MATERIAL_ENTRY = ModArmorMaterials.DIVINE;

    // ⭐ 移除 DarkEffectEntry 的静态声明和初始化

    // 核心：存储套装材质 -> 效果列表的 MAP。
    // 为了简化，我们只存储始终可用的原版效果。自定义效果将在 evaluateArmorEffects 中动态添加。
    private static final Map<RegistryEntry<ArmorMaterial>, List<StatusEffectInstance>> ARMOR_EFFECT_MAP;

    static {
        // 构建 Divine 套装的原版效果列表
        List<StatusEffectInstance> baseEffects = Arrays.asList(
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, StatusEffectInstance.INFINITE, 0, false, false, true),
                new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 0, false, false, true)
        );

        // 构建最终的 MAP
        ARMOR_EFFECT_MAP = new ImmutableMap.Builder<RegistryEntry<ArmorMaterial>, List<StatusEffectInstance>>()
                .put(TARGET_MATERIAL_ENTRY, baseEffects)
                .build();
    }

    // 构造函数
    public ModArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof PlayerEntity player) {

            boolean hasFullSet = hasCorrectArmorSet(player, TARGET_MATERIAL_ENTRY);

            if (hasFullSet) {
                // 套装完整时：评估所有效果（包括动态获取的 divine 效果）
                evaluateArmorEffects(player);
            } else {
                // 套装不完整时：移除所有效果
                removeArmorEffects(player);
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    // 移除效果的逻辑
    private void removeArmorEffects(PlayerEntity player) {
        // 1. 获取 Divine 材质的基础效果列表
        List<StatusEffectInstance> effects = ARMOR_EFFECT_MAP.get(TARGET_MATERIAL_ENTRY);
        if (effects == null) return;

        // 2. 获取动态效果的 Entry (用于移除)
        RegistryEntry<StatusEffect> divineEffectEntry = player.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                .orElse(null);

        // 3. 移除基础效果
        for (StatusEffectInstance effect : effects) {
            RegistryEntry<StatusEffect> effectType = effect.getEffectType();
            if (effectType != null) {
                StatusEffectInstance existingEffect = player.getStatusEffect(effectType);

                // 仅移除持续时间为无限的基础效果
                if (existingEffect != null && existingEffect.isInfinite()) {
                    player.removeStatusEffect(effectType);
                }
            }
        }

        // 4. 移除自定义 divine 效果 (仅移除无限时间的效果)
        if (divineEffectEntry != null) {
            StatusEffectInstance existingDivine = player.getStatusEffect(divineEffectEntry);

            // ⭐ 检查：如果玩家身上有 Divine 效果，并且它是无限持续时间 (isInfinite() == true)
            if (existingDivine != null && existingDivine.isInfinite()) {
                player.removeStatusEffect(divineEffectEntry);
            }
        }
    }


    private void evaluateArmorEffects(PlayerEntity player) {

        // ⭐ 核心实现：动态获取 divine 效果
        RegistryEntry<StatusEffect> divineEffectEntry = player.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "divine")))
                .orElse(null);

        // 遍历所有套装配置（这里只有一个 Divine 套装）
        for (Map.Entry<RegistryEntry<ArmorMaterial>, List<StatusEffectInstance>> entry : ARMOR_EFFECT_MAP.entrySet()) {
            RegistryEntry<ArmorMaterial> materialEntry = entry.getKey();
            List<StatusEffectInstance> effects = entry.getValue();

            if (hasCorrectArmorSet(player, materialEntry)) {

                // 1. 添加基础效果
                for (StatusEffectInstance effect : effects) {
                    addStatusEffectForMaterial(player, effect);
                }

                // 2. 添加动态获取的 divine 效果
                if (divineEffectEntry != null) {
                    StatusEffectInstance divineEffect = new StatusEffectInstance(
                            divineEffectEntry,
                            StatusEffectInstance.INFINITE,
                            0,
                            false,
                            false,
                            true
                    );
                    addStatusEffectForMaterial(player, divineEffect);
                }
            }
        }
    }

    private void addStatusEffectForMaterial(PlayerEntity player, StatusEffectInstance effect) {
        RegistryEntry<StatusEffect> effectType = effect.getEffectType();

        // 检查 effectType 是否为 null
        if (effectType == null) return;

        // 检查玩家是否已经有此效果
        boolean hasEffect = player.hasStatusEffect(effectType);

        if (!hasEffect) {
            // 添加效果的副本
            player.addStatusEffect(new StatusEffectInstance(effect));
        }
    }

    /**
     * 检查玩家是否穿戴了指定材质的完整护甲套装。
     */
    private boolean hasCorrectArmorSet(PlayerEntity player, RegistryEntry<ArmorMaterial> materialEntry) {
        // 遍历四个护甲槽位
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getMaterial() != materialEntry) {
                return false;
            }
        }
        return true;
    }
}