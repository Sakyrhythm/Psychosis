package com.sakyrhythm.psychosis.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class DivineEffect extends StatusEffect{

    // 如果您的DivineEffect是通过注册类注册的，您应该在注册的地方定义一个public static final INSTANCE

    public DivineEffect() {
        super(
                StatusEffectCategory.BENEFICIAL, // 药水效果是有益的
                0xFFD700); // 显示的颜色 (金色)
    }

    /**
     * 这个方法在每个 tick 都会调用，以检查是否应应用药水效果。
     * 由于要求效果最高为I级 (amplifier 0)，我们将回血频率固定为每 20 刻 (1 秒) 一次，不随 amplifier 变化。
     * @param duration 剩余持续时间 (ticks)
     * @param amplifier 药水效果等级 (0为I级，1为II级, etc.)
     * @return 如果应该应用效果（即回血），则返回 true
     */
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // 固定回血频率为每 20 刻 (1 秒)
        return duration % 20 == 0;
    }

    /**
     * 这个方法在 canApplyUpdateEffect 返回 true 时被调用，实现自定义功能 (回血)。
     * @param entity 受影响的实体
     * @param amplifier 药水效果等级
     * @return 总是返回 super 方法的结果
     */
    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        // --- 持续回血逻辑 (每秒恢复 2% 最大生命值) ---

        float maxHealth = entity.getMaxHealth();
        // 基础恢复量 = 2% of maxHealth
        float healAmount = maxHealth * 0.02F;

        // 应用回血
        entity.heal(healAmount);

        return super.applyUpdateEffect(entity, amplifier);
    }
}