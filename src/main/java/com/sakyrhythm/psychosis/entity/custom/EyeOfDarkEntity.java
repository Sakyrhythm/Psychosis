package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

// 继承自 Entity 并实现 FlyingItemEntity
public class EyeOfDarkEntity extends Entity implements FlyingItemEntity {

    // DataTracker 字段
    private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(EyeOfDarkEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final ItemStack DEFAULT_ITEM_STACK = new ItemStack(ModItems.Dark_EYE);

    // 恒定速度值，单位：方块/tick。原版 Eye of Ender 飞行时约为 1.5 ~ 1.8 / 20 = 0.075 ~ 0.09
    // 我们选择一个略快且恒定的速度，例如 0.15
    private static final double CONSTANT_SPEED = 0.15;

    private double targetX;
    private double targetY;
    private double targetZ;
    private int lifespan;
    private boolean dropsItem;

    public EyeOfDarkEntity(EntityType<? extends EyeOfDarkEntity> entityType, World world) {
        super(entityType, world);
        this.lifespan = 0;
    }

    public EyeOfDarkEntity(World world, double x, double y, double z) {
        this(ModEntities.EYE_OF_DARK, world);
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(ITEM, DEFAULT_ITEM_STACK);
    }

    public void setItem(ItemStack itemStack) {
        this.getDataTracker().set(ITEM, itemStack.copyWithCount(1));
    }

    @Override
    public ItemStack getStack() {
        return this.getDataTracker().get(ITEM);
    }

    public void initTargetPos(BlockPos pos) {
        double d = (double)pos.getX() + 0.5;
        int i = pos.getY();
        double e = (double)pos.getZ() + 0.5;
        double f = d - this.getX();
        double g = e - this.getZ();
        double h = Math.sqrt(f * f + g * g);

        if (h > 12.0D) {
            this.targetX = this.getX() + f / h * 12.0D;
            this.targetZ = this.getZ() + g / h * 12.0D;
            this.targetY = this.getY() + 8.0D;
        } else {
            this.targetX = d;
            this.targetY = (double)i;
            this.targetZ = e;
        }

        this.lifespan = 0;
        this.dropsItem = this.random.nextInt(5) > 0;

        // **修改点 1：初始速度**
        // 设置初始速度指向目标，并使用 CONSTANT_SPEED 的一部分作为初始推动力
        // 这样实体在 tick() 中能更快地被恒定速度逻辑接管。
        Vec3d initialDirection = new Vec3d(this.targetX - this.getX(), this.targetY - this.getY(), this.targetZ - this.getZ()).normalize();
        this.setVelocity(initialDirection.multiply(CONSTANT_SPEED * 0.5)); // 初始推力为恒定速度的一半
    }

    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            double d = Math.sqrt(x * x + z * z);
            this.setYaw((float)(MathHelper.atan2(x, z) * (double)(180F / (float)Math.PI)));
            this.setPitch((float)(MathHelper.atan2(y, d) * (double)(180F / (float)Math.PI)));
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
        }
    }

    @Override
    public void tick() {
        super.tick();
        Vec3d vec3d = this.getVelocity();
        double d = this.getX() + vec3d.x;
        double e = this.getY() + vec3d.y;
        double f = this.getZ() + vec3d.z;

        // **修改点 2：恒定速度飞行逻辑**
        if (!this.getWorld().isClient) {

            // 计算从实体当前位置到目标位置的方向向量
            double h = this.targetX - this.getX();
            double i = this.targetY - this.getY();
            double j = this.targetZ - this.getZ();

            // 计算与目标的距离的平方
            double distanceSq = h * h + i * i + j * j;

            if (distanceSq > 0.1) {
                // 如果距离目标足够远，继续追踪

                // 将方向向量标准化（单位化）
                Vec3d targetDirection = new Vec3d(h, i, j).normalize();

                // 将方向向量乘以恒定速度
                Vec3d targetVelocity = targetDirection.multiply(CONSTANT_SPEED);

                // 使用插值平滑地转向目标速度，factor 设为 0.1，可以平滑转向，避免过于生硬。
                double lerpFactor = 0.1;
                vec3d = vec3d.lerp(targetVelocity, lerpFactor);

                // 如果接近目标点，可以稍微减速以平滑停止
                if (distanceSq < 1.0) {
                    vec3d = vec3d.multiply(Math.sqrt(distanceSq)); // 距离越近，速度越慢
                }

            } else {
                // 距离目标非常近，停止移动
                vec3d = Vec3d.ZERO;
            }

            this.setVelocity(vec3d);
        }

        // 更新朝向
        double g = vec3d.horizontalLength();
        this.setPitch(customUpdateRotation(this.prevPitch, (float)(MathHelper.atan2(vec3d.y, g) * (double)(180F / (float)Math.PI))));
        this.setYaw(customUpdateRotation(this.prevYaw, (float)(MathHelper.atan2(vec3d.x, vec3d.z) * (double)(180F / (float)Math.PI))));

        // 粒子效果和生命周期逻辑保持不变
        if (this.isTouchingWater()) {
            for(int p = 0; p < 4; ++p) {
                this.getWorld().addParticle(ParticleTypes.BUBBLE, d - vec3d.x * 0.25D, e - vec3d.y * 0.25D, f - vec3d.z * 0.25D, vec3d.x, vec3d.y, vec3d.z);
            }
        } else {
            this.getWorld().addParticle(ParticleTypes.PORTAL,
                    d - vec3d.x * 0.25D + this.random.nextDouble() * 0.6 - 0.3,
                    e - vec3d.y * 0.25D - 0.5D,
                    f - vec3d.z * 0.25D + this.random.nextDouble() * 0.6 - 0.3,
                    vec3d.x, vec3d.y, vec3d.z
            );
        }

        if (!this.getWorld().isClient) {
            this.setPosition(d, e, f);
            ++this.lifespan;

            // 生命周期判断
            if (this.lifespan > 100) {
                this.playSound(SoundEvents.ENTITY_ENDER_EYE_DEATH, 1.0F, 1.0F);
                this.discard();

                if (this.dropsItem) {
                    this.dropStack(this.getStack());
                } else {
                    this.getWorld().syncWorldEvent(2003, this.getBlockPos(), 0);
                }
            }
        } else {
            this.setPos(d, e, f);
        }
    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("Item", this.getStack().encode(this.getRegistryManager()));
        nbt.putInt("LifeSpan", this.lifespan);
        nbt.putBoolean("DropsItem", this.dropsItem);
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("Item", 10)) {
            this.setItem(ItemStack.fromNbtOrEmpty(this.getRegistryManager(), nbt.getCompound("Item")));
        }
        this.lifespan = nbt.getInt("LifeSpan");
        this.dropsItem = nbt.getBoolean("DropsItem");
    }

    public float getBrightnessAtEyes() {
        return 1.0F;
    }

    public boolean isAttackable() {
        return false;
    }

    private static float customUpdateRotation(float prevAngle, float newAngle) {
        float f = MathHelper.wrapDegrees(newAngle - prevAngle);
        if (f > 45.0F) {
            f = 45.0F;
        }

        if (f < -45.0F) {
            f = -45.0F;
        }

        return prevAngle + f;
    }
}