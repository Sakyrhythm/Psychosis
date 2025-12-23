package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModFoodComponents {

    private static final RegistryKey<StatusEffect> DIVINE_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            Identifier.of(Psychosis.MOD_ID, "divine")
    );
    private static RegistryEntry<StatusEffect> getDivineEffectEntry() {
        return Registries.STATUS_EFFECT.getEntry(DIVINE_EFFECT_KEY)
                .orElseThrow(() -> new IllegalStateException("Divine Status Effect not found in registry!"));
    }

    public static final FoodComponent DIVINE_APPLE = new FoodComponent.Builder()
            .nutrition(10).saturationModifier(1.5f)
            .statusEffect(new StatusEffectInstance(getDivineEffectEntry(), 12000, 0), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 12000, 0), 1f)
            .build();

    public static final FoodComponent HAPPYCANDY = new FoodComponent.Builder()
            .nutrition(2).saturationModifier(0.2f)
            // 调用 Getter 方法
            .statusEffect(new StatusEffectInstance(getDivineEffectEntry(), 60, 0), 1f)
            .alwaysEdible().snack()
            .build();
}