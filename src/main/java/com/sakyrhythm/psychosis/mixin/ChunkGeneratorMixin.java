package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.world.DarkBlockTracker;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    // 注入到 generateFeatures 方法执行完毕之后
    @Inject(method = "generateFeatures", at = @At("RETURN"))
    private void psychosis$scanDarkBlocksAfterFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci) {
        // 仅在服务器运行，且是 ServerWorld
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        DarkBlockTracker tracker = DarkBlockTracker.get(serverWorld);

        // 扫描 Chunk 内所有方块 (注意：这是在特征放置后运行)

        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        int minY = world.getBottomY();
        int maxY = world.getTopY();

        int addedCount = 0;

        // 遍历区块内的所有方块位置
        for (BlockPos pos : BlockPos.iterate(minX, minY, minZ, minX + 15, maxY - 1, minZ + 15)) {
            BlockState state = chunk.getBlockState(pos);

            if (state.isOf(ModBlocks.DARK_BLOCK) && !tracker.getAllPositions().contains(pos)) {
                // 如果是黑暗方块且追踪器中没有，就添加
                tracker.addPosition(pos.toImmutable());
                addedCount++;
            }
        }

        if (addedCount > 0) {
            // 仅在追踪到新方块时记录日志
            serverWorld.getServer().execute(() -> {
                tracker.markDirty();
                // 由于 ChunkGeneratorMixin 在世界加载时可能会对大量区块运行，这里不输出日志以避免刷屏
                // LOGGER.info("Scanned Chunk {}/{}: Added {} DarkBlocks.", chunkPos.x, chunkPos.z, addedCount);
            });
        }
    }
}