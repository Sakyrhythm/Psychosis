package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.block.DarkPortalFrameBlock;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.EyeOfDarkEntity;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import com.sakyrhythm.psychosis.world.DarkBlockTracker;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;

import java.util.Optional;
import net.minecraft.world.chunk.ChunkStatus;

public class DarkEyeItem extends Item {

    // LOCATE_BLOCK_RADIUS 仅用于消息显示，不再限制定位距离
    private static final int LOCATE_BLOCK_RADIUS = 2000;
    private static final double EYE_SPAWN_HEIGHT_OFFSET = 0.5D;

    public DarkEyeItem(Item.Settings settings) {
        super(settings);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        // ... (useOnBlock 逻辑保持不变)
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        if (blockState.isOf(ModBlocks.DARK_BLOCK) && !blockState.get(DarkPortalFrameBlock.EYE)) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            } else {
                BlockState blockState2 = blockState.with(DarkPortalFrameBlock.EYE, true);
                Block.pushEntitiesUpBeforeBlockChange(blockState, blockState2, world, blockPos);
                world.setBlockState(blockPos, blockState2, 2);
                world.updateComparators(blockPos, ModBlocks.DARK_BLOCK);
                context.getStack().decrement(1);

                if (world instanceof ServerWorld serverWorld) {
                    net.minecraft.entity.Entity createdEntity = ModEntities.PLAYER.create(serverWorld);

                    if (createdEntity instanceof com.sakyrhythm.psychosis.entity.custom.PlayerEntity customPlayer) {
                        net.minecraft.nbt.NbtCompound useSkinNbt = new net.minecraft.nbt.NbtCompound();
                        useSkinNbt.putIntArray("id", new int[]{-744927312, 2119846582, -1503445732, 426072093});
                        net.minecraft.nbt.NbtCompound dataTrackerNbt = new net.minecraft.nbt.NbtCompound();
                        dataTrackerNbt.put("useSkin", useSkinNbt);
                        customPlayer.readNbt(dataTrackerNbt);

                        customPlayer.refreshPositionAndAngles(
                                blockPos.getX() + 0.5,
                                blockPos.getY() + 1.0, // 稍微抬高一点
                                blockPos.getZ() + 0.5,
                                0, 0
                        );
                        serverWorld.spawnEntity(customPlayer);

                        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(serverWorld);
                        if (lightning != null) {
                            lightning.refreshPositionAndAngles(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, 0.0F, 0.0F);
                            lightning.setCosmetic(true);
                            serverWorld.spawnEntity(lightning);

                            final float customDamage = Float.MAX_VALUE;
                            final double radius = 3.0;
                            DamageSource damageSource = serverWorld.getDamageSources().create(
                                    serverWorld.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(DamageTypes.LIGHTNING_BOLT).get().registryKey(),
                                    context.getPlayer()
                            );
                            Box searchBox = new Box(blockPos).expand(radius);
                            serverWorld.getEntitiesByClass(
                                    LivingEntity.class,
                                    searchBox,
                                    (entity) -> entity == customPlayer // 只对 customPlayer 的实体造成伤害
                            ).forEach((entity) -> entity.damage(damageSource, customDamage));
                            if (!customPlayer.isAlive()) {
                                net.minecraft.entity.Entity bossEntity = ModEntities.DARK_GOD.create(serverWorld);

                                if (bossEntity != null) {
                                    // 在方块中心上方生成 Boss
                                    bossEntity.refreshPositionAndAngles(
                                            blockPos.getX() + 0.5,
                                            blockPos.getY() + 1.0, // 稍微抬高一点
                                            blockPos.getZ() + 0.5,
                                            0, 0
                                    );
                                    if (bossEntity instanceof com.sakyrhythm.psychosis.entity.custom.DarkGodEntity darkGod) {
                                        darkGod.setSummoningBlockPos(blockPos);
                                    }

                                    serverWorld.spawnEntity(bossEntity);
                                }
                            }
                        }
                    }
                }

                world.syncWorldEvent(1503, blockPos, 0);
                return ActionResult.CONSUME;
            }
        } else {
            return ActionResult.PASS;
        }
    }


    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 0;
    }

    /**
     * 定位特定方块 (ModBlocks.DARK_BLOCK) 的逻辑
     * 已修改为**无限距离**定位，通过 DarkBlockTracker 优化性能。
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        BlockHitResult blockHitResult = raycast(world, player, FluidHandling.NONE);

        if (blockHitResult.getType() == Type.BLOCK && world.getBlockState(blockHitResult.getBlockPos()).isOf(ModBlocks.DARK_BLOCK)) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient) {


            if (!(world instanceof ServerWorld serverWorld)) {
                return TypedActionResult.pass(stack); // 只在服务端执行定位逻辑
            }

            // --- 新的无卡顿定位逻辑 ---
            DarkBlockTracker tracker = DarkBlockTracker.get(serverWorld);

            // 使用 tracker 的 findClosest 方法，该方法现在已移除距离限制
            Optional<BlockPos> targetPosOptional = tracker.findClosest(player.getBlockPos());
            BlockPos targetPos = targetPosOptional.orElse(null);
            if (player instanceof IPlayerEntity playerInterface && !playerInterface.getNoticed()) {
                if(!playerInterface.getNoticed()){
                    targetPos=null;
                }
            }

            if (targetPos == null) {

                // 修改错误消息以匹配无限距离的逻辑
                player.sendMessage( Text.translatable("cantfind").formatted(Formatting.DARK_PURPLE));

                return TypedActionResult.fail(stack);
            }

            // --- 输出定位到的坐标 (仅对投掷玩家显示) ---
            System.out.println("Dark Eye found target block at: " + targetPos.toShortString());
            player.sendMessage(
                    Text.translatable("Target found at: ")
                            .append(Text.literal(targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()).formatted(Formatting.AQUA)),
                    false
            );

            // --- 目标方块已找到 (targetPos 不为 null) ---

            // 4. 投掷实体
            EyeOfDarkEntity darkEye = new EyeOfDarkEntity(
                    world,
                    player.getX(),
                    player.getY() + EYE_SPAWN_HEIGHT_OFFSET,
                    player.getZ()
            );

            // 设置目标
            darkEye.setItem(stack);

            BlockPos finalTargetPos = BlockPos.ofFloored(targetPos.toCenterPos());
            darkEye.initTargetPos(finalTargetPos);

            world.emitGameEvent(GameEvent.PROJECTILE_SHOOT, darkEye.getPos(), Emitter.of(player));
            world.spawnEntity(darkEye);

            // 5. 播放声音和消耗物品
            float f = MathHelper.lerp(world.random.nextFloat(), 0.33F, 0.5F);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.NEUTRAL, 1.0F, f);

            if (!player.getAbilities().creativeMode)
                stack.decrement(1);

            player.incrementStat(Stats.USED.getOrCreateStat(this));
            player.swingHand(hand, true);
        }

        return TypedActionResult.success(stack);
    }
}