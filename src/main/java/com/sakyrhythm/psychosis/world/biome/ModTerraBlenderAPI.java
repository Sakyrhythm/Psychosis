package com.sakyrhythm.psychosis.world.biome;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.world.biome.surface.ModMateriaRules;
import net.minecraft.util.Identifier;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

public class ModTerraBlenderAPI implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new ModOverworldRegion(Identifier.of(Psychosis.MOD_ID, "overworld"), 4));
        SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Psychosis.MOD_ID, ModMateriaRules.makeRules());
    }
}
