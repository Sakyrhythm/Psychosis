package com.sakyrhythm.psychosis;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import com.sakyrhythm.psychosis.entity.effect.DarkEffect;
import com.sakyrhythm.psychosis.entity.effect.DivineEffect;
import com.sakyrhythm.psychosis.entity.effect.FrenzyEffect;
import com.sakyrhythm.psychosis.entity.effect.VulnerableEffect;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import com.sakyrhythm.psychosis.item.ModItemGroups;
import com.sakyrhythm.psychosis.item.ModItems;
import com.sakyrhythm.psychosis.world.DarkBlockTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
	public static final StatusEffect FrenzyEffect = new FrenzyEffect();
	public static final StatusEffect DivineEffect = new DivineEffect();

	// 注册键定义，必须与您的 JSON 文件路径匹配
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
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "frenzy"), FrenzyEffect);
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "divine"), DivineEffect);

		// 🌟 添加 Data Pack 检查日志 (用于调试新/旧存档问题) 🌟
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Optional<RegistryEntry.Reference<DamageType>> checkEntry = server.getRegistryManager()
					.get(RegistryKeys.DAMAGE_TYPE)
					.getEntry(Psychosis.DARK_DAMAGE);

			if (checkEntry.isPresent()) {
				LOGGER.info("✅ SUCCESS: Dark Damage Type ({}) loaded.", Psychosis.DARK_DAMAGE.getValue());
			} else {
				LOGGER.error("❌ FAILURE: Dark Damage Type ({}) is MISSING from the registry. Data pack issue suspected!", Psychosis.DARK_DAMAGE.getValue());
			}
		});

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
		// *** 命令注册 (要求权限等级 >= 2) ***
		// =========================================================================

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("psychosis")
					.then(literal("test-damage")
							.requires((source) -> source.hasPermissionLevel(2)) // 要求管理员权限
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								ServerPlayerEntity player = source.getPlayer();
								if (player == null) {
									source.sendError(Text.literal("只有玩家可以执行此命令"));
									return 0;
								}
								DamageSource damageSource = player.getDamageSources().generic();
								try {
									IPlayerEntity playerInterface = (IPlayerEntity) player;
									playerInterface.setNoticed(true);
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
					.then(literal("locate")
							.then(literal("dark")
									.requires((source) -> source.hasPermissionLevel(2)) // 要求管理员权限
									.executes(context -> executeLocateDarkStructure(context.getSource()))
							)
					)
			);
		});
	}

	public static void forceAndScheduleUnload(ServerWorld world, BlockPos pos) {

		// 1. 结构定位逻辑 (从 executeLocateDarkStructure 复制)

		Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

		// 检查结构注册表，如果找不到，则中止并记录错误
		Optional<RegistryEntry.Reference<Structure>> optionalEntry = structureRegistry.getEntry(DARK_STRUCTURE_KEY);

		if (optionalEntry.isEmpty()) {
			LOGGER.error("Failed to locate DARK_STRUCTURE: Registry entry is missing.");
			return; // 无法定位，直接返回
		}

		RegistryEntry.Reference<Structure> structureReference = optionalEntry.get();
		RegistryEntryList<Structure> structureList = RegistryEntryList.of(new RegistryEntry[]{structureReference});

		// 使用传入的 pos 作为搜索起点
		BlockPos currentPos = BlockPos.ofFloored(pos.getX(), pos.getY(), pos.getZ());

		// 执行定位操作 (使用与命令相同的 100 区块半径)
		Pair<BlockPos, RegistryEntry<Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(
				world,
				structureList,
				currentPos,
				LOCATE_STRUCTURE_RADIUS, // 100 区块半径
				false
		);

		if (pair == null) {
			LOGGER.info("Player was noticed but could not find DARK_STRUCTURE within {} chunks of {}", LOCATE_STRUCTURE_RADIUS, currentPos.toShortString());
			return; // 未找到结构，直接返回
		}

		// 2. 强制加载和调度卸载 (从 executeLocateDarkStructure 复制)

		BlockPos resultPos = pair.getFirst(); // 找到的结构位置
		ChunkPos centerChunk = new ChunkPos(resultPos);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				// 强制加载 3x3 区块
				world.setChunkForced(centerChunk.x + dx, centerChunk.z + dz, true);
			}
		}

		// 3. 调度卸载任务
		CHUNK_UNLOAD_TASKS.add(new ForcedChunkTask(world, centerChunk));

		LOGGER.info("Player was noticed! Forced 3x3 chunks for DARK_STRUCTURE around: {} (Distance: {} blocks)",
				resultPos.toShortString(),
				MathHelper.floor(getHorizontalDistance(currentPos.getX(), currentPos.getZ(), resultPos.getX(), resultPos.getZ())));
	}

	private int executeLocateDarkStructure(ServerCommandSource source) throws CommandSyntaxException {
		Registry<Structure> structureRegistry = source.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);

		Optional<RegistryEntry.Reference<Structure>> optionalEntry = structureRegistry.getEntry(DARK_STRUCTURE_KEY);

		if (optionalEntry.isEmpty()) {
			throw DARK_STRUCTURE_NOT_FOUND_EXCEPTION.create(DARK_STRUCTURE_ID.toString());
		}

		RegistryEntry.Reference<Structure> structureReference = optionalEntry.get();
		RegistryEntryList<Structure> structureList = RegistryEntryList.of(new RegistryEntry[]{structureReference});

		BlockPos currentPos = BlockPos.ofFloored(source.getPosition());
		ServerWorld serverWorld = source.getWorld();

		Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
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
			throw DARK_STRUCTURE_NOT_FOUND_EXCEPTION.create(DARK_STRUCTURE_ID.toString());
		} else {
			BlockPos resultPos = pair.getFirst();
			String entryString = DARK_STRUCTURE_ID.toString();
			ChunkPos centerChunk = new ChunkPos(resultPos);

			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					serverWorld.setChunkForced(centerChunk.x + dx, centerChunk.z + dz, true);
				}
			}
			CHUNK_UNLOAD_TASKS.add(new ForcedChunkTask(serverWorld, centerChunk));
			int distance = MathHelper.floor(getHorizontalDistance(currentPos.getX(), currentPos.getZ(), resultPos.getX(), resultPos.getZ()));
			LOGGER.info("Locating element " + entryString + " took " + timeTaken.toMillis() + " ms. Forced 3x3 chunks for 1 second around: {}", resultPos.toShortString());
			return distance;
		}
	}
	private static float getHorizontalDistance(int x1, int y1, int x2, int y2) {
		int i = x2 - x1;
		int j = y2 - y1;
		return MathHelper.sqrt((float)(i * i + j * j));
	}
}