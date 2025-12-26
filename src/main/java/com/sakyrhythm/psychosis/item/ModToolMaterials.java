package com.sakyrhythm.psychosis.item;

import com.google.common.base.Suppliers;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.Objects;
import java.util.function.Supplier;

public enum ModToolMaterials implements ToolMaterial {
    DARK(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, (int) Float.MAX_VALUE,0f,11.0f,50,() -> Ingredient.ofItems(Items.COMMAND_BLOCK));

    private final TagKey<Block> inverseTag;
    private final int itemDurability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final Supplier<Ingredient> repairIngredient;
    ModToolMaterials(final TagKey<Block> inverseTag, final int itemDurability, final float miningSpeed, final float attackDamage, final int enchantability, final Supplier<Ingredient> repairIngredient) {
        this.inverseTag = inverseTag;
        this.itemDurability = itemDurability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        Objects.requireNonNull(repairIngredient);
        this.repairIngredient = Suppliers.memoize(repairIngredient::get);
    }
    @Override
    public int getDurability() {
        return this.itemDurability;
    }
    @Override
    public float getMiningSpeedMultiplier() {
        return this.miningSpeed;
    }
    @Override
    public float getAttackDamage() {
        return this.attackDamage;
    }
    @Override
    public TagKey<Block> getInverseTag() {
        return this.inverseTag;
    }
    @Override
    public int getEnchantability() {
        return this.enchantability;
    }
    @Override
    public Ingredient getRepairIngredient() {
        return (Ingredient)this.repairIngredient.get();
    }
}
