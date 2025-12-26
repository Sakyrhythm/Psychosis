package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.entity.custom.DarkDartProjectile;
import com.sakyrhythm.psychosis.entity.custom.FlatDartProjectile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 假设 ModToolMaterials 存在且已导入
// 注意：FlatDartProjectile, DarkDartProjectile, ModToolMaterials 需要您自行定义

public class DarkSwordItem extends SwordItem {

    private static final Logger LOGGER = LoggerFactory.getLogger("DarkSword");

    // --- NBT 锚点常量 ---
    private static final String ANCHOR_TAG = "DarkSwordAnchor";
    private static final String HAS_ANCHOR_KEY = "HasAnchor";
    private static final String X_KEY = "AnchorX";
    private static final String Y_KEY = "AnchorY";
    private static final String Z_KEY = "AnchorZ";
    private static final String LEVEL_KEY = "AnchorLevel";
    private static final String ENTITY_ID_KEY = "AnchorEntityId";
    private static final String ORIGINAL_HEALTH_KEY = "OriginalHealth";

    // --- 属性修改器常量 (扣除生命值上限) ---
    private static final Identifier MAX_HEALTH_MODIFIER_ID = Identifier.of("psychosis", "dark_sword_anchor_health");
    private static final UUID MAX_HEALTH_MODIFIER_UUID = UUID.fromString("9D13028C-519A-40B3-B549-F6A22B96102A");
    // 使用 ADD_MULTIPLIED_TOTAL 操作，这里的值 -0.5 意味着生命值上限降低 50%
    private static final double MAX_HEALTH_MODIFIER_VALUE = -0.5;
    // FlatDart 基础伤害
    private static final double BASE_FLAT_DART_DAMAGE = 6.0;
    // 每级黑暗缠绕为 FlatDart 增加的额外伤害
    private static final double FLAT_DART_DAMAGE_PER_AMPLIFIER = 3.0;

    public DarkSwordItem(Settings settings) {
        // 假设 ModToolMaterials.DARK 在您的项目中已定义
        super(ModToolMaterials.DARK, settings);
    }

    // =========================================================================================
    // 属性修改器管理方法
    // =========================================================================================

