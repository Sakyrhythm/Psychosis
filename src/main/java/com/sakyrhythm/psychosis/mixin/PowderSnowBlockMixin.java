package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.item.ModArmorItems;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
    @Inject(method = "canWalkOnPowderSnow", at = @At("RETURN"), cancellable = true)
    private static void injectDivineArmorWalk(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (entity instanceof LivingEntity livingEntity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    ItemStack stack = livingEntity.getEquippedStack(slot);
                    if (isDivineArmor(stack)) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }
    @Unique
    private static boolean isDivineArmor(ItemStack stack) {
        return stack.isOf(ModArmorItems.DIVINE_HELMET) ||
                stack.isOf(ModArmorItems.DIVINE_CHESTPLATE) ||
                stack.isOf(ModArmorItems.DIVINE_LEGGINGS) ||
                stack.isOf(ModArmorItems.DIVINE_BOOTS);
    }
}