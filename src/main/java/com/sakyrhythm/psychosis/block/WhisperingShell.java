package com.sakyrhythm.psychosis.block;

import com.sakyrhythm.psychosis.block.entity.WhisperingShellBlockEntity;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.TentacleEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class WhisperingShell extends Block implements Waterloggable, BlockEntityProvider {

    // 引入含水属性
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public static final VoxelShape SHAPE = Stream.of(
            // ... 你原本极其复杂的 VoxelShape 保持不变 ...
            Block.createCuboidShape(8, 12, 2, 9, 13, 12),
            Block.createCuboidShape(1, 0, 1, 15, 2, 15),
            Block.createCuboidShape(0, 2, 0, 16, 4, 16),
            Block.createCuboidShape(0.001, 4, 7, 5.001, 8, 15),
            Block.createCuboidShape(0, 4, 2, 1, 5, 13),
            Block.createCuboidShape(0.0001, 7, 9.001, 6.000100000000001, 10, 14.001),
            Block.createCuboidShape(0.999, 8, 9, 11.999, 14, 13),
            Block.createCuboidShape(6.9999, 13.0001, 2.0003, 12.9999, 15.0001, 12.0003),
            Block.createCuboidShape(1, 14, 6, 13, 15, 12),
            Block.createCuboidShape(1.9999, 15.0001, 4.0003, 11.9999, 17.0001, 12.0003),
            Block.createCuboidShape(3, 15, 4, 12, 16, 12),
            Block.createCuboidShape(1, 15, 2, 9, 17, 8),
            Block.createCuboidShape(2, 17, 3, 9, 18, 9),
            Block.createCuboidShape(3, 18, 4, 7, 19, 8),
            Block.createCuboidShape(8, 13, 2, 13, 14, 12),
            Block.createCuboidShape(1, 12, 7, 12, 14, 12),
            Block.createCuboidShape(3, 11, 8, 12, 12, 9),
            Block.createCuboidShape(2, 14, 3, 7, 15, 6),
            Block.createCuboidShape(1, 4, 0, 3, 6, 7),
            Block.createCuboidShape(3, 6, 0, 4, 7, 5),
            Block.createCuboidShape(6, 6, 0, 9, 8, 5),
            Block.createCuboidShape(4, 4, -1, 8, 7, 5),
            Block.createCuboidShape(5, 4, 4, 7, 6, 6),
            Block.createCuboidShape(3, 4, 0, 5, 6, 8),
            Block.createCuboidShape(7, 4, 0.001, 15, 7, 15.001),
            Block.createCuboidShape(14, 4, 1, 16, 7, 14),
            Block.createCuboidShape(7, 4.0001, -1.001, 12, 7.0001, 4.9990000000000006),
            Block.createCuboidShape(6, 8.001, 0.001, 13, 9.001, 4.001),
            Block.createCuboidShape(8, 6, 1, 13, 9, 5),
            Block.createCuboidShape(9, 6, 0, 14, 9, 4),
            Block.createCuboidShape(4, 4, 8, 12, 9, 16),
            Block.createCuboidShape(3, 4, 13, 12, 7, 17),
            Block.createCuboidShape(11, 4, 13.0001, 14, 8, 16.0001),
            Block.createCuboidShape(2.0003, 7, 10, 15.0003, 12, 14),
            Block.createCuboidShape(12, 6, 12.001, 14, 10, 15.001),
            Block.createCuboidShape(9, 6, 9, 13, 11, 15),
            Block.createCuboidShape(9, 6, 1.001, 15, 10, 14.001),
            Block.createCuboidShape(9, 6, 1, 13, 12, 2),
            Block.createCuboidShape(11.001, 6, 3, 15.001, 9, 13),
            Block.createCuboidShape(9, 10, 2, 14, 13, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public WhisperingShell(Settings settings) {
        super(settings);
        // 初始化默认状态为含水
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, true));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // 核心：强制返回水流状态。如果你希望它“永远”含水，直接返回 Fluids.WATER.getStill(false)
    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getStill(false);
    }

    // 处理邻居更新，确保水流物理生效
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    // 放置方块时的逻辑：强制设为含水
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(WATERLOGGED, true);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
    // 在 WhisperingShell 类中添加/修改以下代码

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WhisperingShellBlockEntity(pos, state);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // 只在客户端执行粒子效果
        if (!world.isClient) return;

        // 1. 找到水面的基准位置 (从方块位置向上寻找水面)
        BlockPos.Mutable surfacePos = pos.mutableCopy();
        while (world.getFluidState(surfacePos.up()).isStill()) {
            surfacePos.move(Direction.UP);
            // 防止无限循环 (最大向上寻找24格)
            if (surfacePos.getY() >= world.getTopY() || surfacePos.getY() - pos.getY() > 24) break;
        }

        // 只有当水面顶部确实是空气时，才产生粒子
        BlockState surfaceBlockState = world.getBlockState(surfacePos.up());
        if (!surfaceBlockState.isAir()) return;

        // ========== 核心参数调整 ==========
        // 1. 单 tick 生成数量: 提高到 200 (视觉效果非常密集，如果觉得卡顿可以降到100)
        int particleCount = 100;
        // 2. 生成范围半径: 扩大到 5.0 格，让整个水面区域沸腾
        double range = 12.0;
        // 3. 气泡上升速度: 增加随机性，看起来更自然
        // ================================

        for (int i = 0; i < particleCount; i++) {
            // 在水平面上随机偏移
            double offsetX = (random.nextDouble() - 0.5) * range * 2; // 范围: -range 到 +range
            double offsetZ = (random.nextDouble() - 0.5) * range * 2;

            // 计算目标检查点的位置
            BlockPos checkPos = BlockPos.ofFloored(
                    surfacePos.getX() + offsetX,
                    surfacePos.getY(),
                    surfacePos.getZ() + offsetZ
            );

            // 检查该点是否也是水源方块 (确保粒子只生成在水面上)
            if (world.getFluidState(checkPos).isStill() && world.getBlockState(checkPos.up()).isAir()) {

                // 粒子的精确位置 (随机微调，避免完全对齐网格)
                double px = (double) checkPos.getX() + 0.3 + random.nextDouble() * 0.4; // 0.3~0.7 范围
                double py = (double) checkPos.getY() + 0.9 + random.nextDouble() * 0.2; // 水面稍微往上一点
                double pz = (double) checkPos.getZ() + 0.3 + random.nextDouble() * 0.4;

                // 🎯 生成主要气泡粒子
                // 速度: Y轴速度稍快且随机，X/Z有微小扰动
                double velX = (random.nextDouble() - 0.5) * 0.02;
                double velY = 0.25 + random.nextDouble() * 0.2; // 上升速度随机
                double velZ = (random.nextDouble() - 0.5) * 0.02;
                world.addParticle(ParticleTypes.BUBBLE, px, py, pz, velX, velY, velZ);

                // 🎯 增加"泡泡破裂"效果 (30%的概率)
                if (random.nextFloat() < 0.3f) {
                    world.addParticle(ParticleTypes.BUBBLE_POP, px, py + 0.1, pz, 0.0, 0.01, 0.0);
                }

                // 🎯 增加"水花飞溅"效果 (15%的概率)
                if (random.nextFloat() < 0.15f) {
                    world.addParticle(ParticleTypes.SPLASH, px, py + 0.1, pz, 0.0, 0.1, 0.0);
                }

                // 🎯 额外: 生成一些"水下烟雾"或"灵魂"效果来模拟深海热泉 (5%的概率)
                if (random.nextFloat() < 0.05f) {
                    world.addParticle(ParticleTypes.SOUL,
                            px, py - 0.5, pz, // 从水面下一点开始
                            0.0, 0.2, 0.0);
                }
            }
        }

        // 🎯 特殊效果: 每 tick 有 10% 的概率在中心生成一个大团气泡
        if (random.nextFloat() < 0.1f) {
            // 在中心点生成一团密集气泡
            double centerX = surfacePos.getX() + 0.5;
            double centerY = surfacePos.getY() + 0.95;
            double centerZ = surfacePos.getZ() + 0.5;
            for (int j = 0; j < 20; j++) {
                double smallOffsetX = (random.nextDouble() - 0.5) * 1.5;
                double smallOffsetZ = (random.nextDouble() - 0.5) * 1.5;
                world.addParticle(ParticleTypes.BUBBLE,
                        centerX + smallOffsetX, centerY + random.nextDouble() * 0.3, centerZ + smallOffsetZ,
                        0.0, 0.3 + random.nextDouble() * 0.2, 0.0);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // 检查类型是否匹配
        if (type != ModEntities.WHISPERING_SHELL_BE) {
            return null;
        }
        return (world1, pos1, state1, blockEntity) ->
                WhisperingShellBlockEntity.tick(world1, pos1, state1, (WhisperingShellBlockEntity) blockEntity);
    }
}