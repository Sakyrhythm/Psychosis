package com.sakyrhythm.psychosis.item;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.custom.DarkDartProjectile;
import com.sakyrhythm.psychosis.entity.custom.FlatDartProjectile;
import com.sakyrhythm.psychosis.entity.custom.WhirlwindSlashEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
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
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

// 假设 ModToolMaterials 存在且已导入
public class DarkSwordItem extends SwordItem {

    private static final Logger LOGGER = LoggerFactory.getLogger("DarkSword");

    private static final RegistryKey<StatusEffect> DARK_EFFECT_KEY = RegistryKey.of(
            RegistryKeys.STATUS_EFFECT,
            net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")
    );
    RegistryEntry<StatusEffect> darkEffectEntry = Registries.STATUS_EFFECT
            .getEntry(DARK_EFFECT_KEY)
            .orElse(null);
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
    private static final double MAX_HEALTH_MODIFIER_VALUE = -0.5;

    // --- 圆月斩 AOE 常量 ---
    private static final double WHIRLWIND_AOE_RANGE = 4.0;
    private static final float WHIRLWIND_BASE_DAMAGE = 12.0f; // 【修正：基础伤害 12.0f】
    private static final float DARKDART_BASE_DAMAGE = 15.0f; // 【修正：基础伤害 12.0f】
    private static final float DARKDART_DAMAGE_PER_AMPLIFIER = 3.0f; // 【新增：每级黑暗纠缠 2.0f】
    private static final float WHIRLWIND_DAMAGE_MULTIPLIER = 0.5f;
    private static final float WHIRLWIND_DAMAGE_PER_AMPLIFIER = 2.0f; // 【新增：每级黑暗纠缠 2.0f】

    // 黑暗纠缠状态效果的 Identifier，用于获取等级
    private static final Identifier DARK_EFFECT_ID = Identifier.of("psychosis", "dark");


    public DarkSwordItem(Settings settings) {
        // 假设 ModToolMaterials.DARK 在您的项目中已定义
        super(ModToolMaterials.DARK, settings);
    }

    // =========================================================================================
    // 属性修改器管理方法 (保持不变)
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
    // NBT 辅助方法 (保持不变)
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

    public static boolean playerHasAnchoredDarkSwordStatic(PlayerEntity player) {
        // 实际的实现应该直接使用 DarkSwordItem.isSwordAnchored(stack)
        for (ItemStack itemStack : player.getInventory().main) {
            if (isSwordAnchoredStatic(itemStack)) {
                return true;
            }
        }
        if (isSwordAnchoredStatic(player.getOffHandStack())) return true;
        for (ItemStack armor : player.getInventory().armor) {
            if (isSwordAnchoredStatic(armor)) {
                return true;
            }
        }
        return false;
    }

    public static void removeMaxHealthModifierStatic(PlayerEntity player) {
        removeMaxHealthModifier(player); // 调用私有/静态方法
    }

    public static void handleSwordRemoval(ItemStack stack, PlayerEntity player) {
        if (stack.getItem() instanceof DarkSwordItem darkSwordItem) {
            if (darkSwordItem.isSwordAnchored(stack)) {
                NbtCompound anchorTag = darkSwordItem.getAnchorTag(stack);
                Float originalHealth = anchorTag != null && anchorTag.contains(ORIGINAL_HEALTH_KEY)
                        ? anchorTag.getFloat(ORIGINAL_HEALTH_KEY)
                        : null;
                darkSwordItem.disengageAnchorAndSync(stack, player, originalHealth);
                LOGGER.info("{} DarkSword removed (clear/drop/death) and anchor disengaged, health restored.", player.getName().getString());
            }
        }
    }

    // =========================================================================================
    // 锚点保存逻辑 (saveAnchor) (保持不变)
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
    // 物品使用逻辑 (use) (移除飞镖冷却，实体优先锚定)
    // =========================================================================================

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

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

            // 【修正：冷却时间翻倍至 40 刻 (2秒)】
            player.getItemCooldownManager().set(this, 40);

