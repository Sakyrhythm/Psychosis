package com.sakyrhythm.psychosis.entity;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.DWitherEntity;
import com.sakyrhythm.psychosis.entity.custom.EyeOfDarkEntity;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<EyeOfDarkEntity> EYE_OF_DARK = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "eye_of_dark"),
            // EyeOfDarkEntity::new 是实体构造函数引用
            EntityType.Builder.<EyeOfDarkEntity>create(EyeOfDarkEntity::new, SpawnGroup.MISC)
                    // 投掷实体使用较小的尺寸
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build()
    );
    public static final EntityType<PlayerEntity> PLAYER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "player"),
            EntityType.Builder.create(PlayerEntity::new, SpawnGroup.CREATURE)
                    .disableSaving()
                    //.disableSummon()
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
    public static void registerAttributes() {
        DefaultAttributeContainer.Builder builder = DWitherEntity.createDegenerateWitherAttributes();

        builder.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D);

        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                DEGENERATEWITHER, builder);
    }
}
