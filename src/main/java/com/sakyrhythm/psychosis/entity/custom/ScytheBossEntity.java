package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.entity.ModEntities;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ScytheBossEntity extends HostileEntity {
    public final AnimationState animationState = new AnimationState();
    private int attackTimer = 0;

    // 使用 WHITE 作为自定义 DARK 纹理的信号
    private final ServerBossBar bossBar = (ServerBossBar) new ServerBossBar(
            Text.translatable("entity.psychosis.scythe"),
            BossBar.Color.WHITE,
            BossBar.Style.PROGRESS
    ).setDarkenSky(true);

    private int vulnerableTimer = 0;

    public ScytheBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    public static DefaultAttributeContainer.Builder createScytheBossAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 450.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getSource() instanceof NailEntity) {
            this.vulnerableTimer = 40;
            // 破防时变回原版红色
            this.bossBar.setColor(BossBar.Color.RED);

            this.hurtTime = 0;
            this.timeUntilRegen = 0;
            return super.damage(source, amount);
        }

        if (vulnerableTimer > 0) {
            return super.damage(source, amount);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.getSource() instanceof NailEntity || vulnerableTimer > 0) return false;
        return true;
    }

    @Override
    public void tick() {
        if (this.getWorld().isClient()) this.animationState.startIfNotRunning(this.age);
        super.tick();

        this.setYaw(0); this.setPitch(0);
        this.setBodyYaw(0); this.setHeadYaw(0);

        if (!this.getWorld().isClient()) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

            if (vulnerableTimer > 0) {
                vulnerableTimer--;
                if (vulnerableTimer <= 0) {
                    // 恢复无敌，变回自定义 DARK (信号为白色)
                    this.bossBar.setColor(BossBar.Color.WHITE);
                }
            }

            attackTimer++;
            if (attackTimer >= 100) {
                spawnNailAtTarget();
                attackTimer = 0;
            }
        }
    }

    private void spawnNailAtTarget() {
        net.minecraft.entity.player.PlayerEntity player = this.getWorld().getClosestPlayer(this, 60.0);
        if (player == null) return;

        NailEntity nail = new NailEntity(ModEntities.NAIL, this.getWorld());
        Random r = this.getRandom();
        double angle = r.nextDouble() * Math.PI * 2.0;
        double distance = 6.0;
        double spawnX = this.getX() + Math.cos(angle) * distance;
        double spawnZ = this.getZ() + Math.sin(angle) * distance;

        nail.refreshPositionAndAngles(spawnX, this.getY()+2, spawnZ, 0.0F, 0.0F);
        nail.setTargetEntity(player);
        this.getWorld().spawnEntity(nail);
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override public void travel(Vec3d pos) { this.setVelocity(Vec3d.ZERO); }
    @Override public boolean isPushable() { return false; }
}