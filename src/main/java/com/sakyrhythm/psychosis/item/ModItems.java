package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    private static Item registerItem(String id, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Psychosis.MOD_ID,id), item);
    }
}
