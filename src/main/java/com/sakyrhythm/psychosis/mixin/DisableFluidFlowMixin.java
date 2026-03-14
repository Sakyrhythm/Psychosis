package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public abstract class DisableFluidFlowMixin {
    @Inject(method = "canFlow", at = @At("HEAD"), cancellable = true)
    private void onCanFlow(BlockView world, BlockPos fluidPos, BlockState fluidState, Direction flowDirection,
                           BlockPos targetPos, BlockState targetState, FluidState targetFluidState, Fluid fluid,
                           CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof World worldInstance && Psychosis.isTheOcean(worldInstance)) {
            cir.setReturnValue(false); // 不许流动
        }
    }
    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void onScheduledTick(World world, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (Psychosis.isTheOcean(world)) {
            ci.cancel(); // 取消流体计算
        }
    }
}