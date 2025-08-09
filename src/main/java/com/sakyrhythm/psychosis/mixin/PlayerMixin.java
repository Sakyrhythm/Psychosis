package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements IPlayerEntity { // <--- 这里添加 implements IPlayerEntity
    @Unique
    private int dark = 0;

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
}