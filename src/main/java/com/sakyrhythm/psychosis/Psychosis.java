package com.sakyrhythm.psychosis;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.sakyrhythm.psychosis.block.ModBlocks;
import com.sakyrhythm.psychosis.entity.ModEntities;
import com.sakyrhythm.psychosis.entity.custom.PlayerEntity; // 注意这里的 PlayerEntity 可能是你的自定义实体
import com.sakyrhythm.psychosis.entity.effect.DarkEffect;
import com.sakyrhythm.psychosis.entity.effect.DivineEffect;
import com.sakyrhythm.psychosis.entity.effect.FrenzyEffect;
import com.sakyrhythm.psychosis.entity.effect.VulnerableEffect;
import com.sakyrhythm.psychosis.interfaces.IPlayerEntity;
import com.sakyrhythm.psychosis.item.ModArmorItems;
import com.sakyrhythm.psychosis.item.ModItemGroups;
import com.sakyrhythm.psychosis.item.ModItems;
import com.sakyrhythm.psychosis.mixin.PlayerMixin; // 导入 PlayerMixin
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

	// 注册键定义
	public static final RegistryKey<DamageType> DARK_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID, "dark"));
	public static final RegistryKey<DamageType> SHADOW_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID, "shadow"));
	public static final RegistryKey<DamageType> FRENZY_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID, "frenzy"));

	// 自定义定位命令所需常量
	public static final Identifier DARK_STRUCTURE_ID = Identifier.of(MOD_ID, "dark");
	public static final RegistryKey<Structure> DARK_STRUCTURE_KEY = RegistryKey.of(RegistryKeys.STRUCTURE, DARK_STRUCTURE_ID);

	private static final DynamicCommandExceptionType DARK_STRUCTURE_NOT_FOUND_EXCEPTION =
			new DynamicCommandExceptionType((id) -> Text.translatable("commands.locate.structure.not_found", id));
	private static final int LOCATE_STRUCTURE_RADIUS = 100;

	// 用于存储和管理延迟卸载任务的静态列表和任务类
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
		// 永久注册 Tick 事件监听器来处理所有延迟卸载任务
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// 使用 removeIf 迭代器进行安全移除
			CHUNK_UNLOAD_TASKS.removeIf(task -> {
				if (task.timer.decrementAndGet() <= 0) {
					// 执行卸载逻辑
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							task.world.setChunkForced(task.centerChunk.x + dx, task.centerChunk.z + dz, false);
						}
					}
					LOGGER.info("Unloaded 3x3 forced chunks around: {} after 1 second delay.", task.centerChunk.toString());
					return true;
				}
				return false;
			});
		});

		FabricDefaultAttributeRegistry.register(ModEntities.PLAYER, PlayerEntity.createPlayerAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.DEGENERATEWITHER, PlayerEntity.createPlayerAttributes());

		ModItems.registerModItems();
		ModArmorItems.registerModItems();
		ModItemGroups.registerModItemGroups();
		ModBlocks.registerModBlocks();
		ModEntities.registerAttributes();

		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "dark"), DarkEffect);
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "vulnerable"), VulnerableEffect);
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "frenzy"), FrenzyEffect);
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "divine"), DivineEffect);

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

		// 区块加载事件，用于追踪旧世界和重启服务器后加载的 DARK_BLOCK
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
					// 1. 测试伤害命令
					.then(literal("test-damage")
							.requires((source) -> source.hasPermissionLevel(2))
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
					// 2. 定位命令
					.then(literal("locate")
							.then(literal("dark")
									.requires((source) -> source.hasPermissionLevel(2))
									.executes(context -> executeLocateDarkStructure(context.getSource()))
							)
					)
					// 3. ⭐ 新增查询 DarkEffect 状态命令
					.then(literal("status")
							.then(literal("dark")
									.requires((source) -> source.hasPermissionLevel(0))
									.executes(context -> {
										ServerCommandSource source = context.getSource();
										ServerPlayerEntity player = source.getPlayer();
										if (player == null) {
											source.sendError(Text.literal("只有玩家可以执行此命令"));
											return 0;
										}

										// 调用 Mixin 中实现的查询方法
										IPlayerEntity playerMixin = (IPlayerEntity) (Object) player;
										int[] darkInfo = playerMixin.queryDarkEffectInfo();
										int level = darkInfo[0];
										int duration = darkInfo[1];

										if (level > 0) {
											double durationSeconds = (double) duration / 20.0;

											source.sendFeedback(() ->
															Text.literal("✅ DarkEffect 状态: 等级 ")
																	.append(Text.literal(String.valueOf(level)).withColor(0xFF00FF))
																	.append(Text.literal(", 剩余时间: "))
																	.append(Text.literal(String.format("%.1f", durationSeconds) + " 秒").withColor(0x00FFFF)),
													false);
											return 1;
										} else {
											source.sendFeedback(() -> Text.literal("❌ DarkEffect 不存在于你身上。").withColor(0xFF0000), false);
											return 0;
										}
									})
							)
					)
			);
		});
	}

	public static void forceAndScheduleUnload(ServerWorld world, BlockPos pos) {

		Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

		Optional<RegistryEntry.Reference<Structure>> optionalEntry = structureRegistry.getEntry(DARK_STRUCTURE_KEY);

		if (optionalEntry.isEmpty()) {
			LOGGER.error("Failed to locate DARK_STRUCTURE: Registry entry is missing.");
			return;
		}

		RegistryEntry.Reference<Structure> structureReference = optionalEntry.get();
		RegistryEntryList<Structure> structureList = RegistryEntryList.of(new RegistryEntry[]{structureReference});

		BlockPos currentPos = BlockPos.ofFloored(pos.getX(), pos.getY(), pos.getZ());

		Pair<BlockPos, RegistryEntry<Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(
				world,
				structureList,
				currentPos,
				LOCATE_STRUCTURE_RADIUS,
				false
		);

		if (pair == null) {
			LOGGER.info("Player was noticed but could not find DARK_STRUCTURE within {} chunks of {}", LOCATE_STRUCTURE_RADIUS, currentPos.toShortString());
			return;
		}

		BlockPos resultPos = pair.getFirst();
		ChunkPos centerChunk = new ChunkPos(resultPos);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				world.setChunkForced(centerChunk.x + dx, centerChunk.z + dz, true);
			}
		}

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

			source.sendFeedback(() -> Text.translatable("commands.locate.structure.success",
					DARK_STRUCTURE_ID.toString(),
					resultPos.getX(),
					resultPos.getY(),
					resultPos.getZ(),
					distance
			), false);
			return distance;
		}
	}
	private static float getHorizontalDistance(int x1, int y1, int x2, int y2) {
		int i = x2 - x1;
		int j = y2 - y1;
		return MathHelper.sqrt((float)(i * i + j * j));
	}
}