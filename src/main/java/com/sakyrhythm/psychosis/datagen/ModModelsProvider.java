package com.sakyrhythm.psychosis.datagen;

import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.item.ModArmorItems;
import com.sakyrhythm.psychosis.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.data.family.BlockFamily;
import net.minecraft.item.ArmorItem;

public class ModModelsProvider extends FabricModelProvider {
    public ModModelsProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        //blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DARK_STONE);
        blockStateModelGenerator.registerSimpleState(ModBlocks.WHISPERING_SHELL);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.registerArmor((ArmorItem) ModArmorItems.DIVINE_HELMET);
        itemModelGenerator.registerArmor((ArmorItem) ModArmorItems.DIVINE_CHESTPLATE);
        itemModelGenerator.registerArmor((ArmorItem) ModArmorItems.DIVINE_LEGGINGS);
        itemModelGenerator.registerArmor((ArmorItem) ModArmorItems.DIVINE_BOOTS);

    }
}