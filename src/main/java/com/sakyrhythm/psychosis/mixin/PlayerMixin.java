package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis; // 导入主类以调用区块加载方法
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld; // 导入 ServerWorld
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements IPlayerEntity {

    // --- 属性声明 ---
    @Unique
    private int dark = 0;

    @Unique
    private boolean noticed = false; // 布尔变量

    @Unique
    private boolean previousNoticed = false; // 追踪上一刻的状态，用于检测首次变化

    // --- IPlayerEntity 接口实现 ---

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

    @Unique
    @Override
    public void setNoticed(boolean noticed) {
        this.noticed = noticed;
    }

    @Unique
    @Override
    public boolean getNoticed() {
        return this.noticed;
    }

    // --- Tick 注入逻辑 ---

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

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

        // --- 现有 dark 效果清除逻辑 ---
        RegistryEntry<StatusEffect> darkEffectEntry = player.getWorld().getRegistryManager()
                .get(RegistryKeys.STATUS_EFFECT)
                .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark")))
                .orElse(null);

        if (darkEffectEntry != null && !player.hasStatusEffect(darkEffectEntry)) {
            IPlayerEntity playerInterface = (IPlayerEntity) player;
            playerInterface.setDark(0);
        }
    }
}