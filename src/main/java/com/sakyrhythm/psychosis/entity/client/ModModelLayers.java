package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer MODEL_DEGENERATE_WITHER_LAYER =
            new EntityModelLayer(Identifier.of(Psychosis.MOD_ID, "degeneratewither"), "main");
    public static final EntityModelLayer PLAYER_STEVE =
            new EntityModelLayer(Identifier.of(Psychosis.MOD_ID,"player_steve"),"main");
    public static final EntityModelLayer PLAYER_SLIM =
            new EntityModelLayer(Identifier.of(Psychosis.MOD_ID,"player_slim"),"main");
}