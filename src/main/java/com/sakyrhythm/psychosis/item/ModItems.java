package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import com.sakyrhythm.psychosis.item.DarkSwordItem;

public class ModItems {
    public static final Item VOID_ESSENCE = registerItem("void_essence", new Item(new Item.Settings()));
    public static final Item FLAT_DART = registerItem("flat_dart", new Item(new Item.Settings()));
    public static final Item Dark_EYE = registerItem("dark_eye", new DarkEyeItem(new Item.Settings()));
    public static final Item NoticedBottle = registerItem("noticed_bottle", new NoticedBottle(new Item.Settings()));
    public static final Item White_EYE = registerItem("white_eye", new Item(new Item.Settings()));
    public static final Item DIVINE_APPLE = registerItem("divine_apple", new Item((new Item.Settings()).rarity(Rarity.EPIC).food(ModFoodComponents.DIVINE_APPLE).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final Item HAPPYCANDY = registerItem("happycandy", new Item((new Item.Settings()).rarity(Rarity.UNCOMMON).food(ModFoodComponents.HAPPYCANDY)));
    public static final Item DARKSWORD = registerItem("darksword", new DarkSwordItem(new Item.Settings().rarity(Rarity.EPIC).attributeModifiers(DarkSwordItem.createAttributeModifiers(ModToolMaterials.DARK,3,-2.4f))));

    private static Item registerItem(String id, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Psychosis.MOD_ID,id), item);
    }
    public static void registerModItems(){
        Psychosis.LOGGER.info("Registering Items");
    }
}