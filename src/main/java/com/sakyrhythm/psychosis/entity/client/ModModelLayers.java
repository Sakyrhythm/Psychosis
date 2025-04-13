package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer PLAYER =
            new EntityModelLayer(Identifier.of(Psychosis.MOD_ID,"player"),"main");
}
