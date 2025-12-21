package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.item.ModItems; // 假设您的物品注册类
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingRecipeRegistry.class)
public abstract class BrewingRecipeRegistryMixin {

    @Inject(
            method = "registerDefaults(Lnet/minecraft/recipe/BrewingRecipeRegistry$Builder;)V", // 方法签名
            at = @At("HEAD") // 在原版注册开始前注入
    )
    private static void psychosis_injectBrewingRecipes(BrewingRecipeRegistry.Builder builder, CallbackInfo ci) {

        Item inputItem = Items.POTION;
        Item ingredientItem = ModItems.White_EYE;
        Item outputItem = ModItems.NoticedBottle;

        builder.registerItemRecipe(
                inputItem,
                ingredientItem,
                outputItem
        );
    }
}