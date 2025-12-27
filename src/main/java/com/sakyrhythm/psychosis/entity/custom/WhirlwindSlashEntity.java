package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.entity.ModEntities; // 确保这是您注册实体的类
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// 假设 ModEntities 存在且已导入
public class WhirlwindSlashEntity extends Entity {

    // ⭐ 新增：TrackedData 用于同步缩放值
    private static final TrackedData<Float> SCALE_TRACKED =
            DataTracker.registerData(WhirlwindSlashEntity.class, TrackedDataHandlerRegistry.FLOAT);

    // 实体存在的最大刻数 (例如 0.25 秒)
    private static final int MAX_LIFE = 5;
    private int life = 0;

    // 【修正 1】：用于追踪创建者，以替代不存在的 setOwner
    @Nullable
    private Entity owner;

    // --------------------------------------------------------
    // 构造函数
    // --------------------------------------------------------

    // 1. 标准构造函数 (必须保留)
    public WhirlwindSlashEntity(EntityType<? extends WhirlwindSlashEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    // 2. 辅助构造函数：设置特效的创建者 (用于在 DarkSwordItem 中调用)
    // 【重要】请确保您在 ModEntities 中注册并公开了 WHIRLWIND_SLASH_ENTITY_TYPE
    public WhirlwindSlashEntity(World world, LivingEntity owner) {
        // 假设 ModEntities.WHIRLWIND_SLASH_ENTITY_TYPE 已正确注册并公开
        this(ModEntities.WHIRLWIND_SLASH_ENTITY_TYPE, world);
        this.setOwner(owner); // 调用我们新增的 setOwner 方法
    }

    // --------------------------------------------------------
    // 生命周期和逻辑
    // --------------------------------------------------------

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            // 服务器端逻辑：计数并自我销毁
            life++;
            if (life >= MAX_LIFE) {
                this.discard(); // 实体自毁
            }
        }
        // 客户端逻辑：动画由 Renderer 处理
    }

    // --------------------------------------------------------
    // 实体基本设置 (API 修正)
    // --------------------------------------------------------

    // 【修正 2】：覆盖正确的 initDataTracker 签名 (用于数据追踪器)
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // ⭐ 注册 SCALE_TRACKED，默认缩放为 1.0F
        builder.add(SCALE_TRACKED, 1.0F);
    }

    // 【新增】：供 DarkGodEntity 调用
    public void setScale(float scale) {
        // 仅在服务器端设置，客户端通过 TrackedData 接收
        if (!this.getWorld().isClient()) {
            this.dataTracker.set(SCALE_TRACKED, scale);
        }
    }

    // 【新增】：供 WhirlwindSlashRenderer 调用
    public float getScale() {
        if (this.dataTracker == null) return 1.0F; // 安全检查
        return this.dataTracker.get(SCALE_TRACKED);
    }

    // 【新增/修正】手动实现 setOwner 和 getOwner
    public void setOwner(@Nullable Entity owner) {
        this.owner = owner;
    }

    @Nullable
    public Entity getOwner() {
        return this.owner;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        // 如果需要保存缩放，NBT 逻辑应在这里实现
        if (nbt.contains("Scale")) {
            // 在 readCustomDataFromNbt 中设置 TrackedData 是安全的
            this.dataTracker.set(SCALE_TRACKED, nbt.getFloat("Scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        // 保存当前的缩放值
        nbt.putFloat("Scale", this.dataTracker.get(SCALE_TRACKED));
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}