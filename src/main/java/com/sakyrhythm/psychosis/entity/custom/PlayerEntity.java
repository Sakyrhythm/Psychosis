package com.sakyrhythm.psychosis.entity.custom;

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
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlayerEntity extends AnimalEntity {

    public static final TrackedData<NbtCompound> USE_SKIN = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);

    public PlayerEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(USE_SKIN, new NbtCompound());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createPlayerAttributes() {
        return AnimalEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 4.0);
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
        nbt.put("useSkin",getDataTracker().get(USE_SKIN));
        return super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        getDataTracker().set(USE_SKIN,nbt.getCompound("useSkin"));
        super.readNbt(nbt);
    }

    //Client
    /*public static boolean isClient = false;

    Supplier<ClientGetter> clientGetterSupplier = isClient ? () -> {
        throw new UnsupportedOperationException("Accessor client getter without client");
    }
    : new Supplier<>() {
        final ClientGetter getter = new ClientGetter();
        @Override
        public ClientGetter get() {
            return getter;
        }
    };*/
    @Environment(EnvType.CLIENT)
    ClientGetters getter=null;

    public ClientGetters clientGetters(){
        return getter==null?(getter=new ClientGetters()):getter;
    }

//  /summon psychosis:player ~ ~ ~ {usePlayerSkin:[I;-744927312,2119846582,-1503445732,426072093]}
//  /summon psychosis:player ~ ~ ~ {useSkin:{id:[I;-744927312,2119846582,-1503445732,426072093]}}
    @Environment(EnvType.CLIENT)
    public class ClientGetters {
        ProfileComponent profile =null;

        public boolean isSlim() {
            SkinTextures textures=getSkinTextures();
            return textures!=null && textures.model() == SkinTextures.Model.SLIM;
        }

        public Identifier getTexture() {
            SkinTextures textures=getSkinTextures();
            return textures==null ? PlayerRenderer.TEXTURE :textures.texture();
        }

        public @Nullable SkinTextures getSkinTextures() {
//            System.out.println(getDataTracker().get(USE_SKIN));
            var result=ProfileComponent.CODEC.decode(NbtOps.INSTANCE,getDataTracker().get(USE_SKIN));
            if(!result.isSuccess()) return null;
            ProfileComponent trackProfile= result.getOrThrow().getFirst();
            if (profile == null || !profile.gameProfile().equals(trackProfile.gameProfile())) {
                profile=trackProfile;
            }
            if(!profile.isCompleted()){
                profile.getFuture().thenAcceptAsync(p ->
                                ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, p).ifSuccess(profileNbt->{
                                    if(profileNbt instanceof NbtCompound compound){
                                        getDataTracker().set(USE_SKIN,compound);
                                    }
                                })
                        , SkullBlockEntity.EXECUTOR);
            }
            return MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile.gameProfile());
        }

    }

}
