package com.sakyrhythm.psychosis.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Unique
    private static final Identifier DARK_BG = Identifier.of("psychosis", "boss_bar/dark_background");
    @Unique
    private static final Identifier DARK_PROGRESS = Identifier.of("psychosis", "boss_bar/dark_progress");

    @Inject(
            method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;I[Lnet/minecraft/util/Identifier;[Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void psychosis$renderCustomBossBar(DrawContext context, int x, int y, BossBar bossBar, int width, Identifier[] textures, Identifier[] notchedTextures, CallbackInfo ci) {

        if (bossBar.getName().getString().equals("entity.psychosis.scythe") ||
                bossBar.getName().getContent().toString().contains("scythe")) {
            if (bossBar.getColor() == BossBar.Color.WHITE) {
                context.drawGuiTexture(DARK_BG, 182, 5, 0, 0, x, y, 182, 5);

                int i = (int)(bossBar.getPercent() * 182.0F);
                if (i > 0) {
                    context.drawGuiTexture(DARK_PROGRESS, 182, 5, 0, 0, x, y, i, 5);
                }
                if (bossBar.getStyle() != BossBar.Style.PROGRESS) {
                    context.drawGuiTexture(notchedTextures[bossBar.getStyle().ordinal() - 1], 182, 5, 0, 0, x, y, 182, 5);
                }
                ci.cancel();
            }
        }
    }
}