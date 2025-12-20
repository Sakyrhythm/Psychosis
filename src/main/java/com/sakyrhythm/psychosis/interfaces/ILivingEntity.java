package com.sakyrhythm.psychosis.interfaces;

import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface ILivingEntity {

    @Nullable LivingEntity psychosis_template_1_21$getLastAttacker();

    void psychosis_template_1_21$setLastAttacker(@Nullable LivingEntity attacker);

    boolean psychosis_template_1_21$getCBHurt();

    void psychosis_template_1_21$setCBHurt(boolean value);
    /**
     * 获取上一次受到黑暗伤害的时间（游戏刻）。
     * @return 游戏刻时间。
     */
    long psychopomp$getLastDarkDamageTime();

    /**
     * 设置上一次受到黑暗伤害的时间（游戏刻）。
     * @param ticks 当前游戏刻时间。
     */
    void psychopomp$setLastDarkDamageTime(long ticks);
}