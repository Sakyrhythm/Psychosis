package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.DarkGodEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import net.minecraft.world.World;

public class DarkGodRenderer extends EntityRenderer<DarkGodEntity> {

    // --- 粒子类型常量 ---
    private static final Vector3f GATHER_COLOR = new Vector3f(0.8f, 0.4f, 0.2f);
    private static final float GATHER_SIZE = 0.5f;
    private static final ParticleEffect GATHER_PARTICLE_TYPE = new DustParticleEffect(GATHER_COLOR, GATHER_SIZE);

    private static final ParticleEffect BOSS_PARTICLE_TYPE = ParticleTypes.SOUL;
    private static final ParticleEffect BOSS_PARTICLE_TYPE2 = ParticleTypes.ASH;

    // *** 技能/粒子形态 (深紫) ***
    private static final Vector3f SKILL_COLOR = new Vector3f(0.4f, 0.0f, 0.6f);
    private static final float SKILL_SIZE = 1.0f;
    private static final ParticleEffect SKILL_PARTICLE_TYPE = new DustParticleEffect(SKILL_COLOR, SKILL_SIZE);

    // --- 技能常量 (与 DarkGodEntity 保持一致) ---
    private static final int PARTICLIZING_DURATION = 10;
    private static final int SWIPE_DURATION = 20;

    // 简化的 Boss 身体部位的相对 Y 坐标
    private static final double HEAD_Y = 2.7;
    private static final double BODY_Y = 1.5;
    private static final double ARM_CENTER_Y = 2.0;
    private static final double LEG_Y = 0.5;
    private static final double X_OFFSET = 0.4;
    private static final double BOSS_CENTER_Y = 1.5;

    public DarkGodRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(DarkGodEntity entity) {
        return null;
    }

    @Override
    public void render(DarkGodEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        if (!entity.getWorld().isClient()) {
            return;
        }

        if (entity.isAppearing()) {
            runAppearanceAnimation(entity, tickDelta);
            return;
        }

        DarkGodEntity.AttackState state = entity.getAttackState();

        if (state == DarkGodEntity.AttackState.PARTICLIZING) {
            runParticlizingParticles(entity, tickDelta, true);

        } else if (state == DarkGodEntity.AttackState.WINDUP) {
            runParticlizedParticles(entity);
            runWindupParticles(entity);

        } else if (state == DarkGodEntity.AttackState.DASHING) {
            runDashParticles(entity, tickDelta);
            runParticlizedParticles(entity);

        } else if (state == DarkGodEntity.AttackState.SWIPE) {
            // 圆月斩：使用自定义粒子渲染 360 度弧线
            runSwipeParticles(entity, tickDelta);
            runNormalAura(entity);

        } else if (entity.isParticlized() || state == DarkGodEntity.AttackState.RECOVERY) {
            if (entity.isParticlized()) {
                runParticlizedParticles(entity);
            } else {
                runNormalAura(entity);
            }
        } else {
            runNormalAura(entity);
        }
    }

    // --- 粒子效果方法 ---

    /**
     * 运行粒子聚集动画 (出场)
     */
    private void runAppearanceAnimation(DarkGodEntity entity, float tickDelta) {
        World world = entity.getWorld();
        float progress = entity.getAppearanceProgress(tickDelta);
        double startRadius = 7.5;
        double endRadius = 1.0;
        double currentRadius = startRadius + (endRadius - startRadius) * (progress * progress);
        if (world.random.nextInt(3) != 0) { return; }

        double bossX = entity.getX();
        double bossY = entity.getY() + BOSS_CENTER_Y;
        double bossZ = entity.getZ();

        int numParticles = 20;

        for (int i = 0; i < numParticles; i++) {
            double angle = world.random.nextDouble() * 2.0 * Math.PI;
            double verticalRange = 8.0;
            double verticalOffset = (world.random.nextDouble() - 0.5) * verticalRange;

            double spawnX = bossX + currentRadius * Math.cos(angle);
            double spawnY = bossY + verticalOffset;
            double spawnZ = bossZ + currentRadius * Math.sin(angle);

            double dx = bossX - spawnX;
            double dy = bossY - spawnY;
            double dz = bossZ - spawnZ;

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double speedFactor = 0.6;
            double velocityMagnitude = speedFactor * (1.0 - progress);

            double vx = dx / distance * velocityMagnitude;
            double vy = dy / distance * velocityMagnitude;
            double vz = dz / distance * velocityMagnitude;

            double jitter = 0.05;
            vx += (world.random.nextDouble() - 0.5) * jitter;
            vy += (world.random.nextDouble() - 0.5) * jitter;
            vz += (world.random.nextDouble() - 0.5) * jitter;

            world.addParticle( GATHER_PARTICLE_TYPE, spawnX, spawnY, spawnZ, vx, vy, vz );
        }
    }

