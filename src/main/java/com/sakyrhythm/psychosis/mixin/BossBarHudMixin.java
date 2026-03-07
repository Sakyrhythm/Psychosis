package com.sakyrhythm.psychosis.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Unique private static final int TEX_WIDTH = 179;
    @Unique private static final int TEX_HEIGHT = 25;
    @Unique private static final int TARGET_WIDTH = 220;

    // ⭐ 新增：解耦坐标控制
    @Unique private static final int BAR_Y = 8;    // 血条的绝对高度（原为 15，现在往上提）
    @Unique private static final int TEXT_Y = 5;   // 文本的绝对高度（保持在你认为合适的位置）

    @Unique private static final Identifier BOSS_BG = Identifier.of("psychosis", "boss_bar/dark_background");
    @Unique private static final Identifier PROGRESS_DARK_GOD = Identifier.of("psychosis", "boss_bar/dark_progress");
    @Unique private static final Identifier PROGRESS_HEALTH = Identifier.of("psychosis", "boss_bar/dark_progress2");
    @Unique private static final Identifier PROGRESS_SHIELD = Identifier.of("psychosis", "boss_bar/dark_progress1");

    @Unique private int firstModBarY = -1;

    @Inject(method = "render", at = @At("HEAD"))
    private void psychosis$resetFrame(DrawContext context, CallbackInfo ci) {
        this.firstModBarY = -1;
    }

    @Inject(
            method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;I[Lnet/minecraft/util/Identifier;[Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void psychosis$renderCustomBossBar(DrawContext context, int x, int y, BossBar bossBar, int width, Identifier[] textures, Identifier[] notchedTextures, CallbackInfo ci) {
        if (isModBoss(bossBar)) {
            if (this.firstModBarY == -1) this.firstModBarY = y;

            // ⭐ 使用 BAR_Y 控制血条高度
            int renderY = BAR_Y;

            Identifier currentProgress = PROGRESS_DARK_GOD;
            boolean isShield = bossBar.getColor() == BossBar.Color.WHITE;

            if (isShield) {
                currentProgress = PROGRESS_SHIELD;
            } else if (bossBar.getColor() == BossBar.Color.RED) {
                currentProgress = PROGRESS_HEALTH;
            }

            MatrixStack matrices = context.getMatrices();
            matrices.push();

            float screenCenterX = context.getScaledWindowWidth() / 2.0f;

            // ⭐ 保持 Z-Offset 确保护盾在最上层
            float zOffset = isShield ? 1.0f : 0.0f;
            matrices.translate(screenCenterX, (float)renderY, zOffset);

            float scaleX = (float) TARGET_WIDTH / (float) TEX_WIDTH;
            matrices.scale(scaleX, 1.0f, 1.0f);

            int drawX = -TEX_WIDTH / 2;
            float percent = bossBar.getPercent();

            // 1. 绘制背景 (仅由血条层绘制一次)
            if (!isShield) {
                context.drawGuiTexture(BOSS_BG, TEX_WIDTH, TEX_HEIGHT, 0, 0, drawX, 0, TEX_WIDTH, TEX_HEIGHT);
            }

            // 2. 绘制进度
            if (percent > 0.0F) {
                int progressWidth = (int) (percent * (float) TEX_WIDTH);
                context.drawGuiTexture(currentProgress, TEX_WIDTH, TEX_HEIGHT, 0, 0, drawX, 0, progressWidth, TEX_HEIGHT);
            }

            matrices.pop();
            ci.cancel();
        }
    }

    @ModifyArgs(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I")
    )
    private void psychosis$positionBossName(Args args) {
        Text text = args.get(1);
        String name = text.getString();

        if (isModName(name)) {
            if (!name.trim().isEmpty()) {
                // ⭐ 名字使用独立的 TEXT_Y，不再受 BAR_Y 变动影响
                args.set(3, TEXT_Y);
            } else {
                args.set(3, -500); // 隐藏占位符空格
            }
        }
    }

    @Unique
    private boolean isModBoss(BossBar bossBar) {
        String name = bossBar.getName().getString();
        return name.contains("Goddess") || name.contains("Dark God") || name.contains("虚空女神") ||
                name.contains("冥王残魂") || name.equals(" ");
    }

    @Unique
    private boolean isModName(String name) {
        return name.contains("Goddess") || name.contains("Dark God") || name.contains("虚空女神") ||
                name.contains("冥王残魂") || name.equals(" ");
    }
}