    private static void applyMaxHealthModifier(PlayerEntity player) {
        if (!player.getWorld().isClient()) {
            EntityAttributeInstance maxHealthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (maxHealthAttribute != null) {
                // 先移除，确保没有重复
                maxHealthAttribute.removeModifier(MAX_HEALTH_MODIFIER_ID);

                // 创建属性修改器：将 Max Health 降低 50%
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                        MAX_HEALTH_MODIFIER_ID,
                        MAX_HEALTH_MODIFIER_VALUE,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );
                // 确保修改器持久化，即使玩家退出重进也能保持
                maxHealthAttribute.addPersistentModifier(modifier);

                // 检查当前生命值是否超过新的最大生命值，如果是则扣血
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    private static void removeMaxHealthModifier(PlayerEntity player) {
        if (!player.getWorld().isClient()) {
            EntityAttributeInstance maxHealthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (maxHealthAttribute != null) {
                if (maxHealthAttribute.removeModifier(MAX_HEALTH_MODIFIER_ID)) {
                    LOGGER.info("{} removed max health modifier.", player.getName().getString());
                }
            }
        }
    }

    // =========================================================================================
    // NBT 辅助方法
    // =========================================================================================

    @Nullable
    private NbtCompound getAnchorTag(ItemStack stack) {
        if (stack.getItem() != this) return null;

        @Nullable
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound rootNbt = nbtComponent.copyNbt();
            if (rootNbt.contains(ANCHOR_TAG)) {
                NbtCompound anchorTag = rootNbt.getCompound(ANCHOR_TAG);
                if (anchorTag.getBoolean(HAS_ANCHOR_KEY)) {
                    return anchorTag;
                }
            }
        }
        return null;
    }

    private boolean isSwordAnchored(ItemStack stack) {
        return getAnchorTag(stack) != null;
    }

    /**
     * 解除锚定，同步属性，可选恢复血量。
     * @param stack 要修改的物品堆栈。
     * @param player 拥有该物品的玩家。
     * @param healthToRestore 恢复的血量值 (如果为 null 则不恢复)。
     */
    private void disengageAnchorAndSync(ItemStack stack, PlayerEntity player, @Nullable Float healthToRestore) {
        // 步骤 1: 清除当前剑的锚点 NBT
        @Nullable
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound rootTag = nbtComponent.copyNbt();
            rootTag.remove(ANCHOR_TAG);
            rootTag.remove(ORIGINAL_HEALTH_KEY); // 清除原始血量

            if (rootTag.isEmpty()) {
                stack.remove(DataComponentTypes.CUSTOM_DATA);
            } else {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootTag));
            }
        }
        // 清除附魔光效覆盖
        stack.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);

        // 步骤 2: 检查玩家背包是否还有其他锚定剑
        if (!playerHasAnchoredDarkSword(player)) {
            // 步骤 3: 如果没有，立即移除 Max Health 修改器
            removeMaxHealthModifier(player);
        }

        // 步骤 4: 恢复玩家血量 (仅当 healthToRestore 不为 null 时)
        if (healthToRestore != null) {
            // 确保恢复的血量不超过新的 Max Health
            player.setHealth(Math.min(healthToRestore, player.getMaxHealth()));
            LOGGER.info("{} health restored to {}.", player.getName().getString(), healthToRestore);
        }
    }
    public static boolean isSwordAnchoredStatic(ItemStack stack) {
        if (!(stack.getItem() instanceof DarkSwordItem item)) return false;
        return item.isSwordAnchored(stack);
    }

    /**
     * 静态版本: 检查玩家背包中是否有锚定剑。
     */
    public static boolean playerHasAnchoredDarkSwordStatic(PlayerEntity player) {
        DarkSwordItem item = (DarkSwordItem) Items.IRON_SWORD; // 仅用于获取实例，假设所有 DarkSwordItem 行为一致
        // 实际的实现应该直接使用 DarkSwordItem.isSwordAnchored(stack)

        // 检查主物品栏
        for (ItemStack itemStack : player.getInventory().main) {
            if (isSwordAnchoredStatic(itemStack)) {
                return true;
            }
        }
        // 检查副手
        if (isSwordAnchoredStatic(player.getOffHandStack())) return true;

        // 检查盔甲栏
        for (ItemStack armor : player.getInventory().armor) {
            if (isSwordAnchoredStatic(armor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 静态版本: 移除 Max Health 修改器。
     */
    public static void removeMaxHealthModifierStatic(PlayerEntity player) {
        removeMaxHealthModifier(player); // 调用私有/静态方法
    }

    /**
     * 【新增/通用】处理物品被玩家移除（丢弃、清除、死亡清空）时的锚定解除和血量恢复。
     * * 此方法旨在被外部 Mixin 拦截 PlayerInventory 的清空或移除事件时调用。
     * * @param stack 被移除的物品堆栈。
     * @param player 物品所属的玩家。
     */
    public static void handleSwordRemoval(ItemStack stack, PlayerEntity player) {
        // 确保被处理的物品是 DarkSwordItem
        if (stack.getItem() instanceof DarkSwordItem darkSwordItem) {

            // 确保剑是锚定状态
            if (darkSwordItem.isSwordAnchored(stack)) {

                NbtCompound anchorTag = darkSwordItem.getAnchorTag(stack);

                // 获取原始血量值，用于恢复
                Float originalHealth = anchorTag != null && anchorTag.contains(ORIGINAL_HEALTH_KEY)
                        ? anchorTag.getFloat(ORIGINAL_HEALTH_KEY)
                        : null;

                // 关键步骤：解除锚定，清理 NBT，并恢复血量/Max Health属性
                darkSwordItem.disengageAnchorAndSync(stack, player, originalHealth);

                LOGGER.info("{} DarkSword removed (clear/drop/death) and anchor disengaged, health restored.", player.getName().getString());
            }
        }
    }

    // =========================================================================================
    // 锚点保存逻辑 (saveAnchor)
    // =========================================================================================

    public static void saveAnchor(ItemStack swordStack, PlayerEntity player, Hand hand, World hitWorld, Vec3d hitPos, int entityId) {
        // ... (保持不变)
        ItemStack stackToModify = player.getStackInHand(hand);
        if (stackToModify.isEmpty() || !(stackToModify.getItem() instanceof DarkSwordItem)) return;

        float currentHealth = player.getHealth();

        @Nullable
        NbtComponent nbtComponent = stackToModify.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound rootNbt = (nbtComponent != null) ? nbtComponent.copyNbt() : new NbtCompound();
        NbtCompound anchorTag = rootNbt.getCompound(ANCHOR_TAG);

        // 保存锚点 NBT
        anchorTag.putBoolean(HAS_ANCHOR_KEY, true);
        anchorTag.putDouble(X_KEY, hitPos.x);
        anchorTag.putDouble(Y_KEY, hitPos.y);
        anchorTag.putDouble(Z_KEY, hitPos.z);
        // 保存锚定时的玩家血量，用于解除锚定后恢复
        anchorTag.putFloat(ORIGINAL_HEALTH_KEY, currentHealth);

        RegistryKey<World> dimensionKey = hitWorld.getRegistryKey();
        anchorTag.putString(LEVEL_KEY, dimensionKey.getValue().toString());

        if (entityId != -1) {
            anchorTag.putInt(ENTITY_ID_KEY, entityId);
        } else if (anchorTag.contains(ENTITY_ID_KEY)) {
            anchorTag.remove(ENTITY_ID_KEY);
        }

        rootNbt.put(ANCHOR_TAG, anchorTag);

        stackToModify.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootNbt));
        // 强制显示附魔光效
        stackToModify.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        if (!player.getWorld().isClient()) {
            // 锚定成功后，应用 Max Health 修改器
            applyMaxHealthModifier(player);
        }

        LOGGER.info("ANCHOR SAVED: Pos={}, EntityID={}, Health={}, NBT={}", hitPos, entityId, currentHealth, anchorTag.asString());
    }

    // =========================================================================================
    // 物品使用逻辑 (use) - 右键
    // ... (保持不变)
    // =========================================================================================

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        // ... (use 逻辑保持不变)
        if (world.isClient()) {
            player.swingHand(hand, true);
            return TypedActionResult.pass(stack);
        }

        ServerWorld sw = (ServerWorld) world;

        if (stack.isEmpty() || player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        NbtCompound anchorTag = getAnchorTag(stack);

        if (anchorTag != null) {
            // --- 锚点存在：执行传送 (高冷却) ---
            teleportPlayer(stack, player, sw, anchorTag);
            player.getItemCooldownManager().set(this, 20); // 传送冷却 (1秒)
            player.swingHand(hand, true);
            return TypedActionResult.success(stack);
        } else {
            // --- 锚点不存在：发射飞镖 (低冷却) ---
            launchProjectile(sw, player, stack, hand);
            player.getItemCooldownManager().set(this, 2); // 飞镖冷却 (0.1秒)
            player.swingHand(hand, true);
            return TypedActionResult.success(stack);
        }
    }

    // =========================================================================================
    // 右键发射飞镖逻辑 (launchProjectile)
    // ... (保持不变)
    // =========================================================================================
    private void launchProjectile(ServerWorld world, PlayerEntity player, ItemStack stack, Hand hand) {
        if (stack.isEmpty()) return;

        // 发射 DarkDartProjectile (飞镖)
        DarkDartProjectile dart = new DarkDartProjectile(world, player, stack, hand);
        world.spawnEntity(dart);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
    }

    // =========================================================================================
    // 左键发射剑气逻辑 (launchSingleFlatProjectile)
    // ... (保持不变)
    // =========================================================================================

    private void launchSingleFlatProjectile(ServerWorld world, PlayerEntity player, ItemStack stack, Hand hand) {
        LOGGER.info("Launching a single FlatDartProjectile (Sword Wave).");

        float eyeHeight = player.getEyeHeight(EntityPose.STANDING);
        // 关键点：创建 FlatDartProjectile (剑气实体)
        FlatDartProjectile dart = new FlatDartProjectile(world, player, stack, hand);

        float targetPitch = player.getPitch();
        float targetYaw = player.getYaw();

        // 在玩家眼睛高度附近生成剑气
        dart.setPosition(player.getX(), player.getY() + eyeHeight - 0.1f, player.getZ());

        // 设置剑气的速度和方向 (确保直线飞行)
        dart.setVelocity(player, targetPitch, targetYaw, 0.0F, 1.8F, 1.0F);
        dart.setDamage(6.0);
        world.spawnEntity(dart);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
    }
    // ... (launchSequentialProjectiles 已移除)

    // =========================================================================================
    // 传送逻辑 (teleportPlayer) - 右键触发
    // ... (保持不变)
    // =========================================================================================
    private void teleportPlayer(ItemStack swordStack, PlayerEntity player, ServerWorld currentWorld, NbtCompound anchorTag) {
        // ... (逻辑保持不变)
        float savedHealth = player.getHealth();
        boolean hasSavedHealth = anchorTag.contains(ORIGINAL_HEALTH_KEY);
        if (hasSavedHealth) {
            savedHealth = anchorTag.getFloat(ORIGINAL_HEALTH_KEY);
        }

        double x = anchorTag.getDouble(X_KEY);
        double y = anchorTag.getDouble(Y_KEY);
        double z = anchorTag.getDouble(Z_KEY);
        String levelIdString = anchorTag.getString(LEVEL_KEY);

        MinecraftServer server = currentWorld.getServer();

        Identifier targetLevelId = Identifier.of(levelIdString);
        RegistryKey<World> targetKey = RegistryKey.of(RegistryKeys.WORLD, targetLevelId);
        ServerWorld targetWorld = server.getWorld(targetKey);

        if (!(player instanceof ServerPlayerEntity serverPlayer) || targetWorld == null) {
            return;
        }

        // 检查是否有实体锚点
        if (anchorTag.contains(ENTITY_ID_KEY)) {
            int entityId = anchorTag.getInt(ENTITY_ID_KEY);
            Entity targetEntity = targetWorld.getEntityById(entityId);

            if (targetEntity != null && targetEntity.isAlive()) {
                Vec3d entityPos = targetEntity.getPos();
                x = entityPos.x;
                y = entityPos.y;
                z = entityPos.z;

                // 传送到目标实体的身后 1.5 格
                Vec3d offset = targetEntity.getRotationVector().multiply(-1.5);
                x += offset.x;
                z += offset.z;
            }
        }

        // 执行跨维度或同维度的传送
        serverPlayer.teleport(targetWorld, x, y, z, player.getYaw(), player.getPitch());

        // 声音和粒子效果
        currentWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0F, 0.4F);

        targetWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.PORTAL,
                x, y + 1.0, z, 50, 0.5, 0.5, 0.5, 0.0
        );

        // 确保 NBT 清理发生在当前活跃的 ItemStack 上
        ItemStack stackToModify = player.getStackInHand(player.getActiveHand());
        if (stackToModify.isEmpty()) return;

        // 解除锚定并同步属性 (恢复血量)
        disengageAnchorAndSync(stackToModify, player, hasSavedHealth ? savedHealth : null);
    }


    // =========================================================================================
    // 左键攻击逻辑 (postHit) - 核心逻辑 (已包含施加黑暗效果和解除锚定)
    // =========================================================================================

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (attacker instanceof PlayerEntity player && !attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            boolean isAnchored = isSwordAnchored(stack);

            if (isAnchored) {
                // *** 锚定状态下左键攻击 ***

                // --- 1. 施加黑暗纠缠效果 (保持不变) ---
                RegistryKey<net.minecraft.entity.effect.StatusEffect> darkEffectKey =
                        RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark"));

                Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntry =
                        world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT).getEntry(darkEffectKey);

                if (darkEffectEntry.isPresent()) {
                    final int DURATION_TICKS = 10; // 0.5 秒
                    StatusEffectInstance darkEntanglement = new StatusEffectInstance(
                            darkEffectEntry.get(),
                            DURATION_TICKS,
                            0,
                            false,
                            false,
                            false
                    );
                    target.addStatusEffect(darkEntanglement);
                    LOGGER.info("Applied Dark Entanglement ({} ticks) to {} due to anchored hit.", DURATION_TICKS, target.getName().getString());
                }
                // --- 施加黑暗纠缠效果结束 ---


                // 2. 发射单道剑气 (FlatDartProjectile) (保持不变)
                launchSingleFlatProjectile(world, player, stack, player.getActiveHand());

                // 3. 解除锚定逻辑
                // 注意：我们仍然获取 originalHealth，但将其忽略，只传递 null 给 disengageAnchorAndSync
                // NbtCompound anchorTag = getAnchorTag(stack);
                // Float originalHealth = anchorTag != null && anchorTag.contains(ORIGINAL_HEALTH_KEY) ? anchorTag.getFloat(ORIGINAL_HEALTH_KEY) : null;

                // 【关键修正】：传入 null 作为 healthToRestore 参数
                disengageAnchorAndSync(stack, player, null);
                LOGGER.info("Disengaging anchor due to attack (No health restoration).");

                // 4. 应用冷却 (保持不变)
                player.getItemCooldownManager().set(this, 10); // 0.5 秒冷却

                // 5. 确保默认伤害结算 (保持不变)
                return super.postHit(stack, target, attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {

        // 只有当剑处于锚定状态时才添加信息
        NbtCompound anchorTag = getAnchorTag(stack); // 使用现有的获取锚点NBT的方法

        if (anchorTag != null) {

            // 锚定状态提示
            tooltip.add(Text.translatable("tooltip.psychosis.dark_sword.anchored")
                    .styled(style -> style.withColor(0xAA00AA))); // 深紫色

            // --- 检查是否锚定至实体 ---
            if (anchorTag.contains(ENTITY_ID_KEY)) {
                // 锚定至实体：只显示通用翻译文本
                tooltip.add(Text.translatable("tooltip.psychosis.dark_sword.anchor_to_entity")
                        .styled(style -> style.withColor(0xFFFF00))); // 黄色
            } else {
                // 锚定至方块：显示位置信息
                double x = anchorTag.getDouble(X_KEY);
                double y = anchorTag.getDouble(Y_KEY);
                double z = anchorTag.getDouble(Z_KEY);

                // 格式化输出位置信息 (只保留一位小数)
                tooltip.add(Text.translatable("tooltip.psychosis.dark_sword.anchor_pos",
                                String.format("%.1f", x),
                                String.format("%.1f", y),
                                String.format("%.1f", z))
                        .styled(style -> style.withColor(0x8888FF))); // 浅蓝色
            }

            // 读取维度信息
            String levelIdString = anchorTag.getString(LEVEL_KEY);
            tooltip.add(Text.translatable("tooltip.psychosis.dark_sword.anchor_dim", levelIdString)
                    .styled(style -> style.withColor(0xAA0000))); // 红色

        } else {
            // 非锚定状态提示 (可选，提示玩家如何锚定)
            tooltip.add(Text.translatable("tooltip.psychosis.dark_sword.unanchored")
                    .styled(style -> style.withColor(0xAAAAAA))); // 灰色
        }
    }

    // =========================================================================================
    // 物品背包检查 (inventoryTick)
    // ... (保持不变)
    // =========================================================================================

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient() || !(entity instanceof PlayerEntity player)) {
            return;
        }

        boolean hasModifier = hasMaxHealthModifier(player);
        boolean playerHasAnchor = playerHasAnchoredDarkSword(player);

        // 同步 Max Health 修改器状态
        if (playerHasAnchor && !hasModifier) {
            applyMaxHealthModifier(player);
        } else if (!playerHasAnchor && hasModifier) {
            removeMaxHealthModifier(player);
        }

        // 额外的清理逻辑（例如检查锚定实体是否死亡）可以在这里实现
        // ...
    }


    // =========================================================================================
    // 辅助检查方法
    // ... (保持不变)
    // =========================================================================================

    private boolean hasMaxHealthModifier(PlayerEntity player) {
        EntityAttributeInstance maxHealthAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        // 使用 MAX_HEALTH_MODIFIER_ID（Identifier）进行检查
        return maxHealthAttribute != null && maxHealthAttribute.getModifier(MAX_HEALTH_MODIFIER_ID) != null;
    }

    private boolean playerHasAnchoredDarkSword(PlayerEntity player) {
        // 检查主物品栏
        for (ItemStack itemStack : player.getInventory().main) {
            if (isSwordAnchored(itemStack)) {
                return true;
            }
        }
        // 检查副手
        if (isSwordAnchored(player.getOffHandStack())) return true;

        // 检查盔甲栏 (通常不会有，但为了完整性检查)
        for (ItemStack armor : player.getInventory().armor) {
            if (isSwordAnchored(armor)) {
                return true;
            }
        }
        return false;
    }
}