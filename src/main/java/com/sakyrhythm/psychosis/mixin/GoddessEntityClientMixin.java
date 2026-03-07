package com.sakyrhythm.psychosis.mixin;

import com.sakyrhythm.psychosis.entity.custom.GoddessEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoddessEntity.class)
public abstract class GoddessEntityClientMixin extends LivingEntity {

    protected GoddessEntityClientMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // 注入到客户端的 tick() 方法尾部，确保每帧运行
    @Inject(method = "tick", at = @At("TAIL"))
    private void psychosis_tickClient(CallbackInfo ci) {
        // 确保在 ClientWorld (而不是 ServerWorld) 上运行
        if (this.getWorld().isClient()) {
            // 将 Mixin 实例转换为实际的 GoddessEntity 实例
            GoddessEntity self = (GoddessEntity)(Object)this;
            GoddessEntity.HexagramType type = self.getHexagramType();

            // 核心逻辑：如果 HexagramType 不是 NONE，持续渲染粒子
            if (type != GoddessEntity.HexagramType.NONE) {
                this.renderHexagramParticles(self, this.getWorld());
                // 可选日志，用于确认客户端渲染正在进行
                System.out.println("DEBUG (Client): Rendering particles for type: " + type.name());
            }
        }
    }

    // 独立的方法来实现粒子渲染
    private void renderHexagramParticles(GoddessEntity self, World world) {
        GoddessEntity.HexagramType type = self.getHexagramType();
        if (type == GoddessEntity.HexagramType.NONE) return;

        ParticleEffect particleType = switch (type) {
            // 简单粒子类型 (保持不变)
            case WHITE -> ParticleTypes.GLOW;
            case PURPLE -> ParticleTypes.DRAGON_BREATH;
            case BLACK -> ParticleTypes.SMOKE;

            // 复杂粒子类型：使用 org.joml.Vector3f
            case GRAY -> {
                // 创建一个灰色的 DustParticleEffect 实例：
                // 💥 关键修正 1: 使用 new org.joml.Vector3f(...)
                Vector3f grayColor = new Vector3f(0.5F, 0.5F, 0.5F); // 中性灰色 (R, G, B, 范围 0.0 到 1.0)
                float size = 1.0F;
                // 💥 关键修正 2: 构造函数现在接受 Vector3f
                yield new DustParticleEffect(grayColor, size);
            }

            // 默认粒子：使用 SimpleParticleType 确保兼容性 (保持不变)
            default -> ParticleTypes.ELECTRIC_SPARK;
        };

        double centerX = self.getSummoningAnchorPos().getX() + 0.5;
        double centerZ = self.getSummoningAnchorPos().getZ() + 0.5;
        double centerY = self.getSummoningAnchorPos().getY() + 1.5; // 提高到方块上方

        int points = 8;
        double radius = 1.2;
        for (int i = 0; i < points; i++) {
            double angle = (double) i * 2.0 * Math.PI / (double) points;
            double x_out = centerX + radius * Math.cos(angle);
            double z_out = centerZ + radius * Math.sin(angle);
            double y = centerY + 0.1;

            self.getWorld().addParticle(particleType, x_out, y, z_out, 0.0, 0.0, 0.0);

            double inner_radius = radius * 0.4;
            double angle_inner = angle + (Math.PI / points);
            double x_in = centerX + inner_radius * Math.cos(angle_inner);
            double z_in = centerZ + inner_radius * Math.sin(angle_inner);
            self.getWorld().addParticle(particleType, x_in, y, z_in, 0.0, 0.0, 0.0);
        }

    }

}