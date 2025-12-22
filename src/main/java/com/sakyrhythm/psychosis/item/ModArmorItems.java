package com.sakyrhythm.psychosis.item;
import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.item.custom.ModArmorItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModArmorItems {
    public static final Item DIVINE_HELMET = registerItem("divine_helmet",new ModArmorItem(ModArmorMaterials.DIVINE, ArmorItem.Type.HELMET,
            new Item.Settings().fireproof().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(55))));
    public static final Item DIVINE_CHESTPLATE = registerItem("divine_chestplate",new ModArmorItem(ModArmorMaterials.DIVINE, ArmorItem.Type.CHESTPLATE,
            new Item.Settings().fireproof().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(55))));
    public static final Item DIVINE_LEGGINGS = registerItem("divine_leggings",new ModArmorItem(ModArmorMaterials.DIVINE, ArmorItem.Type.LEGGINGS,
            new Item.Settings().fireproof().maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(55))));
    public static final Item DIVINE_BOOTS = registerItem("divine_boots",new ModArmorItem(ModArmorMaterials.DIVINE, ArmorItem.Type.BOOTS,
            new Item.Settings().fireproof().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(55))));
    private static Item registerItem(String id, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Psychosis.MOD_ID,id), item);
    }
    public static void registerModItems(){
        Psychosis.LOGGER.info("Registering Items");
    }
}
