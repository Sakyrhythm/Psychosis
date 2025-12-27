package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.entity.ModEntities; // 确保这是您注册实体的类
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// 假设 ModEntities 存在且已导入
public class WhirlwindSlashEntity extends Entity {
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
        // 【重要】无法解析符号：您需要确保 ModEntities.WHIRLWIND_SLASH_ENTITY_TYPE 在您的 ModEntities 类中是 public static final 的 EntityType 实例
        // 如果您还没有注册，请先注册并替换这里的占位符。
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
        // 特效实体通常不需要数据追踪
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
        // ... (如果您需要保存 owner 或其他数据，在这里实现)
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        // ... (如果您需要保存 owner 或其他数据，在这里实现)
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