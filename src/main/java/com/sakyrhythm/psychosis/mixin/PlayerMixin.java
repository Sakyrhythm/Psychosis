package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements IPlayerEntity {

    // --- IPlayerEntity 属性声明 ---
    @Unique
    private int dark = 0;
    @Unique
    private boolean noticed = false;
    @Unique
    private boolean previousNoticed = false;

    // 消息锁属性
    @Unique private boolean darkMsg1Sent = false;
    @Unique private boolean darkMsg2Sent = false;
    @Unique private boolean darkMsg3Sent = false;
    @Unique private boolean darkMsg4Sent = false;

    // --- IPlayerEntity 接口实现 (已补齐) ---

    // 核心 Dark 状态
    @Unique
    @Override
    public void setDark(int dark) {
        this.dark = dark;
    }

    @Unique
    @Override
    public int getDark() {
        return this.dark;
    }

    // Noticed 状态
    @Unique
    @Override
    public void setNoticed(boolean noticed) {
        this.noticed = noticed;
    }

    // 假设 IPlayerEntity 中有 getNoticed()
    @Unique
    @Override
    public boolean getNoticed() {
        return this.noticed;
    }

    // 消息锁实现
    @Unique @Override public void setDarkMsg1Sent(boolean sent) { this.darkMsg1Sent = sent; }
    @Unique @Override public boolean getDarkMsg1Sent() { return this.darkMsg1Sent; }

    @Unique @Override public void setDarkMsg2Sent(boolean sent) { this.darkMsg2Sent = sent; }
    @Unique @Override public boolean getDarkMsg2Sent() { return this.darkMsg2Sent; }

    @Unique @Override public void setDarkMsg3Sent(boolean sent) { this.darkMsg3Sent = sent; }
    @Unique @Override public boolean getDarkMsg3Sent() { return this.darkMsg3Sent; }

    @Unique @Override public void setDarkMsg4Sent(boolean sent) { this.darkMsg4Sent = sent; }
    @Unique @Override public boolean getDarkMsg4Sent() { return this.darkMsg4Sent; }


    // --- Tick 注入逻辑 ---

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        IPlayerEntity playerInterface = (IPlayerEntity) player; // 提前获取接口实例


        // 1. 仅在服务器端处理 'noticed' 状态变化逻辑
        if (!player.getWorld().isClient()) {
            // 检测状态是否从 false 首次变为 true
            if (this.noticed && !this.previousNoticed) {
                // 执行区块加载逻辑
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    Psychosis.forceAndScheduleUnload(serverWorld, player.getBlockPos());
                    Psychosis.LOGGER.info("Player {} was noticed for the first time! Forced chunks loaded.", player.getName().getString());
                }
            }
            this.previousNoticed = this.noticed;
        }

        // --- 现有 dark 效果清除逻辑 (当效果自然消失时) ---
        RegistryEntry<StatusEffect> darkEffectEntry = player.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark")))
                .orElse(null);

        // 如果效果不存在，清除残留状态 (效果清除时，所有状态都重置)
        if (darkEffectEntry != null && !player.hasStatusEffect(darkEffectEntry)) {

            playerInterface.setDark(0);
            playerInterface.setNoticed(false);
            playerInterface.setDarkMsg1Sent(false);
            playerInterface.setDarkMsg2Sent(false);
            playerInterface.setDarkMsg3Sent(false);
            playerInterface.setDarkMsg4Sent(false);
        }
    }
}