    /**
     * 运行粒子化过渡动画 / 实体化过渡动画
     */
    private void runParticlizingParticles(DarkGodEntity entity, float tickDelta, boolean isParticlizing) {
        World world = entity.getWorld();
        int timer = entity.getAttackTimer();
        float progress = (PARTICLIZING_DURATION - timer + tickDelta) / (float)PARTICLIZING_DURATION;
        if (progress > 1.0f) progress = 1.0f;
        float alphaProgress = isParticlizing ? (1.0f - progress) : progress;

        double bossX = entity.getX();
        double bossY = entity.getY() + BOSS_CENTER_Y;
        double bossZ = entity.getZ();

        double maxRadius = 3.0;
        double currentRadius = maxRadius * MathHelper.sin(progress * (float)Math.PI);

        int numParticles = (int)(5 * currentRadius);

        for (int i = 0; i < numParticles; i++) {
            double randX = (world.random.nextDouble() - 0.5) * currentRadius;
            double randY = (world.random.nextDouble() - 0.5) * currentRadius;
            double randZ = (world.random.nextDouble() - 0.5) * currentRadius;

            world.addParticle(
                    SKILL_PARTICLE_TYPE,
                    bossX + randX,
                    bossY + randY,
                    bossZ + randZ,
                    randX * 0.1, randY * 0.1, randZ * 0.1
            );
        }
    }

    /**
     * 运行粒子形态待机光环 - 减少粒子数量
     */
    private void runParticlizedParticles(DarkGodEntity entity) {
        World world = entity.getWorld();

        if (world.random.nextInt(4) != 0) { return; }

        double bossX = entity.getX();
        double bossY = entity.getY() + BOSS_CENTER_Y;
        double bossZ = entity.getZ();

        spawnParticlesAroundPoint(world, bossX, bossY, bossZ, 1.0, 1, SKILL_PARTICLE_TYPE);
    }

    /**
     * 运行蓄力粒子
     */
    private void runWindupParticles(DarkGodEntity entity) {
        World world = entity.getWorld();

        if (world.random.nextInt(3) != 0) { return; }

        double bossX = entity.getX();
        double bossY = entity.getY() + BOSS_CENTER_Y;
        double bossZ = entity.getZ();

        world.addParticle(
                BOSS_PARTICLE_TYPE,
                bossX + (world.random.nextDouble() - 0.5) * 0.5,
                bossY + (world.random.nextDouble() - 0.5) * 0.5,
                bossZ + (world.random.nextDouble() - 0.5) * 0.5,
                0.0, 0.1, 0.0
        );
    }

    /**
     * 运行冲刺粒子束
     */
    private void runDashParticles(DarkGodEntity entity, float tickDelta) {
        World world = entity.getWorld();

        Vec3d velocity = entity.getVelocity().normalize();
        double speed = entity.getVelocity().length();

        if (speed > 0.01 && world.random.nextInt(2) == 0) {
            double bossX = entity.getPos().x;
            double bossY = entity.getPos().y + BOSS_CENTER_Y;
            double bossZ = entity.getPos().z;

            double spawnX = bossX - velocity.x * 0.5;
            double spawnY = bossY;
            double spawnZ = bossZ - velocity.z * 0.5;

            double radius = 1.0;

            double vx = -velocity.x * 0.1;
            double vy = 0.0;
            double vz = -velocity.z * 0.1;

            world.addParticle(
                    SKILL_PARTICLE_TYPE,
                    spawnX + (world.random.nextDouble() - 0.5) * radius,
                    spawnY + (world.random.nextDouble() - 0.5) * radius * 0.5,
                    spawnZ + (world.random.nextDouble() - 0.5) * radius,
                    vx, vy, vz
            );
        }
    }


