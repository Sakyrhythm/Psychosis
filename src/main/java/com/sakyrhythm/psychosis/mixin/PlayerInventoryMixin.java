package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.item.DarkSwordItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    // --- Shadowed 字段 ---
    // 物品栏所属的玩家实例
    @Shadow @Final
    public PlayerEntity player;

    // 主物品栏列表 (包括快捷栏)
    @Shadow @Final
    public DefaultedList<ItemStack> main;

    // 盔甲栏列表
    @Shadow @Final
    public DefaultedList<ItemStack> armor;

    // 副手列表
    @Shadow @Final
    public DefaultedList<ItemStack> offHand;

    // --- Shadowed 方法 (无需实现，用于类型定义) ---
    @Shadow public abstract void clear();


    /**
     * 拦截 PlayerInventory.clear() 方法的头部 (@At("HEAD"))。
     * 当执行 /clear 命令、创造模式物品栏清空或死亡清空时，此方法会被调用。
     * 我们在原版逻辑清空列表之前，遍历所有列表并对锚定剑解除锚定，并恢复生命值。
     */
    @Inject(method = "clear", at = @At("HEAD"))
    private void psychosis$handleClearAnchoredSwords(CallbackInfo ci) {

        // 1. 检查并处理主物品栏 (主物品栏 + 快捷栏)
        for (ItemStack stack : this.main) {
            DarkSwordItem.handleSwordRemoval(stack, this.player);
        }

        // 2. 检查并处理盔甲栏
        for (ItemStack stack : this.armor) {
            DarkSwordItem.handleSwordRemoval(stack, this.player);
        }

        // 3. 检查并处理副手
        for (ItemStack stack : this.offHand) {
            DarkSwordItem.handleSwordRemoval(stack, this.player);
        }

        // 注意：我们不需要担心列表是否为空，因为 handleSwordRemoval 会检查 ItemStack 是否为 DarkSwordItem。
        // 原版 PlayerInventory.clear() 将在 Mixin 方法结束后继续执行，从而清空这些列表。
    }
}