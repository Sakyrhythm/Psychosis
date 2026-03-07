package com.sakyrhythm.psychosis.config;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("psychosis.properties");
    private static final Properties PROPERTIES = new Properties();

    // 配置项定义
    public static boolean enableRainSlowness = true;

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            PROPERTIES.load(in);
            enableRainSlowness = Boolean.parseBoolean(PROPERTIES.getProperty("enableRainSlowness", "true"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        PROPERTIES.setProperty("enableRainSlowness", String.valueOf(enableRainSlowness));
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            PROPERTIES.store(out, "Psychosis Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}