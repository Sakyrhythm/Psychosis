package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public abstract class StatusEffectInstanceMixin {

    // 假设您的 DarkEffect ID 是 "psychosis:dark"
    @Unique
    private static final Identifier DARK_EFFECT_ID = Identifier.of(Psychosis.MOD_ID, "dark");

    // 将 'getAmplifier' 的注入点设置为 RETURN，即方法执行完毕即将返回值时。
    @Inject(method = "getAmplifier", at = @At("RETURN"), cancellable = true)
    private void psychosis$modifyAmplifierForSafety(CallbackInfoReturnable<Integer> cir) {
        // Mixin Target 对象 (StatusEffectInstance)
        StatusEffectInstance self = (StatusEffectInstance)(Object)this;

        // 获取原始的放大器等级 (也就是 cir.getReturnValue())
        int originalAmplifier = cir.getReturnValue();

        // --- 修正点 2 & 3：安全地获取 Identifier ---
        // 1. 获取 StatusEffectInstance 对应的 StatusEffect 对象
        // 2. 通过 Minecraft 的注册表 Registries.STATUS_EFFECT 获取该效果的 Identifier
        Identifier currentEffectId = Registries.STATUS_EFFECT.getId(self.getEffectType().value());

        if (DARK_EFFECT_ID.equals(currentEffectId)) {

            // 检查等级是否超过安全阈值 (例如 30)
            if (originalAmplifier > 30) {
                // 欺骗游戏核心代码，返回 0 级

                // 使用 CIR.setReturnValue(T) 覆盖方法的返回值
                cir.setReturnValue(0);

                // (可选) 使用 CIR.cancel() 提前退出 Mixin 注入链
                // cir.cancel();
            }
        }

        // 对于其他效果或低等级，不进行操作，方法将返回原始值
    }
}