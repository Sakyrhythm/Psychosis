package com.sakyrhythm.psychosis.entity.ai.goal;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class DarkGoal extends Goal {
    private static final TargetPredicate TEMPTING_ENTITY_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(10.0).ignoreVisibility();
    private final TargetPredicate predicate;
    protected final PathAwareEntity mob;
    private final double baseSpeed; // Base speed, unchanged.
    @Nullable
    protected PlayerEntity closestPlayer;

    public DarkGoal(PathAwareEntity entity, double speed) {
        this.mob = entity;
        this.baseSpeed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.predicate = TEMPTING_ENTITY_PREDICATE.copy().setPredicate(livingEntity -> true);
    }

    @Override
    public boolean canStart() {
        this.closestPlayer = this.mob.getWorld().getClosestPlayer(this.predicate, this.mob);
        return this.closestPlayer != null;
    }

    @Override
    public boolean shouldContinue() {
        return this.closestPlayer != null && this.closestPlayer.isAlive();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        this.closestPlayer = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.closestPlayer == null) return;

        this.mob.getLookControl().lookAt(this.closestPlayer, (float)(this.mob.getMaxHeadRotation() + 20), (float)this.mob.getMaxLookPitchChange());
        World world = this.mob.getWorld();
        int lightLevel = world.getLightLevel(this.mob.getBlockPos());

        double currentSpeed = this.baseSpeed;

        if (lightLevel > 13) {
            currentSpeed /= 2.0;

            if (this.mob.age % 5 == 0) {
                this.mob.damage(world.getDamageSources().onFire(), 1.0F); // 扣除0.5 颗心的火焰伤害
            }
        }

        this.mob.getNavigation().startMovingTo(this.closestPlayer, currentSpeed);
    }
}