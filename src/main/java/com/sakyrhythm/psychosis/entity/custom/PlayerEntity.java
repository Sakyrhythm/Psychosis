package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.entity.ai.goal.AllTemptGoal;
import com.sakyrhythm.psychosis.entity.client.PlayerRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义玩家实体类，继承自动物实体
 * 实现具有玩家皮肤a显示功能的NPC实体
 */
public class PlayerEntity extends AnimalEntity {

    // 跟踪玩家皮肤数据的NBT标签
    public static final TrackedData<NbtCompound> USE_SKIN = DataTracker.registerData(
            PlayerEntity.class,
            TrackedDataHandlerRegistry.NBT_COMPOUND
    );

    public PlayerEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
        // 可在此处添加每帧更新逻辑
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        // 初始化皮肤数据存储
        builder.add(USE_SKIN, new NbtCompound());
    }

    @Override
    protected void initGoals() {
        // 设置AI行为目标
        this.goalSelector.add(1, new AllTemptGoal(this,3D, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));  // 漫游行为
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));  // 注视玩家
        this.goalSelector.add(4, new LookAroundGoal(this));  // 环顾四周
    }

    /**
     * 创建实体属性配置
     */
    public static DefaultAttributeContainer.Builder createPlayerAttributes() {
        return AnimalEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 4.0);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        // 禁用繁殖功能
        return false;
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        // 禁用繁殖后代
        return null;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // 保存皮肤数据到NBT
        nbt.put("useSkin", getDataTracker().get(USE_SKIN));
        return super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        // 从NBT加载皮肤数据
        getDataTracker().set(USE_SKIN, nbt.getCompound("useSkin"));
        super.readNbt(nbt);
    }

    // 客户端专用功能区域
    @Environment(EnvType.CLIENT)
    private ClientGetters getter = null;

    /**
     * 获取客户端数据处理器
     * 示例召唤命令：
     * /summon psychosis:player ~ ~ ~ {useSkin:{id:[I;-744927312,2119846582,-1503445732,426072093]}}
     */
    @Environment(EnvType.CLIENT)
    public ClientGetters clientGetters() {
        return getter == null ? (getter = new ClientGetters()) : getter;
    }

    /**
     * 客户端数据获取工具类
     * 处理皮肤纹理的加载和解析
     */
    @Environment(EnvType.CLIENT)
    public class ClientGetters {
        private ProfileComponent profile = null;

        /**
         * 判断是否为细臂模型
         */
        public boolean isSlim() {
            SkinTextures textures = getSkinTextures();
            return textures != null && textures.model() == SkinTextures.Model.SLIM;
        }

        /**
         * 获取皮肤纹理标识
         */
        public Identifier getTexture() {
            SkinTextures textures = getSkinTextures();
            return textures == null ? PlayerRenderer.TEXTURE : textures.texture();
        }

        /**
         * 获取皮肤纹理数据
         * 包含异步加载和缓存机制
         */
        public @Nullable SkinTextures getSkinTextures() {
            // 解码NBT中的皮肤数据
            var result = ProfileComponent.CODEC.decode(NbtOps.INSTANCE, getDataTracker().get(USE_SKIN));
            if (!result.isSuccess()) return null;

            ProfileComponent trackProfile = result.getOrThrow().getFirst();

            // 初始化或更新缓存
            if (profile == null || !profile.gameProfile().equals(trackProfile.gameProfile())) {
                profile = trackProfile;
            }

            // 异步加载完整皮肤数据
            if (!profile.isCompleted()) {
                profile.getFuture().thenAcceptAsync(p ->
                                ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, p).ifSuccess(profileNbt -> {
                                    if (profileNbt instanceof NbtCompound compound) {
                                        getDataTracker().set(USE_SKIN, compound);
                                    }
                                }),
                        SkullBlockEntity.EXECUTOR
                );
            }

            // 从Minecraft皮肤系统获取纹理
            return MinecraftClient.getInstance().getSkinProvider()
                    .getSkinTextures(profile.gameProfile());
        }
    }
}