    /**
     * 运行圆月斩时的粒子：360度，5格半径横扫轨迹 (使用 SKILL_PARTICLE_TYPE)
     */
    private void runSwipeParticles(DarkGodEntity entity, float tickDelta) {
        World world = entity.getWorld();

        int timer = entity.getAttackTimer();
        int ticksElapsed = SWIPE_DURATION - timer;

        // 粒子应该在挥舞动画的核心部分（例如 5 刻到 15 刻）出现
        if (ticksElapsed >= 5 && ticksElapsed <= 15) {

            // 扫过进度：从 0.0 到 1.0
            double sweepProgress = (double)(ticksElapsed - 5) / (15 - 5);

            double bossX = entity.getX();
            double bossY = entity.getY();
            double bossZ = entity.getZ();
            float entityYaw = entity.prevYaw + (entity.getYaw() - entity.prevYaw) * tickDelta;

            double heightStart = 1.0;
            double heightEnd = 3.0;

            // 保持 5 格半径
            double attackRadius = 5.0;

            // 关键：扩大角度范围至 360 度
            double startAngle = 0.0;
            double endAngle = 360.0;

            // 粒子在 [0, 360] 度内随进度移动
            double currentAngle = startAngle + (endAngle - startAngle) * sweepProgress;
            double currentAngleRad = Math.toRadians(currentAngle);

            double bossYawRad = Math.toRadians(entityYaw);
            double worldAngleRad = bossYawRad + currentAngleRad;

            // 增加采样密度：每次 tick 模拟一段弧
            int numArcSegments = 8;
            int numHeightSamples = 4;

            // 粒子速度沿着切线方向，模拟刀刃运动
            double tangentSpeed = 0.5;

            for (int i = 0; i < numArcSegments; i++) {
                // 在当前进度周围采样一个角度
                double segmentProgress = sweepProgress + (double)i / (numArcSegments * (SWIPE_DURATION - 5));
                double segmentAngle = startAngle + (endAngle - startAngle) * segmentProgress;
                double finalAngleRad = Math.toRadians(segmentAngle) + bossYawRad;

                // 计算切线方向的速度
                double speedX = Math.cos(finalAngleRad + Math.PI / 2) * tangentSpeed;
                double speedZ = Math.sin(finalAngleRad + Math.PI / 2) * tangentSpeed;

                for (int h = 0; h < numHeightSamples; h++) {
                    double height = heightStart + (heightEnd - heightStart) * ((double)h / (numHeightSamples - 1));

                    double spawnX = bossX + attackRadius * Math.sin(finalAngleRad);
                    double spawnY = bossY + height;
                    double spawnZ = bossZ + attackRadius * Math.cos(finalAngleRad);

                    world.addParticle(
                            SKILL_PARTICLE_TYPE,
                            spawnX,
                            spawnY,
                            spawnZ,
                            speedX * 0.2,
                            0.0,
                            speedZ * 0.2
                    );
                }
            }
        }
    }


    /**
     * 运行正常的粒子光环 (实体形态 IDLE) - 减少粒子数量
     */
    private void runNormalAura(DarkGodEntity entity) {
        World world = entity.getWorld();
        if (world.random.nextInt(4) != 0) { return; }

        double bossX = entity.getX();
        double bossY = entity.getY();
        double bossZ = entity.getZ();

        int numParticles = 1;

        // 1. 头部 (Head)
        if (world.random.nextInt(2) == 0) {
            spawnParticlesAroundPoint(world, bossX, bossY + HEAD_Y, bossZ, 0.2, 1, BOSS_PARTICLE_TYPE);
        }

        // 2. 身体 (Body/Torso)
        if (world.random.nextInt(3) == 0) {
            spawnParticlesAroundBox(world, bossX, bossY + BODY_Y, bossZ, 0.5, 1.5, 0.4, 1, BOSS_PARTICLE_TYPE2);
        }

        // 3. 右臂 (Right Arm)
        if (world.random.nextInt(5) == 0) {
            spawnParticlesAroundBox(world, bossX - X_OFFSET, bossY + ARM_CENTER_Y, bossZ, 0.2, 0.8, 0.2, 1, BOSS_PARTICLE_TYPE);
        }

        // 4. 左臂 (Left Arm)
        if (world.random.nextInt(5) == 0) {
            spawnParticlesAroundBox(world, bossX + X_OFFSET, bossY + ARM_CENTER_Y, bossZ, 0.2, 0.8, 0.2, 1, BOSS_PARTICLE_TYPE);
        }

        // 5. 腿部 (Legs)
        if (world.random.nextInt(6) == 0) {
            spawnParticlesAroundBox(world, bossX - X_OFFSET * 0.5, bossY + LEG_Y, bossZ, 0.2, 0.8, 0.2, 1, BOSS_PARTICLE_TYPE2);
            spawnParticlesAroundBox(world, bossX + X_OFFSET * 0.5, bossY + LEG_Y, bossZ, 0.2, 0.8, 0.2, 1, BOSS_PARTICLE_TYPE2);
        }
    }

    // --- 辅助方法 ---
    private void spawnParticlesAroundPoint(World world, double centerX, double centerY, double centerZ, double radius, int count, ParticleEffect type) {
        for (int i = 0; i < count; ++i) {
            double randX = (world.random.nextDouble() - 0.5) * radius * 2;
            double randY = (world.random.nextDouble() - 0.5) * radius * 2;
            double randZ = (world.random.nextDouble() - 0.5) * radius * 2;

            world.addParticle(
                    type,
                    centerX + randX,
                    centerY + randY,
                    centerZ + randZ,
                    0.0, 0.0, 0.0
            );
        }
    }

    private void spawnParticlesAroundBox(World world, double centerX, double centerY, double centerZ, double xSize, double ySize, double zSize, int count, ParticleEffect type) {
        for (int i = 0; i < count; ++i) {
            double randX = (world.random.nextDouble() - 0.5) * xSize;
            double randY = (world.random.nextDouble() - 0.5) * ySize;
            double randZ = (world.random.nextDouble() - 0.5) * zSize;

            world.addParticle(
                    type,
                    centerX + randX,
                    centerY + randY,
                    centerZ + randZ,
                    0.0, 0.0, 0.0
            );
        }
    }
}