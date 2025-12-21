package com.sakyrhythm.psychosis;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import com.sakyrhythm.psychosis.entity.effect.DarkEffect;
import com.sakyrhythm.psychosis.entity.effect.VulnerableEffect;
import com.sakyrhythm.psychosis.item.ModItemGroups;
import com.sakyrhythm.psychosis.item.ModItems;
import com.sakyrhythm.psychosis.world.DarkBlockTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.server.command.CommandManager.literal;

public class Psychosis implements ModInitializer {
	public static final String MOD_ID = "psychosis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final StatusEffect DarkEffect = new DarkEffect();
	public static final StatusEffect VulnerableEffect = new VulnerableEffect();
	public static final StatusEffect FRENZYEffect = new VulnerableEffect();
	public static final RegistryKey<DamageType> DARK_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID, "dark"));
	public static final RegistryKey<DamageType> SHADOW_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID, "shadow"));
	public static final RegistryKey<DamageType> FRENZY_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID, "frenzy"));

	// *** 自定义定位命令所需常量 ***
	public static final Identifier DARK_STRUCTURE_ID = Identifier.of(MOD_ID, "dark");
	public static final RegistryKey<Structure> DARK_STRUCTURE_KEY = RegistryKey.of(RegistryKeys.STRUCTURE, DARK_STRUCTURE_ID);

	private static final DynamicCommandExceptionType DARK_STRUCTURE_NOT_FOUND_EXCEPTION =
			new DynamicCommandExceptionType((id) -> Text.translatable("commands.locate.structure.not_found", id));
	private static final int LOCATE_STRUCTURE_RADIUS = 100;

	// =========================================================================
	// *** 用于存储和管理延迟卸载任务的静态列表和任务类 ***
	// =========================================================================
	private static final List<ForcedChunkTask> CHUNK_UNLOAD_TASKS = new ArrayList<>();

	public static class ForcedChunkTask {
		public final ServerWorld world;
		public final ChunkPos centerChunk;
		public final AtomicInteger timer = new AtomicInteger(20); // 20 刻 = 1 秒

		public ForcedChunkTask(ServerWorld world, ChunkPos centerChunk) {
			this.world = world;
			this.centerChunk = centerChunk;
		}
	}

	@Override
	public void onInitialize() {

		// =========================================================================
		// *** 永久注册 Tick 事件监听器来处理所有延迟卸载任务 (取代 unregister) ***
		// =========================================================================
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// 使用 removeIf 迭代器进行安全移除 (关键！)
			CHUNK_UNLOAD_TASKS.removeIf(task -> {
				// 1. 检查计时器是否到达 0
				if (task.timer.decrementAndGet() <= 0) {

					// 2. 执行卸载逻辑
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							// 强制卸载区块
							task.world.setChunkForced(task.centerChunk.x + dx, task.centerChunk.z + dz, false);
						}
					}
					LOGGER.info("Unloaded 3x3 forced chunks around: {} after 1 second delay.", task.centerChunk.toString());

					// 3. 返回 true，告诉 removeIf 移除此任务
					return true;
				}
				// 4. 返回 false，保留此任务继续计时
				return false;
			});
		});

		FabricDefaultAttributeRegistry.register(ModEntities.PLAYER, PlayerEntity.createPlayerAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.DEGENERATEWITHER, PlayerEntity.createPlayerAttributes());

		ModItems.registerModItems();
		ModItemGroups.registerModItemGroups();
		ModBlocks.registerModBlocks();
		ModEntities.registerAttributes();

		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "dark"), DarkEffect);
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "vulnerable"), VulnerableEffect);
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "frenzy"), FRENZYEffect);

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
				return ActionResult.PASS;
			}

			if (player.getStackInHand(hand).getItem() instanceof BlockItem blockItem) {

				Block block = blockItem.getBlock();
				BlockState heldState = block.getDefaultState();

				if (heldState.isOf(ModBlocks.DARK_BLOCK)) {
					BlockPos placedPos = hitResult.getBlockPos().offset(hitResult.getSide());
					DarkBlockTracker tracker = DarkBlockTracker.get(serverWorld);
					tracker.addPosition(placedPos);
					LOGGER.info("DarkBlockTracker (UseBlock): Added position: {}", placedPos.toShortString());
				}
			}

			return ActionResult.PASS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClient() || hand != Hand.MAIN_HAND || !(world instanceof ServerWorld serverWorld)) {
				return ActionResult.PASS;
			}

			BlockState stateToBreak = world.getBlockState(pos);
			if (stateToBreak.isOf(ModBlocks.DARK_BLOCK)) {
				DarkBlockTracker tracker = DarkBlockTracker.get(serverWorld);
				tracker.removePosition(pos);

				LOGGER.info("DarkBlockTracker (AttackBlock): Removed position: {}", pos.toShortString());
			}

			return ActionResult.PASS;
		});

		// =========================================================================
		// *** 区块加载事件，用于追踪旧世界和重启服务器后加载的 DARK_BLOCK ***
		// =========================================================================
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			if (!(world instanceof ServerWorld serverWorld)) {
				return;
			}

			DarkBlockTracker tracker = DarkBlockTracker.get(serverWorld);
			WorldChunk worldChunk = (WorldChunk) chunk;

			ChunkPos chunkPos = worldChunk.getPos();
			int minX = chunkPos.getStartX();
			int minZ = chunkPos.getStartZ();
			int minY = world.getBottomY();
			int maxY = world.getTopY();

			int addedCount = 0;

			for (BlockPos pos : BlockPos.iterate(minX, minY, minZ, minX + 15, maxY - 1, minZ + 15)) {
				BlockState state = worldChunk.getBlockState(pos);

				if (state.isOf(ModBlocks.DARK_BLOCK) && !tracker.getAllPositions().contains(pos)) {
					tracker.addPosition(pos.toImmutable());
					addedCount++;
				}
			}

			if (addedCount > 0) {
				tracker.markDirty();
			}
		});

		// =========================================================================
		// *** 命令注册 ***
		// =========================================================================

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("psychosis")
					.then(literal("test-damage")
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								ServerPlayerEntity player = source.getPlayer();
								if (player == null) {
									source.sendError(Text.literal("只有玩家可以执行此命令"));
									return 0;
								}
								DamageSource damageSource = player.getDamageSources().generic();
								try {
									for (int i = 0; i < 10; i++) {
										player.damage(damageSource, 1.0f);
										player.sendMessage(Text.literal("造成伤害 #" + (i + 1)), false);
									}
									source.sendFeedback(() -> Text.literal("已成功造成10次无冷却伤害"), false);
									return 1;
								} catch (Exception e) {
									LOGGER.error("在测试伤害命令中发生意外错误", e);
									source.sendError(Text.literal("命令执行失败: " + e.getMessage()));
									return -1;
								}
							})
					)
					// *** /psychosis locate dark 命令 ***
					.then(literal("locate")
							.then(literal("dark")
									.requires((source) -> source.hasPermissionLevel(2))
									.executes(context -> executeLocateDarkStructure(context.getSource()))
							)
					)
			);
		});
	}

	// =========================================================================
	// *** 自定义定位结构体的执行方法 (静默模式，使用静态任务列表) ***
	// =========================================================================

	private int executeLocateDarkStructure(ServerCommandSource source) throws CommandSyntaxException {
		// 1. 获取 Structure 注册表
		Registry<Structure> structureRegistry = source.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);

		// 2. 尝试从注册表中获取指定的结构体 RegistryEntry
		Optional<RegistryEntry.Reference<Structure>> optionalEntry = structureRegistry.getEntry(DARK_STRUCTURE_KEY);

		if (optionalEntry.isEmpty()) {
			// 命令失败：抛出异常
			throw DARK_STRUCTURE_NOT_FOUND_EXCEPTION.create(DARK_STRUCTURE_ID.toString());
		}

		RegistryEntry.Reference<Structure> structureReference = optionalEntry.get();
		RegistryEntryList<Structure> structureList = RegistryEntryList.of(new RegistryEntry[]{structureReference});

		BlockPos currentPos = BlockPos.ofFloored(source.getPosition());
		ServerWorld serverWorld = source.getWorld();

		Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);

		// 3. 执行原版的结构体定位逻辑
		Pair<BlockPos, RegistryEntry<Structure>> pair = serverWorld.getChunkManager().getChunkGenerator().locateStructure(
				serverWorld,
				structureList,
				currentPos,
				LOCATE_STRUCTURE_RADIUS,
				false
		);

		stopwatch.stop();
		Duration timeTaken = stopwatch.elapsed();

		if (pair == null) {
			// 命令失败：抛出异常
			throw DARK_STRUCTURE_NOT_FOUND_EXCEPTION.create(DARK_STRUCTURE_ID.toString());
		} else {
			BlockPos resultPos = pair.getFirst();
			String entryString = DARK_STRUCTURE_ID.toString();

			// =========================================================================
			// *** 强制加载区块 (Load & Add Task to Static List) ***
			// =========================================================================
			ChunkPos centerChunk = new ChunkPos(resultPos);

			// 1. 立即加载 3x3 区块
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					// 强制加载区块
					serverWorld.setChunkForced(centerChunk.x + dx, centerChunk.z + dz, true);
				}
			}

			// 2. 创建任务对象并添加到静态列表。
			CHUNK_UNLOAD_TASKS.add(new ForcedChunkTask(serverWorld, centerChunk));

			// =========================================================================

			// 4. 计算距离并记录日志 (静默，无聊天消息)
			int distance = MathHelper.floor(getHorizontalDistance(currentPos.getX(), currentPos.getZ(), resultPos.getX(), resultPos.getZ()));

			// 仅记录到服务器日志
			LOGGER.info("Locating element " + entryString + " took " + timeTaken.toMillis() + " ms. Forced 3x3 chunks for 1 second around: {}", resultPos.toShortString());

			// 返回命令执行结果（距离）
			return distance;
		}
	}

	/**
	 * Helper function to calculate the horizontal distance.
	 */
	private static float getHorizontalDistance(int x1, int y1, int x2, int y2) {
		int i = x2 - x1;
		int j = y2 - y1;
		return MathHelper.sqrt((float)(i * i + j * j));
	}
}