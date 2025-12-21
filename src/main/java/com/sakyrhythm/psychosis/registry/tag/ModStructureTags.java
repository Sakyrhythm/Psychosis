package com.sakyrhythm.psychosis.registry.tag;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

public interface ModStructureTags {
    TagKey<Structure> DARK_EYE_LOCATED = of("dark");

    private static TagKey<Structure> of(String id) {
        return TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(Psychosis.MOD_ID, id));
    }
}
