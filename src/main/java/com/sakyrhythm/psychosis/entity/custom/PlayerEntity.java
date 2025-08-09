// src/main/java/com/sakyrhythm/psychosis/entity/custom/PlayerEntity.java
package com.sakyrhythm.psychosis.entity.custom;

import com.sakyrhythm.psychosis.Psychosis;
import com.sakyrhythm.psychosis.entity.ai.goal.AllTemptGoal;
import com.sakyrhythm.psychosis.entity.client.PlayerRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlayerEntity extends AnimalEntity {

    public static final TrackedData<NbtCompound> USE_SKIN = DataTracker.registerData(
            PlayerEntity.class,
            TrackedDataHandlerRegistry.NBT_COMPOUND
    );

    private static final double MIN_DISTANCE_SQ = 5.0;
    private static final int COOLDOWN_TICKS = 50; // Delay before a new detection starts after a sequence ends
    private static final int TOTAL_DAMAGE_HITS = 10;
    private static final int DAMAGE_INTERVAL_TICKS = 2;
    private static final int MAX_DARK_EFFECT_LEVEL = 100; // Define a max level for the effect

    private @Nullable net.minecraft.entity.player.PlayerEntity currentDamageTarget = null;
    private int hitsToDeliver = 0;
    private int ticksSinceLastHit = 0;
    private int ticksUntilNextDetection = 0; // New: Cooldown timer for detection

    private RegistryEntry.Reference<DamageType> darkDamageEntry;
    private RegistryEntry<StatusEffect> darkEffectEntry; // This type is correct

    public PlayerEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        // Initialize darkDamageEntry and darkEffectEntry once world is ready
        if (darkDamageEntry == null) {
            darkDamageEntry = getWorld().getRegistryManager()
                    .get(RegistryKeys.DAMAGE_TYPE)
                    .getEntry(Psychosis.SHADOW_DAMAGE)
                    .orElse(null); // .orElse(null) works here because darkDamageEntry is RegistryEntry.Reference
            if (darkDamageEntry == null) {
                Psychosis.LOGGER.error("Failed to find SHADOW_DAMAGE entry in registry for PlayerEntity! Damage will not be applied.");
                return;
            }
        }

        // Initialize darkEffectEntry similarly
        if (darkEffectEntry == null) {
            darkEffectEntry = getWorld().getRegistryManager()
                    .get(RegistryKeys.STATUS_EFFECT)
                    .getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, net.minecraft.util.Identifier.of(Psychosis.MOD_ID, "dark")))
                    .orElse(null); // <--- ADDED .orElse(null) HERE
            if (darkEffectEntry == null) {
                Psychosis.LOGGER.error("Failed to find DarkEffect entry in registry for PlayerEntity! Status effect will not be applied.");
            }
        }


        // --- Logic Flow ---
        if (currentDamageTarget != null) {
            if (!currentDamageTarget.isAlive() || currentDamageTarget.isRemoved()) {
                currentDamageTarget = null;
                hitsToDeliver = 0;
                ticksUntilNextDetection = COOLDOWN_TICKS;
                return;
            }

            ticksSinceLastHit++;

            if (ticksSinceLastHit >= DAMAGE_INTERVAL_TICKS) {
                if (hitsToDeliver > 0) {
                    DamageSource damageSource = new DamageSource(darkDamageEntry);
                    currentDamageTarget.damage(damageSource, 1.0f);
                    // --- ADDING THE STATUS EFFECT LOGIC HERE ---
                    // Only attempt to apply the effect if darkEffectEntry was successfully found
                    if (darkEffectEntry != null) {
                        int newAmplifier = 0;
                        int effectDuration = StatusEffectInstance.INFINITE;

                        // Use darkEffectEntry here
                        // hasStatusEffect() and getStatusEffect() can take RegistryEntry.Reference as well.
                        if (currentDamageTarget.hasStatusEffect(darkEffectEntry)) {
                            StatusEffectInstance existingEffect = currentDamageTarget.getStatusEffect(darkEffectEntry);
                            if (existingEffect != null) {
                                newAmplifier = Math.min(existingEffect.getAmplifier() + 1, MAX_DARK_EFFECT_LEVEL);
                            }
                        }

                        // Use darkEffectEntry in the StatusEffectInstance constructor
                        currentDamageTarget.addStatusEffect(new StatusEffectInstance(
                                darkEffectEntry, // Pass the RegistryEntry
                                effectDuration,
                                newAmplifier,
                                false,
                                true,
                                true
                        ));
                    } else {
                        // Log a warning if the effect couldn't be applied because it wasn't registered
                        Psychosis.LOGGER.warn("DarkEffect could not be applied because its RegistryEntry was not found.");
                    }
                    // --- END STATUS EFFECT LOGIC ---

                    hitsToDeliver--;
                    ticksSinceLastHit = 0;
                } else {
                    currentDamageTarget = null;
                    ticksUntilNextDetection = COOLDOWN_TICKS;
                    this.remove(RemovalReason.DISCARDED);
                    return;
                }
            }
        }
        else {
            if (ticksUntilNextDetection > 0) {
                ticksUntilNextDetection--;
            } else {
                net.minecraft.entity.player.PlayerEntity closestPlayer = getWorld().getClosestPlayer(
                        this,
                        getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE)
                );

                if (closestPlayer != null) {
                    double distanceSq = this.squaredDistanceTo(closestPlayer);

                    if (distanceSq < MIN_DISTANCE_SQ) {
                        ((ServerWorld) getWorld()).spawnParticles(
                                ParticleTypes.CLOUD,
                                getX(), getY() + 0.5, getZ(),
                                10,
                                0.3, 0.3, 0.3,
                                0.1
                        );
                        currentDamageTarget = closestPlayer;
                        hitsToDeliver = TOTAL_DAMAGE_HITS;
                        ticksSinceLastHit = 0;
                    }
                }
            }
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(USE_SKIN, new NbtCompound());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AllTemptGoal(this,1.3));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, net.minecraft.entity.player.PlayerEntity.class, 6.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createPlayerAttributes() {
        return AnimalEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0);
    }
    public static void sendMessageToAllPlayers(Boolean able, ServerWorld world, Text message) {
        if(able){
            world.getServer().getPlayerManager().broadcast(message, false);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("useSkin", getDataTracker().get(USE_SKIN));
        return super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        getDataTracker().set(USE_SKIN, nbt.getCompound("useSkin"));
        super.readNbt(nbt);
    }

    @Environment(EnvType.CLIENT)
    private ClientGetters getter = null;

    @Environment(EnvType.CLIENT)
    public ClientGetters clientGetters() {
        return getter == null ? (getter = new ClientGetters()) : getter;
    }

    @Environment(EnvType.CLIENT)
    public class ClientGetters {
        private ProfileComponent profile = null;

        public boolean isSlim() {
            SkinTextures textures = getSkinTextures();
            return textures != null && textures.model() == SkinTextures.Model.SLIM;
        }

        public Identifier getTexture() {
            SkinTextures textures = getSkinTextures();
            return textures == null ? PlayerRenderer.TEXTURE : textures.texture();
        }

        public @Nullable SkinTextures getSkinTextures() {
            var result = ProfileComponent.CODEC.decode(NbtOps.INSTANCE, getDataTracker().get(USE_SKIN));
            if (!result.isSuccess()) return null;

            ProfileComponent trackProfile = result.getOrThrow().getFirst();

            if (profile == null || !profile.gameProfile().equals(trackProfile.gameProfile())) {
                profile = trackProfile;
            }

            if (!profile.isCompleted()) {
                profile.getFuture().thenAcceptAsync(p ->
                                ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, p).ifSuccess(profileNbt -> {
                                    if (profileNbt instanceof NbtCompound compound) {
                                        getDataTracker().set(USE_SKIN, compound);
                                    }
                                }),
                        SkullBlockEntity.EXECUTOR
                );
            }

            return MinecraftClient.getInstance().getSkinProvider()
                    .getSkinTextures(profile.gameProfile());
        }
    }
}