package com.sakyrhythm.psychosis.entity.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class NailEntity extends Entity {
    private Vec3d targetPos;
    private int lifeTicks = 0;
    private float damage = 10.0f;

    public NailEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setTarget(double x, double y, double z) {
        this.targetPos = new Vec3d(x, y, z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();
        this.prevPitch = this.getPitch();
        this.prevYaw = this.getYaw();

        super.tick();

        if (targetPos != null) {
            Vec3d currentPos = this.getPos();
            // 1. 核心方向向量：目标 - 当前
            Vec3d dir = targetPos.subtract(currentPos).normalize();

            // 2. 更加丧心病狂的加速度
            // 初始速度 0.3，每滴增加 0.1 * T^2
            double baseSpeed = 0.3;
            double accelerationRate = 0.12;
            double currentSpeed = baseSpeed + (accelerationRate * lifeTicks * lifeTicks);
            currentSpeed = Math.min(currentSpeed, 10.0); // 最高速度提升到 10.0 (每秒200格)

            this.setVelocity(dir.multiply(currentSpeed));

            // 3. 旋转计算
            updateRotation();
        }

        this.move(MovementType.SELF, this.getVelocity());
        lifeTicks++;

        if (!this.getWorld().isClient()) {
            checkEntityCollision();
            // 碰撞到任何方块立刻消失
            if (this.horizontalCollision || this.verticalCollision || this.isOnGround() ||
                    (targetPos != null && this.getPos().distanceTo(targetPos) < 0.5) || lifeTicks > 200) {
                this.discard();
            }
        }
    }

    private void checkEntityCollision() {
        // 稍微扩大检测范围，适配高速移动
        List<Entity> targets = this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(0.3));
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                DamageSource source = this.getDamageSources().magic();
                if (living.damage(source, this.damage)) {
                    this.discard(); // 击中生物后消失
                }
            }
        }
    }



    private void updateRotation() {
        Vec3d v = this.getVelocity();
        // 计算水平面上的向量长度
        double hLength = Math.sqrt(v.x * v.x + v.z * v.z);

        // 使用传统的 180 / PI 进行转换，绕过无法解析的常量
        float newYaw = (float) (MathHelper.atan2(v.x, v.z) * (180.0 / Math.PI));
        float newPitch = (float) (MathHelper.atan2(v.y, hLength) * (180.0 / Math.PI));

        this.setYaw(newYaw);
        this.setPitch(newPitch);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("TargetX")) {
            this.targetPos = new Vec3d(nbt.getDouble("TargetX"), nbt.getDouble("TargetY"), nbt.getDouble("TargetZ"));
        }
        this.lifeTicks = nbt.getInt("LifeTicks");
        this.damage = nbt.getFloat("Damage");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (targetPos != null) {
            nbt.putDouble("TargetX", targetPos.x);
            nbt.putDouble("TargetY", targetPos.y);
            nbt.putDouble("TargetZ", targetPos.z);
        }
        nbt.putInt("LifeTicks", this.lifeTicks);
        nbt.putFloat("Damage", this.damage);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return new EntitySpawnS2CPacket(this, entityTrackerEntry);
    }
}