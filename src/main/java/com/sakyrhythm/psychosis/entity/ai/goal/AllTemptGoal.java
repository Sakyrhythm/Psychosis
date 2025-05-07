package com.sakyrhythm.psychosis.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public class AllTemptGoal extends Goal {
    private static final TargetPredicate TEMPTING_ENTITY_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(10.0).ignoreVisibility();
    private final TargetPredicate predicate;
    protected final PathAwareEntity mob;
    private final double speed;
    @Nullable
    protected PlayerEntity closestPlayer;

    public AllTemptGoal(PathAwareEntity entity, double speed) {
        this.mob = entity;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        // 允许任何玩家成为目标
        this.predicate = TEMPTING_ENTITY_PREDICATE.copy().setPredicate(livingEntity -> true);
    }
    public boolean canStart() {
        this.closestPlayer = this.mob.getWorld().getClosestPlayer(this.predicate, this.mob);
        return this.closestPlayer != null;
    }

    public boolean shouldContinue() {
        // 只要玩家存在且存活，继续移动
        return this.closestPlayer != null && this.closestPlayer.isAlive();
    }

    public void start() {
    }

    public void stop() {
        this.closestPlayer = null;
        this.mob.getNavigation().stop();
    }

    public void tick() {
        // 始终朝着玩家移动，无视距离
        if (this.closestPlayer != null) {
            this.mob.getLookControl().lookAt(this.closestPlayer, (float)(this.mob.getMaxHeadRotation() + 20), (float)this.mob.getMaxLookPitchChange());
            this.mob.getNavigation().startMovingTo(this.closestPlayer, this.speed);
        }
    }
}