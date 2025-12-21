package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item VOID_ESSENCE = registerItem("void_essence", new Item(new Item.Settings()));
    public static final Item Dark_EYE = registerItem("dark_eye", new DarkEyeItem(new Item.Settings()));
    private static Item registerItem(String id, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Psychosis.MOD_ID,id), item);
    }
    public static void registerModItems(){
        Psychosis.LOGGER.info("Registering Items");
    }
}
