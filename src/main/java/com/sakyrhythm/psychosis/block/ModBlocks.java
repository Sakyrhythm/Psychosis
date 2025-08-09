package com.sakyrhythm.psychosis.block;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block DARK_STONE = register("dark_stone", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(1.5F, 6.0F)));
    public static final Block DARK_COBBLESTONE = register("dark_cobblestone", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(2.0F, 6.0F)));
    public static final Block DARK_DIRT = register("dark_dirt", new Block(AbstractBlock.Settings.create().mapColor(MapColor.DIRT_BROWN).strength(0.5F).sounds(BlockSoundGroup.GRAVEL)));
    public static final Block DARK_BLOCK = register("dark_portal_frame", new DarkPortalFrameBlock(AbstractBlock.Settings
            .create()
            .mapColor(MapColor.GOLD)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sounds(BlockSoundGroup.GLASS)
            .luminance((state) -> {
                return 1;
            }).strength(-1.0F, 3600000.0F).dropsNothing()));

    public static void registerBlockItems(String id, Block block) {
        Item item = Registry.register(Registries.ITEM, Identifier.of(Psychosis.MOD_ID, id), new BlockItem(block, new Item.Settings()));
        if (item instanceof BlockItem) {
            ((BlockItem)item).appendBlocks(Item.BLOCK_ITEMS, item);
        }
    }

    public static Block register(String id, Block block) {
        registerBlockItems(id, block);
        return Registry.register(Registries.BLOCK, Identifier.of(Psychosis.MOD_ID, id), block);
    }

    public static void registerModBlocks() {
        Psychosis.LOGGER.info("Registering ModBlocks");
    }
}