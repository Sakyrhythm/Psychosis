package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
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

import java.util.Optional; // 导入 Optional 用于封装结果

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements IPlayerEntity {

    // --- 缓存 StatusEffect RegistryEntry ---
    @Unique
    private static RegistryEntry<StatusEffect> darkEffectEntryCache = null;

    @Unique
    private static RegistryEntry<StatusEffect> frenzyEffectEntryCache = null;

    @Unique
    private static RegistryEntry<StatusEffect> vulnerableEffectEntryCache = null;


    // --- IPlayerEntity 属性声明 ---
    @Unique private int dark = 0;
    @Unique public boolean noticed = false;
    @Unique private boolean previousNoticed = false;
    @Unique private boolean darkMsg1Sent = false;
    @Unique private boolean darkMsg2Sent = false;
    @Unique private boolean darkMsg3Sent = false;
    @Unique private boolean darkMsg4Sent = false;

    // --- IPlayerEntity 接口实现 ---
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

    // ==========================================================
    // ⭐ 新增查询方法：查询 DarkEffect 详情
    // ==========================================================
    /**
     * 查询当前玩家身上的 DarkEffect 实例。
     * 仅在服务器端有效。
     * @return 如果找到 DarkEffect，返回 Optional<StatusEffectInstance>；否则返回 Optional.empty()。
     */
    @Unique
    public Optional<StatusEffectInstance> queryDarkEffectInstance() {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // 确保在服务器端执行，且 DarkEffect 注册项已被加载
        if (player.getWorld().isClient() || darkEffectEntryCache == null) {
            return Optional.empty();
        }

        // 使用 getStatusEffect(RegistryEntry<StatusEffect>) 方法来安全查询
        StatusEffectInstance instance = player.getStatusEffect(darkEffectEntryCache);

        return Optional.ofNullable(instance);
    }

    /**
     * 查询 DarkEffect 的等级 (Amplifier + 1) 和剩余时间 (Duration in ticks)。
     * @return 一个数组：[等级, 剩余时间 (ticks)]。如果不存在，返回 [0, 0]。
     */
    @Unique
    public int[] queryDarkEffectInfo() {
        return queryDarkEffectInstance().map(instance ->
                new int[]{instance.getAmplifier() + 1, instance.getDuration()}
        ).orElseGet(() -> new int[]{0, 0});
    }

    // ==========================================================
    // --- Tick 注入逻辑 ---

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        IPlayerEntity playerInterface = (IPlayerEntity) player;

        // 【死亡时清除逻辑】
        if (player.getHealth() <= 0.0F) {
            if (!player.getWorld().isClient()) {
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

            // --- 效果注册项加载和缓存 (确保在 Tick 中能获取到 RegistryEntry) ---
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
            if (vulnerableEffectEntryCache == null) {
                vulnerableEffectEntryCache = player.getWorld().getRegistryManager()
                        .get(RegistryKeys.STATUS_EFFECT)
                        .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "vulnerable")))
                        .orElse(null);
            }


            // --- 现有 dark 效果清除逻辑 ---
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
            if (darkEffectEntryCache != null && !player.hasStatusEffect(darkEffectEntryCache)) {

                boolean removedFrenzy = false;
                boolean removedVulnerable = false;

                if (frenzyEffectEntryCache != null && player.hasStatusEffect(frenzyEffectEntryCache)) {
                    player.removeStatusEffect(frenzyEffectEntryCache);
                    removedFrenzy = true;
                }

                if (vulnerableEffectEntryCache != null && player.hasStatusEffect(vulnerableEffectEntryCache)) {
                    player.removeStatusEffect(vulnerableEffectEntryCache);
                    removedVulnerable = true;
                }

                if (removedFrenzy || removedVulnerable) {
                    Psychosis.LOGGER.debug("Player {} 失去了 DarkEffect，依赖效果（Frenzy/Vulnerable）已被移除。", player.getName().getString());
                }
            } else if (darkEffectEntryCache == null && player.getWorld().getTime() % 200 == 0) {
                Psychosis.LOGGER.warn("无法在 PlayerMixin 中找到 DarkEffect 的 RegistryEntry。依赖检查无法执行。");
            }
        }
    }
}