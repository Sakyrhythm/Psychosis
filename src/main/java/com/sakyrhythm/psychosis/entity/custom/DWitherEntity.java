// DWitherEntity.java
package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SkinOverlayOwner;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;
import net.minecraft.util.math.random.Random;
import java.util.function.Predicate;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;

public class DWitherEntity extends HostileEntity implements SkinOverlayOwner, RangedAttackMob  {

    private static final Predicate<LivingEntity> CAN_ATTACK_PREDICATE;
    private static final TrackedData<Integer> INVUL_TIMER =
            DataTracker.registerData(DWitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private int attackSequenceCounter = -1;
    private final int[] skullCooldowns = new int[2];

    public DWitherEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world);
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(true);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }

    public static DefaultAttributeContainer.Builder createDegenerateWitherAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(INVUL_TIMER, 0);
    }

    public int getInvulnerableTimer() {
        return this.dataTracker.get(INVUL_TIMER);
    }

    public void setInvulTimer(int ticks) {
        this.dataTracker.set(INVUL_TIMER, ticks);
    }


    @Override
    protected void mobTick() {
        if (this.getInvulnerableTimer() > 0) {
            this.setInvulTimer(this.getInvulnerableTimer() - 1);
            if (this.age % 10 == 0) {
                this.heal(1.0F);
            }
        } else {
            super.mobTick();

            // 粒子效果
            if (!this.getWorld().isClient && this.isAlive()) {
                this.getWorld().addParticle(ParticleTypes.SMOKE,
                        this.getX() + this.random.nextGaussian() * 0.3,
                        this.getY() + this.random.nextGaussian() * 0.3,
                        this.getZ() + this.random.nextGaussian() * 0.3,
                        0.0, 0.0, 0.0);
            }

            // 攻击逻辑 (只保留主头)
            LivingEntity target = this.getTarget();
            if (target != null && this.canTarget(target)) {
                if (this.age % 40 == 0) {
                    // 当冷却结束时，启动攻击序列，从0开始计数
                    attackSequenceCounter = 0;
                }

                // 处理快速攻击的逻辑
                // 当 attackSequenceCounter 大于等于0，且小于10时，执行攻击
                if (attackSequenceCounter >= 0 && attackSequenceCounter < 10) {
                    // 在每个游戏刻内，发射一个弹射物
                    this.shootSkullWithOffset(target,0.3F, (Random) this.random);
                    // 每次攻击后，将计数器加1
                    attackSequenceCounter++;

                    // 可以在这里添加粒子效果，以表明攻击正在进行
                    ((ServerWorld) getWorld()).spawnParticles(
                            ParticleTypes.CLOUD,
                            getX(), getY() + 0.5, getZ(),
                            1, // 每次只生成一个粒子
                            0.1, 0.1, 0.1,
                            0.0
                    );
                }
            }
        }
    }


    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!this.getWorld().isClient) {
            ItemStack witherSkull = new ItemStack(Items.WITHER_SKELETON_SKULL, 1);
            this.dropItem(witherSkull.getItem());

            int coalCount = 3 + this.getRandom().nextInt(3);
            ItemStack coal = new ItemStack(Items.COAL, coalCount);
            for (int i = 0; i < coalCount; i++)
                this.dropItem(coal.getItem());

            int boneCount = 2 + this.getRandom().nextInt(5);
            ItemStack bone = new ItemStack(Items.BONE, boneCount);
            for (int i = 0; i < boneCount; i++)
                this.dropItem(bone.getItem());
            int voidCount = 5 + this.getRandom().nextInt(5);
            ItemStack void_essence = new ItemStack(ModItems.VOID_ESSENCE, voidCount);
            for (int i = 0; i < voidCount; i++)
                this.dropItem(void_essence.getItem());
        }
        //if (!this.getWorld().isClient) {
        //    this.getWorld().createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, ExplosionSourceType.MOB);
        //}
    }
    /**
     * 朝向目标发射一个凋灵之首式的弹射物，并添加随机偏移。
     * @param target 目标实体。
     * @param offsetPercentage 偏移量百分比，例如 0.3f 代表 30% 的偏移。
     * @param randomSource 一个 Random 实例，用于生成随机数。
     */
    private void shootSkullWithOffset(LivingEntity target, float offsetPercentage, Random randomSource) {
        if (!this.isSilent()) {
            this.getWorld().syncWorldEvent(null, 1024, this.getBlockPos(), 0);
        }

        double d = this.getX();
        double e = this.getY() + 3.0D * (double)this.getScale();
        double f = this.getZ();

        double g = target.getX() - d;
        double h = target.getY() + (double)target.getStandingEyeHeight() * 0.5D - e;
        double i = target.getZ() - f;

        // 添加随机角度偏移
        // 偏移量由 offsetPercentage 决定
        g += randomSource.nextFloat() * (double)offsetPercentage - (double)offsetPercentage / 2.0;
        h += randomSource.nextFloat() * (double)offsetPercentage - (double)offsetPercentage / 2.0;
        i += randomSource.nextFloat() * (double)offsetPercentage - (double)offsetPercentage / 2.0;

        Vec3d vec3d = new Vec3d(g, h, i);

        // 凋灵之首的发射点偏移，使其不在一个精确点
        double spawnX = d + randomSource.nextFloat() * 1.5 - 0.75;
        double spawnY = e + randomSource.nextFloat() * 1.5 - 0.75;
        double spawnZ = f + randomSource.nextFloat() * 1.5 - 0.75;

        WitherSkullEntity witherSkullEntity = new WitherSkullEntity(this.getWorld(), this, vec3d.normalize());
        witherSkullEntity.setOwner(this);
        witherSkullEntity.setCharged(false);
        witherSkullEntity.setPos(spawnX, spawnY, spawnZ);

        this.getWorld().spawnEntity(witherSkullEntity);
    }

    protected void initGoals() {
        this.goalSelector.add(2, new ProjectileAttackGoal((RangedAttackMob) this, (double)1.0F, 40, 20.0F));
        this.goalSelector.add(10, new FlyGoal(this, (double)0.5F));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 0, false, false, CAN_ATTACK_PREDICATE));
    }

    @Override
    public boolean shouldRenderOverlay() {
        return false;
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {

    }
    static {
        CAN_ATTACK_PREDICATE = (entity) ->
                !entity.getType().isIn(EntityTypeTags.WITHER_FRIENDS) &&
                        entity.isMobOrPlayer() &&
                        // 🔥 新增排除逻辑
                        entity.getType() != ModEntities.DARK_GOD &&
                        entity.getType() != ModEntities.GODDESS;
    }
}