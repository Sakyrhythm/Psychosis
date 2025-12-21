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

    // --- 缓存 StatusEffect RegistryEntry ---
    @Unique
    private static RegistryEntry<StatusEffect> darkEffectEntryCache = null;

    @Unique
    private static RegistryEntry<StatusEffect> frenzyEffectEntryCache = null;

    // 新增：缓存 VulnerableEffect 的 RegistryEntry
    @Unique
    private static RegistryEntry<StatusEffect> vulnerableEffectEntryCache = null;


    // --- IPlayerEntity 属性声明 (保持不变) ---
    @Unique private int dark = 0;
    @Unique public boolean noticed = false;
    @Unique private boolean previousNoticed = false;
    @Unique private boolean darkMsg1Sent = false;
    @Unique private boolean darkMsg2Sent = false;
    @Unique private boolean darkMsg3Sent = false;
    @Unique private boolean darkMsg4Sent = false;

    // --- IPlayerEntity 接口实现 (已省略，假设已完整) ---
    @Unique @Override public void setDark(int dark) { this.dark = dark; }
    @Unique @Override public int getDark() { return this.dark; }
    @Unique @Override public void setNoticed(boolean noticed) { this.noticed = noticed; }
    @Unique @Override public boolean getNoticed() { return this.noticed; }
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
        IPlayerEntity playerInterface = (IPlayerEntity) player;

        // 【死亡时清除逻辑】: 检查玩家是否死亡 (保持不变)
        if (player.getHealth() <= 0.0F) {
            if (!player.getWorld().isClient()) {
                Psychosis.LOGGER.info("Player {} health is zero. Executing full psychosis state reset.", player.getName().getString());
                playerInterface.setDark(0);
                playerInterface.setNoticed(false);
                playerInterface.setDarkMsg1Sent(false);
                playerInterface.setDarkMsg2Sent(false);
                playerInterface.setDarkMsg3Sent(false);
                playerInterface.setDarkMsg4Sent(false);
            }
            return;
        }

        // 仅在服务器端处理逻辑
        if (!player.getWorld().isClient()) {
            // ... (现有的 noticed 状态和区块加载逻辑保持不变) ...
            if (this.noticed && !this.previousNoticed) {
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    Psychosis.forceAndScheduleUnload(serverWorld, player.getBlockPos());
                    Psychosis.LOGGER.info("Player {} was noticed for the first time! Forced chunks loaded.", player.getName().getString());
                }
            }
            this.previousNoticed = this.noticed;

            // --- 效果注册项加载和缓存 ---
            if (darkEffectEntryCache == null) {
                darkEffectEntryCache = player.getWorld().getRegistryManager()
                        .get(RegistryKeys.STATUS_EFFECT)
                        .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "dark")))
                        .orElse(null);
            }
            if (frenzyEffectEntryCache == null) {
                frenzyEffectEntryCache = player.getWorld().getRegistryManager()
                        .get(RegistryKeys.STATUS_EFFECT)
                        .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "frenzy")))
                        .orElse(null);
            }
            // 新增：加载 VulnerableEffect
            if (vulnerableEffectEntryCache == null) {
                vulnerableEffectEntryCache = player.getWorld().getRegistryManager()
                        .get(RegistryKeys.STATUS_EFFECT)
                        .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")))
                        .orElse(null);
            }


            // --- 现有 dark 效果清除逻辑 (当效果自然消失时) ---
            if (darkEffectEntryCache != null && !player.hasStatusEffect(darkEffectEntryCache)) {
                // 如果 DarkEffect 消失，重置所有状态
                playerInterface.setDark(0);
                playerInterface.setNoticed(false);
                playerInterface.setDarkMsg1Sent(false);
                playerInterface.setDarkMsg2Sent(false);
                playerInterface.setDarkMsg3Sent(false);
                playerInterface.setDarkMsg4Sent(false);
            }

            // --- 核心逻辑: 检查 DarkEffect 依赖并移除 Frenzy 和 Vulnerable ---

            // 确保 DarkEffect 注册项存在
            if (darkEffectEntryCache != null) {
                // 检查玩家是否**没有** DarkEffect
                if (!player.hasStatusEffect(darkEffectEntryCache)) {

                    boolean removedFrenzy = false;
                    boolean removedVulnerable = false;

                    // 1. 检查并移除 FrenzyEffect
                    if (frenzyEffectEntryCache != null && player.hasStatusEffect(frenzyEffectEntryCache)) {
                        player.removeStatusEffect(frenzyEffectEntryCache);
                        removedFrenzy = true;
                    }

                    // 2. 检查并移除 VulnerableEffect
                    if (vulnerableEffectEntryCache != null && player.hasStatusEffect(vulnerableEffectEntryCache)) {
                        player.removeStatusEffect(vulnerableEffectEntryCache);
                        removedVulnerable = true;
                    }

                    if (removedFrenzy || removedVulnerable) {
                        Psychosis.LOGGER.debug("Player {} 失去了 DarkEffect，依赖效果（Frenzy/Vulnerable）已被移除。", player.getName().getString());
                    }

                }
            } else {
                // 如果 DarkEffect 注册项找不到，则每 10 秒警告一次
                if (player.getWorld().getTime() % 200 == 0) {
                    Psychosis.LOGGER.warn("无法在 PlayerMixin 中找到 DarkEffect 的 RegistryEntry。依赖检查无法执行。");
                }
            }
        }
    }
}