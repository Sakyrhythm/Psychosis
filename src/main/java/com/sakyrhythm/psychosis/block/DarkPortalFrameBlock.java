package com.sakyrhythm.psychosis.block;

import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import com.sakyrhythm.psychosis.entity.custom.DarkGodEntity;
import com.sakyrhythm.psychosis.entity.custom.GoddessEntity;
import net.minecraft.block.*;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

public class DarkPortalFrameBlock extends Block {
    public static final MapCodec<DarkPortalFrameBlock> CODEC = createCodec(DarkPortalFrameBlock::new);
    public static final DirectionProperty FACING;
    public static final BooleanProperty EYE;
    protected static final VoxelShape FRAME_SHAPE;
    protected static final VoxelShape EYE_SHAPE;
    protected static final VoxelShape FRAME_WITH_EYE_SHAPE;
    private static BlockPattern COMPLETED_FRAME;

    public MapCodec<DarkPortalFrameBlock> getCodec() {
        return CODEC;
    }

    public DarkPortalFrameBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(EYE, false));
    }

    // ⭐ 修复后的状态替换监听：确保 EYE 变动时启动检查
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient) {
            // 如果方块本身没变，但 EYE 从 false 变成了 true
            if (state.isOf(newState.getBlock())) {
                if (!state.get(EYE) && newState.get(EYE)) {
                    // 启动 5 秒后的检查闹钟
                    world.scheduleBlockTick(pos, this, 100);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ⭐ 修复后的计划刻：扫描并自愈
    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(EYE)) {
            // 扩大检测范围到 100 格
            Box checkArea = new Box(pos).expand(1000.0);

            // 使用更通用的方法查找存活的 Boss
            boolean bossExists = world.getEntitiesByClass(DarkGodEntity.class, checkArea, Entity::isAlive).size() > 0
                    || world.getEntitiesByClass(GoddessEntity.class, checkArea, Entity::isAlive).size() > 0;

            if (!bossExists) {
                // 如果没找到 Boss，重置方块
                world.setBlockState(pos, state.with(EYE, false), Block.NOTIFY_ALL);
                // 发送一个小的视觉反馈（可选）
                world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
            } else {
                // 如果 Boss 还在，预约 5 秒后再次检查
                world.scheduleBlockTick(pos, this, 100);
            }
        }
    }

    // --- 以下代码保持不变 ---

    protected boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(EYE) ? FRAME_WITH_EYE_SHAPE : FRAME_SHAPE;
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(EYE, false);
    }

    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(EYE) ? 15 : 0;
    }

    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, EYE);
    }

    public static BlockPattern getCompletedFramePattern() {
        if (COMPLETED_FRAME == null) {
            COMPLETED_FRAME = BlockPatternBuilder.start().aisle(new String[]{"?vvv?", ">???<", ">???<", ">???<", "?^^^?"}).where('?', CachedBlockPosition.matchesBlockState(BlockStatePredicate.ANY)).where('^', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.SOUTH)))).where('>', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.WEST)))).where('v', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.NORTH)))).where('<', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.EAST)))).build();
        }
        return COMPLETED_FRAME;
    }

    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    static {
        FACING = HorizontalFacingBlock.FACING;
        EYE = Properties.EYE;
        FRAME_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);
        EYE_SHAPE = Block.createCuboidShape(4.0, 13.0, 4.0, 12.0, 16.0, 12.0);
        FRAME_WITH_EYE_SHAPE = VoxelShapes.union(FRAME_SHAPE, EYE_SHAPE);
    }
}