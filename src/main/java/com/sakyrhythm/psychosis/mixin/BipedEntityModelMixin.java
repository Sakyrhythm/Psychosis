package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> extends AnimalModel<T> {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void psychosis$applyUmbrellaHangingPose(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {

        if (!entity.isOnGround() && entity.getVelocity().y < -0.1) {

            ItemStack mainHand = entity.getMainHandStack();
            ItemStack offHand = entity.getOffHandStack();

            boolean holdingInRight = (mainHand.isOf(ModItems.UMBRELLA) && entity.getMainArm() == Arm.RIGHT)
                    || (offHand.isOf(ModItems.UMBRELLA) && entity.getMainArm() == Arm.LEFT);
            boolean holdingInLeft = (mainHand.isOf(ModItems.UMBRELLA) && entity.getMainArm() == Arm.LEFT)
                    || (offHand.isOf(ModItems.UMBRELLA) && entity.getMainArm() == Arm.RIGHT);
            if (holdingInRight || holdingInLeft) {
                float armUpPitch = -3.1F;
                if (holdingInRight) {
                    this.rightArm.pitch = armUpPitch;
                    this.rightArm.yaw = 0.0F;
                    this.rightArm.roll = 0.05F;
                }
                if (holdingInLeft) {
                    this.leftArm.pitch = armUpPitch;
                    this.leftArm.yaw = 0.0F;
                    this.leftArm.roll = -0.05F;
                }
                this.rightLeg.pitch = 0.15F;
                this.leftLeg.pitch = 0.15F;
                this.rightLeg.yaw = 0.0F;
                this.leftLeg.yaw = 0.0F;
                this.head.pitch = -0.4F;
                float swing = (float) Math.sin(animationProgress * 0.15F) * 0.04F;
                this.body.roll = swing;
                this.rightLeg.roll = swing;
                this.leftLeg.roll = swing;
                if (!holdingInRight) this.rightArm.pitch = 0.2F + (float) Math.cos(animationProgress * 0.1F) * 0.05F;
                if (!holdingInLeft) this.leftArm.pitch = 0.2F + (float) Math.cos(animationProgress * 0.1F) * 0.05F;
            }
        }
    }
}