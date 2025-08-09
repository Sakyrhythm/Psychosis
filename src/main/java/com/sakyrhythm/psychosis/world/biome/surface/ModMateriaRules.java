package com.sakyrhythm.psychosis.world.biome.surface;

import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.world.biome.ModBiomes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;

public class ModMateriaRules {
    private static final MaterialRules.MaterialRule DARK_DIRT = makeRule(ModBlocks.DARK_DIRT);
    private static final MaterialRules.MaterialRule DARK_STONE = makeRule(ModBlocks.DARK_STONE);
    private static final MaterialRules.MaterialRule OBSIDIAN = makeRule(Blocks.OBSIDIAN);
    public static MaterialRules.MaterialRule makeRules() {
        MaterialRules.MaterialCondition isAtOrAboveWaterLevel = MaterialRules.water(-1, 0);
        MaterialRules.MaterialRule grassSurface = MaterialRules.sequence(MaterialRules.condition(isAtOrAboveWaterLevel,OBSIDIAN),DARK_STONE);
        return MaterialRules.sequence(
                MaterialRules.sequence(MaterialRules.condition(MaterialRules.biome(ModBiomes.DARK_BIOME),
                        MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR,DARK_DIRT)),
                        MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING,DARK_STONE)),
                MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR,grassSurface)
        );
    }

    private static MaterialRules.MaterialRule makeRule(Block block){
        return MaterialRules.block(block.getDefaultState());
    }

}
