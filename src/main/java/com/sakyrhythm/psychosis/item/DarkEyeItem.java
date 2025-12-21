//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.block.DarkPortalFrameBlock;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.registry.tag.ModStructureTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

import java.util.Objects;

public class DarkEyeItem extends Item {
    public DarkEyeItem(Item.Settings settings) {
        super(settings);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        // 检查方块是否为自定义的 Dark Block 且没有插入黑暗之眼
        if (blockState.isOf(ModBlocks.DARK_BLOCK) && !(Boolean)blockState.get(DarkPortalFrameBlock.EYE)) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            } else {
                // 设置方块状态为已插入黑暗之眼
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
                        customPlayer.refreshPositionAndAngles(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, 0, 0);
                        serverWorld.spawnEntity(customPlayer);

                        LightningEntity lightning =
                                EntityType.LIGHTNING_BOLT.create(serverWorld);

                        if (lightning != null) {
                            lightning.refreshPositionAndAngles(
                                    blockPos.getX() + 0.5,
                                    blockPos.getY(),
                                    blockPos.getZ() + 0.5,
                                    0.0F,
                                    0.0F
                            );

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
                                    (entity) -> entity == customPlayer
                            ).forEach((entity) -> entity.damage(damageSource, customDamage));
                        }
                    }
                }

                // 执行事件同步
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

    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        BlockHitResult blockHitResult = raycast(world, user, FluidHandling.NONE);
        if (blockHitResult.getType() == Type.BLOCK && world.getBlockState(blockHitResult.getBlockPos()).isOf(ModBlocks.DARK_BLOCK)) {
            return TypedActionResult.pass(itemStack);
        } else {
            user.setCurrentHand(hand);
            if (world instanceof ServerWorld serverWorld) {
                BlockPos blockPos = serverWorld.locateStructure(ModStructureTags.DARK_EYE_LOCATED, user.getBlockPos(), 100, false);
                if (blockPos != null) {
                    EyeOfEnderEntity eyeOfEnderEntity = new EyeOfEnderEntity(world, user.getX(), user.getBodyY(0.5F), user.getZ());
                    eyeOfEnderEntity.setItem(itemStack);
                    eyeOfEnderEntity.initTargetPos(blockPos);
                    world.emitGameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnderEntity.getPos(), Emitter.of(user));
                    world.spawnEntity(eyeOfEnderEntity);

                    float f = MathHelper.lerp(world.random.nextFloat(), 0.33F, 0.5F);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.NEUTRAL, 1.0F, f);
                    itemStack.decrementUnlessCreative(1, user);
                    user.incrementStat(Stats.USED.getOrCreateStat(this));
                    user.swingHand(hand, true);
                    return TypedActionResult.success(itemStack);
                }
            }

            return TypedActionResult.consume(itemStack);
        }
    }
}
