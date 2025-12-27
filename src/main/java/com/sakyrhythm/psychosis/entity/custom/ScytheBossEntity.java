package com.sakyrhythm.psychosis.entity.custom;

import net.minecraft.entity.AnimationState; // 1. 必须导入这个类
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ScytheBossEntity extends HostileEntity {

    // 2. ⭐ 核心修改：定义公开的动画状态变量
    // 这个变量必须是 public，否则模型类 ScytheModel 无法通过 entity.animationState 访问它
    public final AnimationState animationState = new AnimationState();

    public ScytheBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true); // 禁用重力
    }

    @Override
    protected void initGoals() {
        // 不添加任何 Goal，AI 就会保持静止
    }

    public static DefaultAttributeContainer.Builder createScytheBossAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 400.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // 100% 抗击退
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);    // 移动速度 0
    }

    @Override
    public void travel(Vec3d pos) {
        // 覆盖位移逻辑，使其无法移动
        if (this.isLogicalSideForUpdatingMovement()) {
            this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
            this.setVelocity(Vec3d.ZERO);
        }
    }

    @Override
    public void tick() {
        // 3. ⭐ 核心修改：在客户端每一帧检查并启动动画状态
        if (this.getWorld().isClient()) {
            this.animationState.startIfNotRunning(this.age);
        }

        super.tick();

        // 强制锁死旋转角度，使其不会转头
        this.setYaw(0);
        this.setPitch(0);
        this.setBodyYaw(0);
        this.setHeadYaw(0);
        this.prevYaw = 0;
        this.prevPitch = 0;
        this.prevHeadYaw = 0;
    }

    @Override
    public boolean isPushable() {
        return false; // 不可被推挤
    }

    @Override
    protected void pushAway(net.minecraft.entity.Entity entity) {
        // 不会被挤走
    }
}