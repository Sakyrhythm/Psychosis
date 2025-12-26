package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.entity.custom.DarkDartProjectile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarkSwordItem extends SwordItem {

    private static final Logger LOGGER = LoggerFactory.getLogger("DarkSword");

    private static final String ANCHOR_TAG = "DarkSwordAnchor";
    private static final String HAS_ANCHOR_KEY = "HasAnchor";
    private static final String X_KEY = "AnchorX";
    private static final String Y_KEY = "AnchorY";
    private static final String Z_KEY = "AnchorZ";
    private static final String LEVEL_KEY = "AnchorLevel";
    private static final String ENTITY_ID_KEY = "AnchorEntityId";

    public DarkSwordItem(Settings settings) {
        super(ModToolMaterials.DARK, settings);
    }

    public static void saveAnchor(ItemStack swordStack, PlayerEntity player, Hand hand, World hitWorld, Vec3d hitPos, int entityId) {

        ItemStack stackToModify = player.getStackInHand(hand);

        // 修正点 1：确保 stackToModify 不为空
        if (stackToModify.isEmpty()) return;

        @Nullable
        NbtComponent nbtComponent = stackToModify.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound rootNbt = (nbtComponent != null) ? nbtComponent.copyNbt() : new NbtCompound();
        NbtCompound anchorTag = rootNbt.getCompound(ANCHOR_TAG);

        anchorTag.putBoolean(HAS_ANCHOR_KEY, true);
        anchorTag.putDouble(X_KEY, hitPos.x);
        anchorTag.putDouble(Y_KEY, hitPos.y);
        anchorTag.putDouble(Z_KEY, hitPos.z);

        RegistryKey<World> dimensionKey = hitWorld.getRegistryKey();
        anchorTag.putString(LEVEL_KEY, dimensionKey.getValue().toString());

        if (entityId != -1) {
            anchorTag.putInt(ENTITY_ID_KEY, entityId);
        } else if (anchorTag.contains(ENTITY_ID_KEY)) {
            anchorTag.remove(ENTITY_ID_KEY);
        }

        rootNbt.put(ANCHOR_TAG, anchorTag);

        stackToModify.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootNbt));
        stackToModify.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        LOGGER.info("ANCHOR SAVED: Pos={}, EntityID={}, NBT={}", hitPos, entityId, anchorTag.asString());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.pass(stack);
        }

        ServerWorld sw = (ServerWorld) world;

        if (stack.isEmpty()) return TypedActionResult.pass(stack);

        NbtCompound anchorTag = new NbtCompound();
        boolean hasAnchor = false;

        @Nullable
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);

        if (nbtComponent != null) {
            NbtCompound rootNbt = nbtComponent.copyNbt();
            if (rootNbt.contains(ANCHOR_TAG)) {
                anchorTag = rootNbt.getCompound(ANCHOR_TAG);
                hasAnchor = anchorTag.getBoolean(HAS_ANCHOR_KEY);
            }
        }

        if (hasAnchor) {
        } else {
        }

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (hasAnchor) {
            teleportPlayer(stack, player, sw, anchorTag);
            player.getItemCooldownManager().set(this, 20); // 传送 CD: 1秒
            return TypedActionResult.success(stack);
        } else {
            launchProjectile(sw, player, stack, hand);
            player.getItemCooldownManager().set(this, 2); // 发射 CD: 0.1秒
            return TypedActionResult.success(stack);
        }
    }

    private void launchProjectile(ServerWorld world, PlayerEntity player, ItemStack stack, Hand hand) {

        if (stack.isEmpty()) return;

        @Nullable
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound rootTag = nbtComponent.copyNbt();
            rootTag.remove(ANCHOR_TAG);

            if (rootTag.isEmpty()) {
                stack.remove(DataComponentTypes.CUSTOM_DATA);
            } else {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootTag));
            }
        }

        stack.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);

        DarkDartProjectile dart = new DarkDartProjectile(world, player, stack, hand);
        world.spawnEntity(dart);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
    }

    private void teleportPlayer(ItemStack swordStack, PlayerEntity player, ServerWorld currentWorld, NbtCompound anchorTag) {
        double x = anchorTag.getDouble(X_KEY);
        double y = anchorTag.getDouble(Y_KEY);
        double z = anchorTag.getDouble(Z_KEY);
        String levelIdString = anchorTag.getString(LEVEL_KEY);

        MinecraftServer server = currentWorld.getServer();

        Identifier targetLevelId = Identifier.of(levelIdString);
        RegistryKey<World> targetKey = RegistryKey.of(RegistryKeys.WORLD, targetLevelId);
        ServerWorld targetWorld = server.getWorld(targetKey);

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (targetWorld == null) {
            return;
        }

        if (anchorTag.contains(ENTITY_ID_KEY)) {
            int entityId = anchorTag.getInt(ENTITY_ID_KEY);
            Entity targetEntity = targetWorld.getEntityById(entityId);

            if (targetEntity != null && targetEntity.isAlive()) {
                Vec3d entityPos = targetEntity.getPos();
                x = entityPos.x;
                y = entityPos.y;
                z = entityPos.z;

                Vec3d offset = targetEntity.getRotationVector().multiply(-1.5);
                x += offset.x;
                z += offset.z;

            } else {
            }
        } else {
        }

        serverPlayer.teleport(targetWorld, x, y, z, player.getYaw(), player.getPitch());

        currentWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0F, 0.4F);

        targetWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.PORTAL,
                x, y + 1.0, z, 50, 0.5, 0.5, 0.5, 0.0
        );

        // 修正点 2：在执行任何组件操作前，对 stackToModify 再次进行空栈检查
        ItemStack stackToModify = player.getStackInHand(player.getActiveHand());
        if (stackToModify.isEmpty()) return;

        @Nullable
        NbtComponent nbtComponent = stackToModify.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound rootTag = nbtComponent.copyNbt();
            rootTag.remove(ANCHOR_TAG);

            if (rootTag.isEmpty()) {
                stackToModify.remove(DataComponentTypes.CUSTOM_DATA);
            } else {
                stackToModify.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootTag));
            }
        }
        stackToModify.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
    }
}