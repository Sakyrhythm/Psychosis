package com.sakyrhythm.psychosis.entity;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.DWitherEntity;
import com.sakyrhythm.psychosis.entity.custom.EyeOfDarkEntity;
import com.sakyrhythm.psychosis.entity.custom.DarkDartProjectile;
import com.sakyrhythm.psychosis.entity.custom.FlatDartProjectile;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import com.sakyrhythm.psychosis.entity.custom.WhirlwindSlashEntity; // <-- 【重要】导入圆月斩特效实体类
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    // =========================================================================================
    // DarkDartProjectile 实体注册 (DarkSwordItem 的投掷物)
    // =========================================================================================

    public static final EntityType<DarkDartProjectile> DARK_DART_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "dark_dart_projectile"),
            EntityType.Builder.<DarkDartProjectile>create(DarkDartProjectile::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(8)
                    .trackingTickInterval(1)
                    .build()
    );

    // =========================================================================================
    // 【新增】FlatDartProjectile 实体注册 (扁平平面剑气投掷物)
    // =========================================================================================

    public static final EntityType<FlatDartProjectile> FLAT_DART = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "flat_dart"),
            EntityType.Builder.<FlatDartProjectile>create(FlatDartProjectile::new, SpawnGroup.MISC)
                    // 尺寸调整：使其非常扁平，以匹配渲染器的平面效果
                    .dimensions(0.5F, 0.05F)
                    .maxTrackingRange(16)
                    .trackingTickInterval(1)
                    .build()
    );

    // =========================================================================================
    // 【新增】WHIRLWIND_SLASH_ENTITY_TYPE 实体注册 (圆月斩动画特效实体)
    // =========================================================================================

    public static final EntityType<WhirlwindSlashEntity> WHIRLWIND_SLASH_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "whirlwind_slash"),
            // 使用 MISC (杂项) SpawnGroup，因为它是一个非交互、短时间存在的特效
            EntityType.Builder.<WhirlwindSlashEntity>create(WhirlwindSlashEntity::new, SpawnGroup.MISC)
                    // 尺寸应设置为动画效果的预估最大范围
                    .dimensions(3.0F, 1.5F) // 例如：水平半径 3 格，高度 1.5 格
                    .maxTrackingRange(32) // 确保玩家在远距离也能看到特效
                    .trackingTickInterval(1)
                    .build()
    );


    // =========================================================================================
    // 您已有的注册 (保持不变)
    // =========================================================================================

    public static final EntityType<EyeOfDarkEntity> EYE_OF_DARK = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "eye_of_dark"),
            EntityType.Builder.<EyeOfDarkEntity>create(EyeOfDarkEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build()
    );

    public static final EntityType<PlayerEntity> PLAYER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "player"),
            EntityType.Builder.create(PlayerEntity::new, SpawnGroup.CREATURE)
                    .disableSaving()
                    .dimensions(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .vehicleAttachment(net.minecraft.entity.player.PlayerEntity.VEHICLE_ATTACHMENT_POS)
                    .maxTrackingRange(32)
                    .trackingTickInterval(2)
                    .build());


    public static final EntityType<DWitherEntity> DEGENERATEWITHER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID,"degeneratewither"),
            EntityType.Builder.create(DWitherEntity::new, SpawnGroup.MONSTER)
                    .makeFireImmune()
                    .allowSpawningInside(Blocks.WITHER_ROSE)
                    .dimensions(0.9F, 3.5F)
                    .maxTrackingRange(10)
                    .build());

    // =========================================================================================
    // 属性和初始化 (保持不变)
    // =========================================================================================

    public static void registerAttributes() {
        DefaultAttributeContainer.Builder builder = DWitherEntity.createDegenerateWitherAttributes();

        builder.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D);

        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                DEGENERATEWITHER, builder);
    }

    public static void registerModEntities() {
        // 此方法调用会自动触发所有静态字段的初始化和注册。
    }
}