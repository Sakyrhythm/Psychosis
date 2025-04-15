package com.sakyrhythm.psychosis.entity;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<PlayerEntity> PLAYER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(Psychosis.MOD_ID, "player"),
            EntityType.Builder.create(PlayerEntity::new, SpawnGroup.CREATURE).dimensions(0.6f, 1.8f).build());
}
