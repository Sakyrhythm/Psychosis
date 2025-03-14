package com.sakyrhythm.psychosis;

import com.sakyrhythm.psychosis.entity.client.HumanoidEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<HumanoidEntity> HUMANOID =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of("psychosis", "humanoid"),
                    FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, HumanoidEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                            .trackRangeBlocks(64)
                            .build()
            );

    public static void register() {
        // 注册实体属性
        FabricDefaultAttributeRegistry.register(
                HUMANOID,
                PathAwareEntity.createLivingAttributes()
                        .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
        );
    }
}