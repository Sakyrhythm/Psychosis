package com.sakyrhythm.psychosis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PsychosisModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
    }
}