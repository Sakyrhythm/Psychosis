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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // 1.21 手动 Lambda 写法，解决 checkType/validateTicker 报错
        return world.isClient ? null : (type == ModEntities.WHISPERING_SHELL_BE ?
                (world1, pos1, state1, blockEntity) -> WhisperingShellBlockEntity.tick(world1, pos1, state1, (WhisperingShellBlockEntity) blockEntity) : null);
    }
}