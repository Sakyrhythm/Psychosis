package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class FallenSwordEntity extends Entity {
    private static final TrackedData<Boolean> LANDED = DataTracker.registerData(FallenSwordEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private double targetY = 0;

    public FallenSwordEntity(EntityType<?> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public void setTargetY(double y) { this.targetY = y; }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LANDED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) {
            // 落地后的环境粒子（紫色魔法气息）
            if (this.dataTracker.get(LANDED) && this.random.nextInt(3) == 0) {
                this.getWorld().addParticle(ParticleTypes.WITCH, this.getX(), this.getY() + 0.3, this.getZ(), 0, 0.1, 0);
            }
            return;
        }

        if (!this.dataTracker.get(LANDED)) {
            // 极速下坠逻辑
            this.setPosition(this.getX(), this.getY() - 1.5, this.getZ());

            // 到达目标方块上方时停住
            if (this.getY() <= this.targetY) {
                this.setPosition(this.getX(), this.targetY, this.getZ());
                this.dataTracker.set(LANDED, true);
                this.onImpact();
            }
        }
    }

    private void onImpact() {
        ServerWorld world = (ServerWorld) this.getWorld();
        // 1. 爆炸视觉 (不破坏地形)
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 5, 0.2, 0.2, 0.2, 0);

        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 2.0f, 0.5f);
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.9f);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && this.dataTracker.get(LANDED)) {
            // ⭐ 赋予玩家 DarkSword 物品
            ItemStack sword = new ItemStack(ModItems.DARKSWORD);
            player.giveItemStack(sword);

            // 拾取音效
            this.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 1.2f);
            this.discard();
            return ActionResult.SUCCESS;
        }
        return ActionResult.SUCCESS;
    }

    // 存盘读取，确保存档后再进来剑还在地上
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(LANDED, nbt.getBoolean("Landed"));
        this.targetY = nbt.getDouble("TargetY");
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("Landed", this.dataTracker.get(LANDED));
        nbt.putDouble("TargetY", this.targetY);
    }

    @Override public boolean canHit() { return true; }
}