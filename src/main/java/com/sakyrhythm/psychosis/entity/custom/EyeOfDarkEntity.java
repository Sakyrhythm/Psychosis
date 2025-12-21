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

        // 调整初始推力，略微减速，确保转向平滑
        this.setVelocity(f * 0.03, 0.4, g * 0.03);
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
        double g = vec3d.horizontalLength();

        this.setPitch(customUpdateRotation(this.prevPitch, (float)(MathHelper.atan2(vec3d.y, g) * (double)(180F / (float)Math.PI))));
        this.setYaw(customUpdateRotation(this.prevYaw, (float)(MathHelper.atan2(vec3d.x, vec3d.z) * (double)(180F / (float)Math.PI))));

        if (!this.getWorld().isClient) {
            double h = this.targetX - d;
            double i = this.targetZ - f;
            float j = (float)Math.sqrt(h * h + i * i);
            float k = (float)MathHelper.atan2(i, h);

            // 飞行逻辑调整：使用 0.0035 因子，略快于原版，增强追踪性
            double l = MathHelper.lerp(0.0035, g, (double)j);
            double m = vec3d.y;

            if (j < 1.0F) {
                l *= 0.8;
                m *= 0.8;
            }

            int n = this.getY() < this.targetY ? 1 : -1;
            vec3d = new Vec3d(Math.cos((double)k) * l, m + ((double)n - m) * 0.015D, Math.sin((double)k) * l);
            this.setVelocity(vec3d);
        }

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

            // 延长生命周期：从 80 增加到 100 ticks (5秒)
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