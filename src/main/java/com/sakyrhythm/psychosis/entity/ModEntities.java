package com.sakyrhythm.psychosis.entity;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.block.entity.WhisperingShellBlockEntity;
import com.sakyrhythm.psychosis.entity.custom.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
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
    public static final BlockEntityType<WhisperingShellBlockEntity> WHISPERING_SHELL_BE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("psychosis", "whispering_shell_be"),
            BlockEntityType.Builder.create(WhisperingShellBlockEntity::new, ModBlocks.WHISPERING_SHELL).build()
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
// 【新增】NAIL 实体注册 (判罚之钉)
// =========================================================================================

    public static final EntityType<NailEntity> NAIL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "nail"),
            EntityType.Builder.<NailEntity>create(NailEntity::new, SpawnGroup.MISC)
                    .dimensions(1F, 1F)   // 细长钉子
                    .maxTrackingRange(32)
                    .trackingTickInterval(1)
                    .build()
    );

    // =========================================================================================
// 【新增】SCYTHE_BOSS 实体注册 (镰刀Boss)
// =========================================================================================

    public static final EntityType<ScytheBossEntity> SCYTHE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(Psychosis.MOD_ID, "scythe"),
                    EntityType.Builder.create(ScytheBossEntity::new, SpawnGroup.MONSTER)
                            .dimensions(1.2f, 4.5f)
                            .makeFireImmune()
                            .maxTrackingRange(64)
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

    public static final EntityType<DarkGodEntity> DARK_GOD =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(Psychosis.MOD_ID, "dark_god"),
                    EntityType.Builder.create(DarkGodEntity::new,SpawnGroup.MONSTER)
                            .dimensions(1.5f, 3.0f)
                            .makeFireImmune()
                            .build()
            );
    // 在 ModEntities 类中添加以下字段
    public static final EntityType<TentacleEntity> TENTACLE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "tentacle"),
            EntityType.Builder.<TentacleEntity>create(TentacleEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(32)
                    .trackingTickInterval(1)
                    .build()
    );
    public static final EntityType<GoddessEntity> GODDESS =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(Psychosis.MOD_ID, "goddess"),
                    EntityType.Builder.create(GoddessEntity::new,SpawnGroup.MONSTER)
                            .dimensions(1.5f, 3.0f)
                            .makeFireImmune()
                            .build()
            );
    public static void registerAttributes() {
        DefaultAttributeContainer.Builder builder = DWitherEntity.createDegenerateWitherAttributes();

        builder.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D);

        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                DEGENERATEWITHER, builder);
        FabricDefaultAttributeRegistry.register(
                DARK_GOD, DarkGodEntity.createDarkGodAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                GODDESS, GoddessEntity.createGoddessBossAttributes()
        );
    }

    public static void registerModEntities() {
        // 此方法调用会自动触发所有静态字段的初始化和注册。
    }
}