            player.swingHand(hand, true);
            return TypedActionResult.success(stack);
        } else {
            // --- 锚点不存在：执行实体/方块检测或发射飞镖 ---

            final double MAX_DISTANCE = 50.0;

            // 1. 手动计算射线终点
            Vec3d playerEyePos = player.getEyePos();
            Vec3d rotationVector = player.getRotationVector();
            Vec3d endPos = playerEyePos.add(rotationVector.multiply(MAX_DISTANCE));

            // 2. 方块射线检测 (Block Raycasting)
            RaycastContext rayCtx = new RaycastContext(
                    playerEyePos,
                    endPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            );
            BlockHitResult blockHit = world.raycast(rayCtx);

            // 3. 实体射线检测 (Entity Raycasting) - 寻找LivingEntity

            Vec3d blockHitPos = blockHit.getPos();

            // 计算实体检测的最大终点：方块命中点或最大距离
            double entityCheckDistance = blockHit.getType() != HitResult.Type.MISS
                    ? playerEyePos.distanceTo(blockHitPos)
                    : MAX_DISTANCE;

            // 实体检测：寻找最近的 LivingEntity
            EntityHitResult entityHit = ProjectileUtil.raycast(
                    player,
                    playerEyePos,
                    playerEyePos.add(player.getRotationVector().multiply(entityCheckDistance)),
                    player.getBoundingBox().stretch(player.getRotationVector().multiply(entityCheckDistance)).expand(1.0),
                    (entity) -> entity instanceof LivingEntity && !entity.isSpectator() && entity.canHit(),
                    entityCheckDistance * entityCheckDistance
            );

            // --- 目标处理与锚定 ---

            if (entityHit != null) {
                // ** 实体命中：立即锚定！**
                Entity targetEntity = entityHit.getEntity();
                Vec3d targetHitPos = entityHit.getPos();
                int playerDarkLevel = getDarkEntanglementAmplifier(player); // <-- 检查玩家自身的效果
                float darkBonusDamage = playerDarkLevel * DARKDART_DAMAGE_PER_AMPLIFIER;


                if (targetEntity instanceof LivingEntity target) {
                    saveAnchor(stack, player, hand, sw, targetHitPos, targetEntity.getId());
                    // 飞镖音效 (Warden Roar) 在锚定成功时取消，只播放锚点音效
                    //player.getItemCooldownManager().set(this, 40); // 锚定成功
                    player.swingHand(hand, true);
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,
                            SoundCategory.PLAYERS, 1.0F, 1.0F);
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_WARDEN_ROAR,
                            SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
                    float totalDamage = DARKDART_BASE_DAMAGE;
                    if (targetEntity!= player) {
                        // 2. 武器攻击力附加伤害 (可选)
                        float baseAttackDamage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                        totalDamage += (baseAttackDamage * WHIRLWIND_DAMAGE_MULTIPLIER);

                        // 3. 应用玩家自身的黑暗纠缠伤害加成
                        totalDamage += darkBonusDamage; // <-- 使用预先计算好的加成
                        if (darkEffectEntry != null) {
                            // 检查目标是否已经拥有该效果
                            if (!target.hasStatusEffect(darkEffectEntry)) { // <--- 修正: 使用 targetEntity 实例
                                final int DURATION_TICKS = 10;
                                final int AMPLIFIER = 0;

                                target.addStatusEffect(new StatusEffectInstance( // <--- 修正: 使用 targetEntity 实例
                                        darkEffectEntry,
                                        DURATION_TICKS,
                                        AMPLIFIER,
                                        false,
                                        false,
                                        true
                                ));
                            } else {
                                // 空的 else 块可以删除，或者添加日志
                            }
                        }                  // 造成伤害
                        DamageSources damageSources = target.getWorld().getDamageSources();
                        target.damage(damageSources.playerAttack(player), totalDamage);
                    }
                    return TypedActionResult.success(stack);
                }
            }

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                // ** 方块命中：发射飞镖 (无冷却) **
                if (playerEyePos.distanceTo(blockHitPos) > 1.0) { // 防止太近的方块误判
                    launchProjectile(sw, player, stack, hand);
                    // 【已移除冷却设置】
                    player.swingHand(hand, true);
                    return TypedActionResult.success(stack);
                }
            }

            // 未击中有效目标 (MISS)
            if (player instanceof ServerPlayerEntity) {
                //player.sendMessage(Text.translatable("tooltip.psychosis.dark_sword.no_target").styled(style -> style.withColor(Formatting.RED)), true);
            }
            return TypedActionResult.fail(stack);
        }
    }

    // =========================================================================================
    // 右键发射飞镖逻辑 (launchProjectile) (音效已更改)
    // =========================================================================================
    private void launchProjectile(ServerWorld world, PlayerEntity player, ItemStack stack, Hand hand) {
        if (stack.isEmpty()) return;

        // 发射 DarkDartProjectile (飞镖)
        DarkDartProjectile dart = new DarkDartProjectile(world, player, stack, hand);
        world.spawnEntity(dart);
    }


    // =========================================================================================
    // 左键发射剑气逻辑 (launchSingleFlatProjectile) (已修改音效为 ENTITY_WITHER_SHOOT)
    // =========================================================================================
    private void launchSingleFlatProjectile(ServerWorld world, PlayerEntity player, ItemStack stack, Hand hand) {
        LOGGER.info("Launching a single FlatDartProjectile (Sword Wave).");

        // 创建剑气实体
        FlatDartProjectile dart = new FlatDartProjectile(world, player, stack, hand);

        // 获取玩家面向的方向（完全不考虑玩家速度）
        Vec3d lookDirection = Vec3d.fromPolar(player.getPitch(), player.getYaw()).normalize();

        // 在玩家眼睛高度附近生成剑气
        float eyeHeight = player.getEyeHeight(EntityPose.STANDING);

        // 将生成位置向前移动一小段距离，避免与玩家碰撞
        double spawnOffset = 0.5;
        Vec3d spawnPos = player.getEyePos().add(lookDirection.multiply(spawnOffset));

        // 设置剑气位置
        dart.setPosition(spawnPos.x, spawnPos.y - 0.1, spawnPos.z);

        // 基础速度 - 固定值，不受玩家速度影响
        double baseSpeed = 2.2;

        // 设置剑气速度 - 只使用看向的方向
        dart.setVelocity(lookDirection.x * baseSpeed,
                lookDirection.y * baseSpeed,
                lookDirection.z * baseSpeed);

        // 设置伤害
        dart.setDamage(6.0);

        world.spawnEntity(dart);

        // 播放音效
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
    }

    // =========================================================================================
    // 传送逻辑 (teleportPlayer) - 右键触发 (音效已更改)
    // =========================================================================================
    private void teleportPlayer(ItemStack swordStack, PlayerEntity player, ServerWorld currentWorld, NbtCompound anchorTag) {

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

        // 声音和粒子效果 (传送音效)
        currentWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SHULKER_TELEPORT, // 传送音效保持 Shulker Teleport，因为它很独特
                SoundCategory.PLAYERS, 1.0F, 0.4F);

        targetWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.PORTAL,
                x, y + 1.0, z, 50, 0.5, 0.5, 0.5, 0.0
        );
        // --- 圆月斩 AOE 逻辑 ---

        // 1. 【核心修正】：预先计算玩家自身的黑暗纠缠伤害加成
        int playerDarkLevel = getDarkEntanglementAmplifier(player); // <-- 检查玩家自身的效果
        float darkBonusDamage = playerDarkLevel * WHIRLWIND_DAMAGE_PER_AMPLIFIER;

        // 2. 定义 AOE 区域 (以玩家为中心)
        Box aoeBox = new Box(
                x - WHIRLWIND_AOE_RANGE, y - 1.0, z - WHIRLWIND_AOE_RANGE,
                x + WHIRLWIND_AOE_RANGE, y + 2.0, z + WHIRLWIND_AOE_RANGE
        );

        // 3. 遍历 AOE 范围内的实体
        targetWorld.getOtherEntities(player, aoeBox, entity -> entity instanceof LivingEntity)
                .forEach(entity -> {
                    if (entity != player) {
                        LivingEntity targetEntity = (LivingEntity) entity;

                        // --- 伤害计算修正 ---
                        // 1. 基础伤害 (12.0f)
                        float totalDamage = WHIRLWIND_BASE_DAMAGE;

                        // 2. 武器攻击力附加伤害 (可选)
                        float baseAttackDamage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                        totalDamage += (baseAttackDamage * WHIRLWIND_DAMAGE_MULTIPLIER);

                        // 3. 应用玩家自身的黑暗纠缠伤害加成
                        totalDamage += darkBonusDamage; // <-- 使用预先计算好的加成
                        if (darkEffectEntry != null) {
                            // 检查目标是否已经拥有该效果
                            if (!targetEntity.hasStatusEffect(darkEffectEntry)) { // <--- 修正: 使用 targetEntity 实例
                                final int DURATION_TICKS = 10;
                                final int AMPLIFIER = 0;

                                targetEntity.addStatusEffect(new StatusEffectInstance( // <--- 修正: 使用 targetEntity 实例
                                        darkEffectEntry,
                                        DURATION_TICKS,
                                        AMPLIFIER,
                                        false,
                                        false,
                                        true
                                ));
                            } else {
                                // 空的 else 块可以删除，或者添加日志
                            }
                        }                  // 造成伤害
                        DamageSources damageSources = targetWorld.getDamageSources();
                        targetEntity.damage(damageSources.playerAttack(player), totalDamage);

                        // AOE 粒子效果 (可选)
                        targetWorld.spawnParticles(
                                ParticleTypes.CRIT,
                                targetEntity.getX(),
                                targetEntity.getY() + targetEntity.getHeight() / 2.0,
                                targetEntity.getZ(),
                                5, 0.1, 0.1, 0.1, 0.0
                        );
                    }
                });

        // 3. 生成圆月斩特效实体 (客户端渲染)
        WhirlwindSlashEntity slash = new WhirlwindSlashEntity(targetWorld, player);

        // 【生成位置修正】：调整 Y 轴偏移量，将特效抬高。
        final double Y_OFFSET = 1.1f;

        slash.setPos(x, y + Y_OFFSET, z); // 生成在传送后的位置，并向上抬高
        targetWorld.spawnEntity(slash);

        // AOE 成功声音 【音效已更改】
        targetWorld.playSound(null, x, y, z,
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, // <--- 已更改为 Warden Sonic Boom
                SoundCategory.PLAYERS, 1.0F, 0.6F + targetWorld.random.nextFloat() * 0.4F);

        // --- 圆月斩 AOE 逻辑结束 ---

        // 确保 NBT 清理发生在当前活跃的 ItemStack 上
        ItemStack stackToModify = player.getStackInHand(player.getActiveHand());
        if (stackToModify.isEmpty()) return;

        // 解除锚定并同步属性 (恢复血量)
        disengageAnchorAndSync(stackToModify, player, hasSavedHealth ? savedHealth : null);
    }


    // =========================================================================================
    // 左键攻击逻辑 (postHit) (保持不变)
    // =========================================================================================
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (attacker instanceof PlayerEntity player && !attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            boolean isAnchored = isSwordAnchored(stack);

            if (isAnchored) {
                // *** 锚定状态下左键攻击 ***

                // --- 1. 施加黑暗纠缠效果 (添加条件检查) ---
                RegistryKey<net.minecraft.entity.effect.StatusEffect> darkEffectKey =
                        RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of("psychosis", "dark"));

                Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntry =
                        world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT).getEntry(darkEffectKey);

                if (darkEffectEntry.isPresent()) {
                    // 【关键修正点】：检查目标实体是否已经拥有该效果
                    if (!target.hasStatusEffect(darkEffectEntry.get())) {

                        final int DURATION_TICKS = 10; // 0.5 秒
                        StatusEffectInstance darkEntanglement = new StatusEffectInstance(
                                darkEffectEntry.get(),
                                DURATION_TICKS,
                                0, // Amplifier 0 = Level 1
                                false,
                                false,
                                false
                        );
                        target.addStatusEffect(darkEntanglement);
                        LOGGER.info("Applied Dark Entanglement ({} ticks) to {} due to anchored hit.", DURATION_TICKS, target.getName().getString());
                    } else {
                        LOGGER.info("{} already has Dark Entanglement. Skipping reapplication on postHit.", target.getName().getString());
                    }
                }
                // --- 施加黑暗纠缠效果结束 ---


                // 2. 发射单道剑气 (FlatDartProjectile) (保持不变)
                launchSingleFlatProjectile(world, player, stack, player.getActiveHand());

                // 3. 解除锚定逻辑
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
    // 在 DarkSwordItem.java 中添加这个方法
    /**
     * 统一的左键点击处理逻辑
     * 在所有左键点击事件中调用（攻击实体、挖掘方块、点空、GUI点击等）
     */
    public void handleAnyLeftClick(ItemStack stack, PlayerEntity player, @Nullable Entity target, String clickSource) {
        if (stack.isEmpty() || !(stack.getItem() instanceof DarkSwordItem)) return;
        if (player.getWorld().isClient()) return; // 只在服务端执行逻辑

        ServerWorld world = (ServerWorld) player.getWorld();
        boolean isAnchored = isSwordAnchored(stack);

        LOGGER.info("Left click triggered from: {} with target: {}, isAnchored: {}",
                clickSource, target != null ? target.getName().getString() : "none", isAnchored);

        if (isAnchored) {
            // === 锚定状态下的左键逻辑 ===

            // --- 1. 如果目标是实体，施加黑暗纠缠效果 ---
            if (target instanceof LivingEntity livingTarget) {
                RegistryKey<StatusEffect> darkEffectKey =
                        RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "dark"));

                Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntry =
                        world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT).getEntry(darkEffectKey);

                if (darkEffectEntry.isPresent()) {
                    // 检查目标实体是否已经拥有该效果
                    if (!livingTarget.hasStatusEffect(darkEffectEntry.get())) {
                        final int DURATION_TICKS = 10; // 0.5 秒
                        StatusEffectInstance darkEntanglement = new StatusEffectInstance(
                                darkEffectEntry.get(),
                                DURATION_TICKS,
                                0, // Amplifier 0 = Level 1
                                false,
                                false,
                                false
                        );
                        livingTarget.addStatusEffect(darkEntanglement);
                        LOGGER.info("Applied Dark Entanglement ({} ticks) to {} due to anchored left click.",
                                DURATION_TICKS, livingTarget.getName().getString());
                    } else {
                        LOGGER.info("{} already has Dark Entanglement. Skipping reapplication on left click.",
                                livingTarget.getName().getString());
                    }
                }
            } else {
                LOGGER.info("Left click target is not a LivingEntity or is null. Target: {}", target);
            }

            // 2. 发射单道剑气 (FlatDartProjectile)
            Hand hand = player.getMainHandStack() == stack ? Hand.MAIN_HAND : Hand.OFF_HAND;
            launchSingleFlatProjectile(world, player, stack, hand);

            // 3. 解除锚定逻辑
            disengageAnchorAndSync(stack, player, null);
            LOGGER.info("Disengaging anchor due to left click (No health restoration).");

            // 4. 应用冷却 - 注意这里是 this 指向 DarkSwordItem 实例
            player.getItemCooldownManager().set(this, 10); // 0.5 秒冷却

            // 5. 播放左键锚定攻击音效 - 修正参数数量
            world.playSound(
                    null,                       // PlayerEntity - 为null表示所有玩家都能听到
                    player.getX(),               // double x
                    player.getY(),               // double y
                    player.getZ(),               // double z
                    SoundEvents.ENTITY_WITHER_SHOOT, // SoundEvent sound
                    SoundCategory.PLAYERS,       // SoundCategory category
                    0.8F,                        // float volume
                    0.8F + world.random.nextFloat() * 0.4F // float pitch
            );

        } else {
            // === 非锚定状态下的左键逻辑 ===
            if (target instanceof LivingEntity livingTarget) {
                // 可以有概率施加短暂的效果
                if (player.getWorld().random.nextFloat() < 0.1f) { // 10%几率
                    RegistryKey<StatusEffect> darkEffectKey =
                            RegistryKey.of(RegistryKeys.STATUS_EFFECT, Identifier.of(Psychosis.MOD_ID, "dark"));

                    Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntry =
                            world.getRegistryManager().get(RegistryKeys.STATUS_EFFECT).getEntry(darkEffectKey);

                    if (darkEffectEntry.isPresent()) {
                        livingTarget.addStatusEffect(new StatusEffectInstance(
                                darkEffectEntry.get(),
                                5, // 短暂效果
                                0,
                                false,
                                false,
                                true
                        ));
                        LOGGER.info("Applied brief Dark Entanglement to {} from unanchored hit (10% chance).",
                                livingTarget.getName().getString());
                    }
                }
            }

            // 非锚定状态下添加粒子效果
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0
                );
            }
        }
    }

    // =========================================================================================
    // 物品提示信息 (appendTooltip) (保持不变)
    // =========================================================================================
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {

        // 只有当剑处于锚定状态时才添加信息
        NbtCompound anchorTag = getAnchorTag(stack);

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
    // 物品背包检查 (inventoryTick) (保持不变)
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
    // 辅助检查方法 (保持不变)
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

    // =========================================================================================
    // 辅助方法：获取黑暗纠缠等级 (用于圆月斩伤害计算)
    // =========================================================================================

    /**
     * 获取目标实体身上的“黑暗纠缠”状态效果的等级 (放大器 + 1)。
     * 在 teleportPlayer 中用于获取玩家自身的黑暗纠缠等级。
     */
    private int getDarkEntanglementAmplifier(LivingEntity target) {
        // 获取注册表条目
        Optional<RegistryEntry.Reference<StatusEffect>> darkEffectEntry =
                target.getWorld().getRegistryManager().get(RegistryKeys.STATUS_EFFECT).getEntry(DARK_EFFECT_ID);

        if (darkEffectEntry.isPresent()) {
            StatusEffectInstance instance = target.getStatusEffect(darkEffectEntry.get());
            if (instance != null) {
                // 等级 = 放大器 + 1 (Level 1 是 Amplifier 0)
                return instance.getAmplifier() + 1;
            }
        }
        return 0;
    }
}