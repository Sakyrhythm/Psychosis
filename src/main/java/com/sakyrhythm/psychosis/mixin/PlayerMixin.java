package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
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