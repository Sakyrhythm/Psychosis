package com.sakyrhythm.psychosis.world.biome;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.biome.BiomeParticleConfig;
import net.minecraft.particle.ParticleTypes;

public class ModBiomes {
    public static final RegistryKey<Biome> DARK_BIOME = RegistryKey.of(RegistryKeys.BIOME,
            Identifier.of(Psychosis.MOD_ID, "dark_biome"));

    public static void globalOverWorldGeneration(GenerationSettings.LookupBackedBuilder builder) {
        DefaultBiomeFeatures.addLandCarvers(builder); // 洞穴,峡谷
        DefaultBiomeFeatures.addDefaultOres(builder); // 矿石

        /* 添加一些更适合黑暗主题的特性喵
        *DefaultBiomeFeatures.addDripstone(builder); // 应该是石笋什么的?
        *DefaultBiomeFeatures.addAmethystGeodes(builder); // 紫水晶矿洞,我很神秘
        *DefaultBiomeFeatures.addDesertDeadBushes(builder); // 死灌木
        */
    }

    // 私有方法，用于构建黑暗生物群系
    private static Biome DarkBiome(Registerable<Biome> context) {
        SpawnSettings.Builder spawnSettings = new SpawnSettings.Builder();

        // 移除和平生物的默认生成，因为这是黑暗之神的领地!!!!
        // 添加敌对生物~只保留一些更具威胁性的或符合主题的
        DefaultBiomeFeatures.addMonsters(spawnSettings, 95, 5, 100, false); // 默认怪物生成

        spawnSettings.spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(ModEntities.PLAYER, 20, 1, 3)); // 增加PlayerEntity生成，权重高

        // 666恶魂都来了
        spawnSettings.spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.ZOMBIE, 10, 4, 8));
        spawnSettings.spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.SKELETON, 10, 4, 8));
        spawnSettings.spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.SPIDER, 5, 2, 6));
        spawnSettings.spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.ENDERMAN, 20, 1, 2));
        spawnSettings.spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.GHAST, 30, 1, 1));

        GenerationSettings.LookupBackedBuilder generationSettings = new GenerationSettings.LookupBackedBuilder(
                context.getRegistryLookup(RegistryKeys.PLACED_FEATURE),
                context.getRegistryLookup(RegistryKeys.CONFIGURED_CARVER)
        );
        globalOverWorldGeneration(generationSettings);
        DefaultBiomeFeatures.addDesertDeadBushes(generationSettings);

        BiomeEffects.Builder biomeEffects = new BiomeEffects.Builder();
        biomeEffects
                .skyColor(0x1a001a)       // 天空颜色: 更深的暗紫色
                .fogColor(0x0f0a0f)       // 雾颜色: 几乎黑色，非常压抑
                .waterColor(0x0a000a)     // 水颜色: 极深的暗色，接近纯黑
                .waterFogColor(0x0a000a)  // 水雾颜色: 与水颜色相同
                .grassColor(0x283a28)     // 草地颜色: 更暗的墨绿色/灰绿色，显得不健康
                .foliageColor(0x283a28);  // 树叶颜色: 与草地相同

        biomeEffects.loopSound(SoundEvents.AMBIENT_CAVE);
        biomeEffects.moodSound(new BiomeMoodSound(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0));
        biomeEffects.particleConfig(new BiomeParticleConfig(ParticleTypes.ASH, 0.005f));

        Biome darkBiome = new Biome.Builder()
                .precipitation(true)
                .temperature(0.0f) //应该能结冰?
                .downfall(0.8f)
                .effects(biomeEffects.build())
                .spawnSettings(spawnSettings.build())
                .generationSettings(generationSettings.build())
                .build();
        return darkBiome;
    }

    public static void bootstrap(Registerable<Biome> context) {
        context.register(DARK_BIOME, DarkBiome(context));
        Psychosis.LOGGER.info("Registered custom biome: dark_biome - The Domain of the Dark God");
    }
}