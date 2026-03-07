package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup PSYCHOSIS_GROUP = Registry
            .register(Registries.ITEM_GROUP, Identifier.of(Psychosis.MOD_ID, "psychosis_group"), ItemGroup
                    .create(null, -1)
                    .displayName(Text.of("Psychosis Group")) // Changed display name for clarity
                    .icon(() -> new ItemStack(ModBlocks.DARK_BLOCK)) // Set an icon for the item group
                    .entries((displayContext, entries) -> {
                        entries.add(ModBlocks.DARK_STONE);
                        entries.add(ModBlocks.DARK_COBBLESTONE);
                        entries.add(ModBlocks.DARK_DIRT);
                        entries.add(ModBlocks.DARK_BLOCK); // Use add for a single block
                        entries.add(ModItems.VOID_ESSENCE);
                        entries.add(ModItems.DIVINECLOTH);
                        entries.add(ModItems.Dark_EYE);
                        entries.add(ModItems.NoticedBottle);
                        entries.add(ModItems.White_EYE);
                        entries.add(ModArmorItems.DIVINE_HELMET);
                        entries.add(ModArmorItems.DIVINE_CHESTPLATE);
                        entries.add(ModArmorItems.DIVINE_LEGGINGS);
                        entries.add(ModArmorItems.DIVINE_BOOTS);
                        entries.add(ModItems.HAPPYCANDY);
                        entries.add(ModItems.DIVINE_APPLE);
                        entries.add(ModItems.DARKSWORD);
                        entries.add(ModItems.UMBRELLA);
                    }).build());

    public static void registerModItemGroups() {
        Psychosis.LOGGER.info("Registering Mod Item Groups");
    }
}