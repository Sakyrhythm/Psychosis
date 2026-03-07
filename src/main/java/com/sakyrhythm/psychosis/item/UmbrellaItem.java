package com.sakyrhythm.psychosis.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class UmbrellaItem extends Item {

    public UmbrellaItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof LivingEntity user) {
            boolean isHeld = user.getMainHandStack() == stack || user.getOffHandStack() == stack;

            if (isHeld) {
                user.onLanding();

                Vec3d velocity = user.getVelocity();
                if (velocity.y < -0.15) {
                    user.setVelocity(velocity.x, -0.15, velocity.z);
                    user.velocityDirty = true;
                }

                if (world.isClient && user instanceof PlayerEntity player) {
                    boolean isFalling = !player.isOnGround() && player.getVelocity().y < -0.1;
                    System.out.println("Umbrella held - OnGround: " + player.isOnGround() +
                            ", Velocity Y: " + player.getVelocity().y +
                            ", isFalling: " + isFalling);
                }
            }
        }
    }
    @Environment(EnvType.CLIENT)
    public static void registerModelPredicate() {
        System.out.println("Registering umbrella model predicate");

        ModelPredicateProviderRegistry.register(
                ModItems.UMBRELLA,
                Identifier.of("psychosis", "falling"),
                (stack, world, entity, seed) -> {
                    if (entity instanceof PlayerEntity player) {
                        boolean isFalling = !player.isOnGround() && player.getVelocity().y < -0.1;

                        return isFalling ? 1.0f : 0.0f;
                    }
                    return 0.0f;
                }
        );

        System.out.println("Umbrella model predicate registered");
    }
}