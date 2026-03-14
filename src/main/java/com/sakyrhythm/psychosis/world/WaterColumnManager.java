package com.sakyrhythm.psychosis.world;

import com.sakyrhythm.psychosis.Psychosis;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaterColumnManager {
    // 2:通知, 16:防流体, 128:防光照更新
    private static final int FAST_FLAGS = 2 | 16 | 128;

    private static final List<IcePillarColumn> ACTIVE_PILLARS = new CopyOnWriteArrayList<>();
    private static final List<IceSpear> ACTIVE_SPEARS = new CopyOnWriteArrayList<>();
    private static final Set<BlockPos> FOOT_ICE_POSITIONS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> PLAYER_HIT_COOLDOWN = new ConcurrentHashMap<>();

    private static boolean pendingGlobalClear = false;

    private static final int ICE_SPEAR_CHANCE = 250;
    private static final int ICE_PILLAR_CHANCE = 80;
    public static final int BOTTOM_WATER_TOP = -64 + 70;
    public static final int ICE_LAYER_Y = BOTTOM_WATER_TOP - 1; // Y=5
    public static final int TOP_WATER_BOTTOM = BOTTOM_WATER_TOP + 100;
    public static final int TOP_WATER_TOP = TOP_WATER_BOTTOM + 60;
    private static final int PILLAR_START_Y = TOP_WATER_BOTTOM - 1;

    // --- 冰柱类 ---
    public static class IcePillarColumn {
        public BlockPos sourcePos;
        public Set<BlockPos> history = new HashSet<>();
        public long startTime;
        public boolean reachedBottom = false;
        public int currentY = PILLAR_START_Y;
        public int iceY = BOTTOM_WATER_TOP;
        public boolean isGrowthComplete = false;

        public IcePillarColumn(BlockPos pos, long worldTime) {
            this.sourcePos = pos;
            this.startTime = worldTime;
        }

        public void tick(ServerWorld world) {
            if (!reachedBottom) {
                for (int i = 0; i < 3; i++) {
                    if (currentY <= BOTTOM_WATER_TOP) { reachedBottom = true; break; }
                    placeCrossWater(world, sourcePos.withY(currentY));
                    currentY--;
                }
            } else if (!isGrowthComplete) {
                if (iceY % 5 == 0) {
                    world.playSound(null, sourcePos.getX(), iceY, sourcePos.getZ(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 1f, 0.6f);
                }
                for (int i = 0; i < 2 && iceY < PILLAR_START_Y; i++) {
                    int radius = getOriginalRadius(iceY, BOTTOM_WATER_TOP, PILLAR_START_Y);
                    drawCircleIce(world, sourcePos.withY(iceY), radius);
                    iceY++;
                }
                if (iceY >= PILLAR_START_Y) isGrowthComplete = true;
            }
        }

        private void placeCrossWater(ServerWorld world, BlockPos center) {
            BlockPos[] cross = {center, center.north(), center.south(), center.east(), center.west()};
            for (BlockPos p : cross) {
                if (world.isAir(p)) {
                    world.setBlockState(p, Blocks.WATER.getDefaultState(), FAST_FLAGS);
                    history.add(p);
                }
            }
        }

        private void drawCircleIce(ServerWorld world, BlockPos center, int r) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= r * r) {
                        BlockPos p = center.add(x, 0, z);
                        if (world.isAir(p) || world.getBlockState(p).isOf(Blocks.WATER)) {
                            world.setBlockState(p, Blocks.PACKED_ICE.getDefaultState(), FAST_FLAGS);
                            history.add(p);
                        }
                    }
                }
            }
        }

        private int getOriginalRadius(int y, int bottomY, int topY) {
            double progress = (double) (y - bottomY) / (topY - bottomY);
            if (progress < 0.3) return 4 - (int)((progress / 0.3) * 2);
            if (progress < 0.7) return 1 + (int)(Math.sin(progress * Math.PI) * 1);
            return 2 + (int)(((progress - 0.7) / 0.3) * 2);
        }

        public void remove(ServerWorld world) {
            for (BlockPos p : history) world.setBlockState(p, Blocks.AIR.getDefaultState(), FAST_FLAGS);
            history.clear();
        }
    }

    // --- 冰矛类 ---
    public static class IceSpear {
        public Vec3d position;
        public Vec3d velocity;
        public Vec3d direction;
        public ServerPlayerEntity target;
        public Set<BlockPos> history = new HashSet<>();
        public boolean active = true;

        public IceSpear(Vec3d pos, Vec3d dir, ServerPlayerEntity target) {
            this.position = pos;
            this.direction = dir.normalize();
            this.velocity = this.direction.multiply(1.5);
            this.target = target;
        }

        public void tick(ServerWorld world) {
            if (!active) return;
            drawSpearHead(world);

            if (target != null && target.isAlive() && position.distanceTo(target.getPos()) < 5.0) {
                Vec3d kb = direction.multiply(6.5).add(0, 2.0, 0);
                target.setVelocity(kb.x, kb.y, kb.z);
                target.velocityModified = true;
                target.damage(target.getDamageSources().magic(), target.getMaxHealth() * 0.3f);

                PLAYER_HIT_COOLDOWN.put(target.getUuid(), 40);
                pendingGlobalClear = true;
                this.active = false;
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1.2f);
                return;
            }

            position = position.add(velocity);
            if (position.y < -100 || position.y > 500) active = false;
        }

        private void drawSpearHead(ServerWorld world) {
            Vec3d right = direction.crossProduct(new Vec3d(0, 1, 0)).normalize();
            if (right.lengthSquared() < 0.01) right = direction.crossProduct(new Vec3d(1, 0, 0)).normalize();
            Vec3d up = right.crossProduct(direction).normalize();

            for (int i = 0; i < 10; i++) {
                Vec3d lPos = position.subtract(direction.multiply(i));
                int size = (i == 0) ? 0 : (i < 3 ? 1 : (i < 8 ? 2 : 1));
                for (int r = -size; r <= size; r++) {
                    for (int u = -size; u <= size; u++) {
                        if (size > 0 && Math.abs(r) + Math.abs(u) > size) continue;
                        Vec3d p = lPos.add(right.multiply(r)).add(up.multiply(u));
                        BlockPos bp = new BlockPos((int)p.x, (int)p.y, (int)p.z);
                        if (bp.getY() < PILLAR_START_Y && world.isAir(bp)) {
                            world.setBlockState(bp, Blocks.PACKED_ICE.getDefaultState(), FAST_FLAGS);
                            history.add(bp);
                        }
                    }
                }
            }
        }

        public void remove(ServerWorld world) {
            for (BlockPos p : history) {
                if (world.getBlockState(p).isOf(Blocks.PACKED_ICE)) world.setBlockState(p, Blocks.AIR.getDefaultState(), FAST_FLAGS);
            }
            history.clear();
        }
    }

    public static void tick(ServerWorld world) {
        if (!Psychosis.isTheOcean(world)) return;

        Iterator<Map.Entry<UUID, Integer>> cooldownIt = PLAYER_HIT_COOLDOWN.entrySet().iterator();
        while (cooldownIt.hasNext()) {
            Map.Entry<UUID, Integer> entry = cooldownIt.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) cooldownIt.remove();
            else entry.setValue(remaining);
        }

        ensureWaterFloor(world);
        repairSkyWaterOptimized(world);

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getY() < ICE_LAYER_Y - 0.5) {
                applyWaterThrustAndBreakIce(world, player);
            } else if (canPlayerGenerateIce(player)) {
                generateIceCircle(world, player);
            }
        }

        ACTIVE_PILLARS.removeIf(p -> {
            if (world.getTime() - p.startTime > 600) { p.remove(world); return true; }
            p.tick(world);
            return false;
        });

        ACTIVE_SPEARS.removeIf(s -> {
            if (!s.active) { s.remove(world); return true; }
            s.tick(world);
            return false;
        });

        if (pendingGlobalClear) {
            performGlobalDimensionClear(world);
            pendingGlobalClear = false;
        }

        if (world.getTime() % 10 == 0) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (world.random.nextInt(ICE_PILLAR_CHANCE) == 0) spawnIcePillar(world, player);
                if (world.random.nextInt(ICE_SPEAR_CHANCE) == 0) spawnIceSpear(world, player);
            }
        }
    }

    private static void applyWaterThrustAndBreakIce(ServerWorld world, ServerPlayerEntity player) {
        BlockPos center = player.getBlockPos().withY(ICE_LAYER_Y);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos p = center.add(x, 0, z);
                Block b = world.getBlockState(p).getBlock();
                if (b == Blocks.ICE || b == Blocks.PACKED_ICE) {
                    world.setBlockState(p, Blocks.WATER.getDefaultState(), FAST_FLAGS);
                }
            }
        }
        if (player.getY() < ICE_LAYER_Y) {
            Vec3d currentVel = player.getVelocity();
            player.setVelocity(currentVel.x, 1, currentVel.z);
            player.velocityModified = true;
            if (world.getTime() % 5 == 0) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.8f, 1.2f);
                world.spawnParticles(ParticleTypes.SPLASH, player.getX(), ICE_LAYER_Y, player.getZ(), 10, 0.5, 0.1, 0.5, 0.1);
            }
        }
    }

    private static void performGlobalDimensionClear(ServerWorld world) {
        for (IcePillarColumn p : ACTIVE_PILLARS) p.remove(world);
        ACTIVE_PILLARS.clear();
        for (IceSpear s : ACTIVE_SPEARS) s.remove(world);
        ACTIVE_SPEARS.clear();
        for (BlockPos p : FOOT_ICE_POSITIONS) world.setBlockState(p, Blocks.WATER.getDefaultState(), FAST_FLAGS);
        FOOT_ICE_POSITIONS.clear();

        for (ServerPlayerEntity player : world.getPlayers()) {
            BlockPos pos = player.getBlockPos();
            Iterable<BlockPos> area = BlockPos.iterate(pos.getX()-50, BOTTOM_WATER_TOP-2, pos.getZ()-50, pos.getX()+50, PILLAR_START_Y+2, pos.getZ()+50);
            for (BlockPos target : area) {
                Block b = world.getBlockState(target).getBlock();
                if (b == Blocks.ICE || b == Blocks.PACKED_ICE) world.setBlockState(target, Blocks.AIR.getDefaultState(), FAST_FLAGS);
            }
        }
        world.playSound(null, 0, 64, 0, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.AMBIENT, 5.0f, 0.5f);
    }

    private static boolean canPlayerGenerateIce(ServerPlayerEntity player) {
        if (player.getY() < ICE_LAYER_Y - 0.5) return false;
        return !PLAYER_HIT_COOLDOWN.containsKey(player.getUuid());
    }

    public static void generateIceCircle(ServerWorld world, ServerPlayerEntity player) {
        BlockPos center = player.getBlockPos().withY(ICE_LAYER_Y);
        boolean soundPlayed = false;
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (x * x + z * z <= 49) {
                    BlockPos p = center.add(x, 0, z);
                    if (world.getBlockState(p).isOf(Blocks.WATER)) {
                        world.setBlockState(p, Blocks.ICE.getDefaultState(), FAST_FLAGS);
                        FOOT_ICE_POSITIONS.add(p.toImmutable());
                        if (!soundPlayed && world.random.nextInt(10) == 0) {
                            world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.3f, 0.5f);
                            soundPlayed = true;
                        }
                    }
                    if (world.getBlockState(p.up()).isOf(Blocks.WATER)) world.setBlockState(p.up(), Blocks.AIR.getDefaultState(), FAST_FLAGS);
                }
            }
        }
    }

    private static void ensureWaterFloor(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!PLAYER_HIT_COOLDOWN.containsKey(player.getUuid())) {
                BlockPos center = player.getBlockPos().withY(ICE_LAYER_Y);
                for (int x = -8; x <= 8; x++) {
                    for (int z = -8; z <= 8; z++) {
                        BlockPos p = center.add(x, 0, z);
                        if (world.isAir(p)) world.setBlockState(p, Blocks.WATER.getDefaultState(), FAST_FLAGS);
                    }
                }
            }
        }
    }

    private static void repairSkyWaterOptimized(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            BlockPos p = player.getBlockPos();
            for (int i = 0; i < 150; i++) {
                int x = p.getX() + world.random.nextInt(81) - 40;
                int z = p.getZ() + world.random.nextInt(81) - 40;
                int y = TOP_WATER_BOTTOM + world.random.nextInt(TOP_WATER_TOP - TOP_WATER_BOTTOM);
                BlockPos target = new BlockPos(x, y, z);
                if (world.isAir(target)) world.setBlockState(target, Blocks.WATER.getDefaultState(), FAST_FLAGS);
            }
        }
    }

    public static void spawnIcePillar(ServerWorld world, ServerPlayerEntity player) {
        if (ACTIVE_PILLARS.size() >= 5) return;
        BlockPos pos = player.getBlockPos().add(world.random.nextInt(40)-20, 0, world.random.nextInt(40)-20).withY(PILLAR_START_Y);
        ACTIVE_PILLARS.add(new IcePillarColumn(pos, world.getTime()));
    }

    public static void spawnIceSpear(ServerWorld world, ServerPlayerEntity player) {
        Vec3d start = player.getPos().add(world.random.nextInt(60)-30, 40, world.random.nextInt(60)-30);
        ACTIVE_SPEARS.add(new IceSpear(start, player.getPos().subtract(start), player));
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.8f);
    }

    public static double getDefaultSpawnY() { return (double) ICE_LAYER_Y + 1.0; }
}