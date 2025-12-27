package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.animation.GoddessAnimation;
import com.sakyrhythm.psychosis.entity.custom.GoddessEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AnimationState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import org.joml.Vector3f;

// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class GoddessModel<T extends GoddessEntity> extends SinglePartEntityModel <T> {
	private final ModelPart Goddess;
	private final ModelPart head;
	private final Vector3f tempVec = new Vector3f();

	public GoddessModel(ModelPart root) {
		this.Goddess = root.getChild("Goddess");
		this.head = Goddess.getChild("head");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		// 🎯 根部件 Godeess 保持原样
		ModelPartData Goddess = modelPartData.addChild("Goddess", ModelPartBuilder.create(), ModelTransform.pivot(7.0F, 24.0F, 3.0F));

		ModelPartData left_arm = Goddess.addChild("left_arm", ModelPartBuilder.create().uv(26, 0).cuboid(-8.0F, -4.0F, 0.0F, 3.0F, 15.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

		ModelPartData bone13 = left_arm.addChild("bone13", ModelPartBuilder.create(), ModelTransform.of(-10.0F, -2.0F, 2.0F, 0.0F, 3.1416F, 0.0F));

		ModelPartData cube_r1 = bone13.addChild("cube_r1", ModelPartBuilder.create().uv(66, 75).cuboid(-1.6358F, -5.8059F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r2 = bone13.addChild("cube_r2", ModelPartBuilder.create().uv(5, 61).cuboid(-2.6358F, -5.8059F, 0.0F, 1.0F, 11.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 61).cuboid(-2.6358F, -5.8059F, -4.0F, 1.0F, 11.0F, 1.0F, new Dilation(0.0F))
				.uv(37, 52).cuboid(-1.6358F, -5.8059F, -3.0F, 1.0F, 9.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData bone14 = left_arm.addChild("bone14", ModelPartBuilder.create(), ModelTransform.of(-10.0F, 4.0F, 2.0F, 3.1416F, 0.0F, 2.8798F));

		ModelPartData cube_r3 = bone14.addChild("cube_r3", ModelPartBuilder.create().uv(86, 84).cuboid(-1.6358F, -5.8059F, 0.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
				.uv(80, 19).cuboid(-2.6358F, -5.8059F, -1.0F, 2.0F, 4.0F, 1.0F, new Dilation(0.0F))
				.uv(86, 78).cuboid(-1.6358F, -5.8059F, -2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r4 = bone14.addChild("cube_r4", ModelPartBuilder.create().uv(63, 0).cuboid(-1.6358F, -8.8059F, 0.0F, 1.0F, 10.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData sword = left_arm.addChild("sword", ModelPartBuilder.create().uv(40, 96).cuboid(-20.9705F, 22.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(45, 96).cuboid(-21.9705F, 22.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 46).cuboid(-21.9705F, 21.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(50, 96).cuboid(-22.9705F, 21.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 63).cuboid(-22.9705F, 22.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 66).cuboid(-23.9705F, 21.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 72).cuboid(-23.9705F, 20.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 75).cuboid(-24.9705F, 20.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 78).cuboid(-24.9705F, 19.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 81).cuboid(-25.9705F, 19.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(84, 96).cuboid(-25.9705F, 18.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 84).cuboid(-26.9705F, 18.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 87).cuboid(-26.9705F, 17.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(89, 96).cuboid(-27.9705F, 17.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(46, 52).cuboid(-30.9705F, 16.7056F, 0.3112F, 8.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 90).cuboid(-23.9705F, 17.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(53, 17).cuboid(-31.9705F, 15.7056F, 0.3112F, 8.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(94, 96).cuboid(-29.9705F, 17.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(78, 56).cuboid(-32.9705F, 13.7056F, 0.3112F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 84).cuboid(-33.9705F, 11.7056F, 0.3112F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(84, 25).cuboid(-34.9705F, 10.7056F, 0.3112F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(84, 28).cuboid(-35.9705F, 9.7056F, 0.3112F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(84, 31).cuboid(-35.9705F, 8.7056F, 0.3112F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(79, 34).cuboid(-33.9705F, 12.7056F, 0.3112F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(60, 14).cuboid(-32.9705F, 14.7056F, 0.3112F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 97).cuboid(-21.9705F, 23.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(5, 97).cuboid(-20.9705F, 23.7056F, 0.3112F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-5.6472F, -17.1111F, -14.5623F, 1.5708F, -1.4399F, -1.5708F));

		ModelPartData left_leg = Goddess.addChild("left_leg", ModelPartBuilder.create().uv(0, 0).cuboid(2.0F, 7.0F, 0.0F, 3.0F, 18.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-7.0F, 4.0F, -3.0F));

		ModelPartData bone9 = left_leg.addChild("bone9", ModelPartBuilder.create(), ModelTransform.pivot(7.0F, -2.0F, 1.0F));

		ModelPartData cube_r5 = bone9.addChild("cube_r5", ModelPartBuilder.create().uv(74, 27).cuboid(-0.2F, 5.1F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
				.uv(5, 74).cuboid(-0.2F, 5.1F, -1.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 74).cuboid(-0.2F, 5.1F, -2.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r6 = bone9.addChild("cube_r6", ModelPartBuilder.create().uv(5, 43).cuboid(-1.2F, 5.1F, 0.0F, 1.0F, 16.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0262F, 0.0F, 0.1309F));

		ModelPartData cube_r7 = bone9.addChild("cube_r7", ModelPartBuilder.create().uv(15, 61).cuboid(-1.2F, 5.1F, 0.0F, 1.0F, 10.0F, 1.0F, new Dilation(0.0F))
				.uv(10, 61).cuboid(-1.2F, 5.1F, 4.0F, 1.0F, 10.0F, 1.0F, new Dilation(0.0F))
				.uv(42, 71).cuboid(-0.2F, 5.1F, 2.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F))
				.uv(69, 27).cuboid(-0.2F, 5.1F, 1.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F))
				.uv(47, 71).cuboid(-0.2F, 5.1F, 3.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r8 = bone9.addChild("cube_r8", ModelPartBuilder.create().uv(0, 43).cuboid(-1.2F, 5.1F, 0.0F, 1.0F, 16.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 2.0F, -0.0262F, 0.0F, 0.1309F));

		ModelPartData cube_r9 = bone9.addChild("cube_r9", ModelPartBuilder.create().uv(10, 43).cuboid(-0.2F, 6.1F, 0.0F, 1.0F, 16.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData bone15 = left_leg.addChild("bone15", ModelPartBuilder.create().uv(25, 83).cuboid(0.0F, 13.0F, 0.0F, 3.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, 19.0F, -9.0F, 1.0908F, 0.0F, 0.0F));

		ModelPartData cube_r10 = bone15.addChild("cube_r10", ModelPartBuilder.create().uv(79, 0).cuboid(0.0F, 13.0F, 1.0F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.0F, -4.0F, 0.1745F, 0.0F, 0.0F));

		ModelPartData body = Goddess.addChild("body", ModelPartBuilder.create().uv(0, 33).cuboid(-3.0F, 1.0F, 0.0F, 8.0F, 3.0F, 3.0F, new Dilation(0.0F))
				.uv(58, 48).cuboid(-2.0F, 4.0F, 0.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(36, 46).cuboid(-2.0F, 4.0F, 1.0F, 6.0F, 3.0F, 2.0F, new Dilation(0.0F))
				.uv(0, 22).cuboid(-3.0F, -6.0F, 0.0F, 8.0F, 7.0F, 3.0F, new Dilation(0.0F))
				.uv(39, 0).cuboid(-3.0F, -5.0F, -1.0F, 8.0F, 5.0F, 1.0F, new Dilation(0.0F))
				.uv(58, 34).cuboid(-1.0F, 2.0F, 3.0F, 4.0F, 6.0F, 1.0F, new Dilation(0.0F))
				.uv(56, 55).cuboid(-2.0F, -1.0F, 3.0F, 6.0F, 3.0F, 1.0F, new Dilation(0.0F))
				.uv(66, 20).cuboid(0.0F, -7.0F, 0.0F, 2.0F, 4.0F, 2.0F, new Dilation(0.0F))
				.uv(39, 31).cuboid(2.0F, -4.0F, 3.0F, 3.0F, 3.0F, 2.0F, new Dilation(0.0F))
				.uv(39, 40).cuboid(-3.0F, -4.0F, 3.0F, 3.0F, 3.0F, 2.0F, new Dilation(0.0F))
				.uv(86, 59).cuboid(0.0F, -3.0F, 3.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-7.0F, 4.0F, -3.0F));

		ModelPartData cube_r11 = body.addChild("cube_r11", ModelPartBuilder.create().uv(73, 38).cuboid(-0.2F, 5.1F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(5.0F, -4.0F, 3.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r12 = body.addChild("cube_r12", ModelPartBuilder.create().uv(15, 73).cuboid(0.0F, 5.1F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(4.0F, -5.0F, 3.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r13 = body.addChild("cube_r13", ModelPartBuilder.create().uv(52, 73).cuboid(0.2F, 5.2F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-4.0F, -4.0F, 3.0F, 0.0F, 0.0F, -0.1309F));

		ModelPartData cube_r14 = body.addChild("cube_r14", ModelPartBuilder.create().uv(10, 73).cuboid(0.0F, 5.2F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, -5.0F, 3.0F, 0.0F, 0.0F, -0.1309F));

		ModelPartData cube_r15 = body.addChild("cube_r15", ModelPartBuilder.create().uv(25, 67).cuboid(-3.0F, 4.0F, 0.0F, 4.0F, 3.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, -2.0F, 3.0F, 0.0F, 0.0F, 0.0436F));

		ModelPartData cube_r16 = body.addChild("cube_r16", ModelPartBuilder.create().uv(68, 0).cuboid(-3.0F, 4.0F, 0.0F, 4.0F, 3.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, -2.0F, 3.0F, 0.0F, 0.0F, -0.0436F));

		ModelPartData bone8 = body.addChild("bone8", ModelPartBuilder.create().uv(71, 51).cuboid(-2.0F, 3.0F, 0.0F, 2.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, -1.0F, 4.0F, -0.0785F, 0.0F, 0.0F));

		ModelPartData cube_r17 = bone8.addChild("cube_r17", ModelPartBuilder.create().uv(12, 87).cuboid(-2.9F, 6.9F, 0.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

		ModelPartData cube_r18 = bone8.addChild("cube_r18", ModelPartBuilder.create().uv(87, 9).cuboid(-3.0F, 6.0F, 0.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

		ModelPartData cube_r19 = bone8.addChild("cube_r19", ModelPartBuilder.create().uv(45, 82).cuboid(-2.0F, 6.0F, -0.3F, 2.0F, 3.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 1.0F, -0.0873F, 0.0F, 0.0F));

		ModelPartData bone7 = body.addChild("bone7", ModelPartBuilder.create().uv(68, 5).cuboid(-2.0F, 3.0F, 0.0F, 2.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, -3.0F, 4.0F, -0.0785F, 0.0F, 0.0F));

		ModelPartData cube_r20 = bone7.addChild("cube_r20", ModelPartBuilder.create().uv(7, 87).cuboid(-2.9F, 6.9F, 0.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

		ModelPartData cube_r21 = bone7.addChild("cube_r21", ModelPartBuilder.create().uv(65, 51).cuboid(-3.0F, 6.0F, 0.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

		ModelPartData right_leg = Goddess.addChild("right_leg", ModelPartBuilder.create().uv(13, 0).cuboid(-3.0F, 7.0F, 0.0F, 3.0F, 18.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-7.0F, 4.0F, -3.0F));

		ModelPartData bone10 = right_leg.addChild("bone10", ModelPartBuilder.create(), ModelTransform.of(-1.0F, -2.0F, 2.0F, 0.0F, 3.1416F, 0.0F));

		ModelPartData cube_r22 = bone10.addChild("cube_r22", ModelPartBuilder.create().uv(76, 59).cuboid(-0.2F, 5.1F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
				.uv(57, 76).cuboid(-0.2F, 5.1F, -1.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
				.uv(71, 75).cuboid(-0.2F, 5.1F, -2.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r23 = bone10.addChild("cube_r23", ModelPartBuilder.create().uv(25, 49).cuboid(-1.2F, 5.1F, 0.0F, 1.0F, 16.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, 0.0F, -2.0F, 0.0262F, 0.0F, 0.1309F));

		ModelPartData cube_r24 = bone10.addChild("cube_r24", ModelPartBuilder.create().uv(37, 65).cuboid(-1.2F, 5.1F, 0.0F, 1.0F, 10.0F, 1.0F, new Dilation(0.0F))
				.uv(61, 64).cuboid(-1.2F, 5.1F, 4.0F, 1.0F, 10.0F, 1.0F, new Dilation(0.0F))
				.uv(30, 72).cuboid(-0.2F, 5.1F, 2.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F))
				.uv(71, 64).cuboid(-0.2F, 5.1F, 1.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F))
				.uv(25, 72).cuboid(-0.2F, 5.1F, 3.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r25 = bone10.addChild("cube_r25", ModelPartBuilder.create().uv(20, 49).cuboid(-1.2F, 5.1F, 0.0F, 1.0F, 16.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, 0.0F, 2.0F, -0.0262F, 0.0F, 0.1309F));

		ModelPartData cube_r26 = bone10.addChild("cube_r26", ModelPartBuilder.create().uv(15, 43).cuboid(-0.2F, 6.1F, 0.0F, 1.0F, 16.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData bone16 = right_leg.addChild("bone16", ModelPartBuilder.create().uv(10, 83).cuboid(0.0F, 13.0F, 0.0F, 3.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, 19.0F, -9.0F, 1.0908F, 0.0F, 0.0F));

		ModelPartData cube_r27 = bone16.addChild("cube_r27", ModelPartBuilder.create().uv(78, 51).cuboid(0.0F, 13.0F, 1.0F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.0F, -4.0F, 0.1745F, 0.0F, 0.0F));

		ModelPartData head = Goddess.addChild("head", ModelPartBuilder.create().uv(61, 60).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(39, 7).cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 1.0F, 5.0F, new Dilation(0.0F))
				.uv(37, 93).cuboid(-1.0F, -3.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(27, 87).cuboid(-3.0F, -3.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(17, 90).cuboid(-3.0F, -3.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(87, 69).cuboid(-4.0F, -5.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(88, 43).cuboid(-3.0F, -5.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(81, 88).cuboid(-3.0F, -4.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(89, 54).cuboid(-3.0F, -6.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(59, 89).cuboid(-3.0F, -7.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(64, 89).cuboid(-3.0F, -8.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(69, 89).cuboid(-3.0F, -9.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 90).cuboid(-2.0F, -10.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(52, 83).cuboid(0.0F, -1.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(22, 90).cuboid(-2.0F, -3.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(36, 19).cuboid(-2.0F, -1.0F, -1.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.0F))
				.uv(30, 49).cuboid(-1.0F, -1.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(87, 16).cuboid(-2.0F, -2.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(87, 13).cuboid(-1.0F, -2.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-6.0F, -6.0F, -3.0F));

		ModelPartData cube_r28 = head.addChild("cube_r28", ModelPartBuilder.create().uv(39, 14).cuboid(1.0F, -1.0F, -1.1F, 9.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-11.0F, -2.0F, 1.0F, 0.0F, 0.0873F, 0.0F));

		ModelPartData cube_r29 = head.addChild("cube_r29", ModelPartBuilder.create().uv(0, 40).cuboid(0.0F, -1.0F, -1.0F, 9.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(1.0F, -2.0F, 0.0F, 0.0F, -0.0873F, 0.0F));

		ModelPartData hair = head.addChild("hair", ModelPartBuilder.create().uv(58, 45).cuboid(-4.0F, 2.0F, -4.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(58, 42).cuboid(-4.0F, -4.0F, -2.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(53, 20).cuboid(-3.0F, -3.0F, -4.0F, 4.0F, 5.0F, 2.0F, new Dilation(0.0F))
				.uv(80, 5).cuboid(1.0F, -2.0F, -4.0F, 1.0F, 4.0F, 2.0F, new Dilation(0.0F))
				.uv(80, 12).cuboid(-4.0F, -2.0F, -4.0F, 1.0F, 4.0F, 2.0F, new Dilation(0.0F))
				.uv(53, 28).cuboid(-4.0F, -3.0F, -2.0F, 6.0F, 4.0F, 1.0F, new Dilation(0.0F))
				.uv(81, 59).cuboid(2.0F, -1.0F, -3.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
				.uv(76, 72).cuboid(-5.0F, 0.0F, -4.0F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
				.uv(35, 77).cuboid(2.0F, 0.0F, -4.0F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
				.uv(81, 72).cuboid(-5.0F, -1.0F, -3.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
				.uv(47, 93).cuboid(-4.0F, -4.0F, -1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(52, 93).cuboid(1.0F, -4.0F, -1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(1.0F, 3.0F, 0.0F));

		ModelPartData cube_r30 = hair.addChild("cube_r30", ModelPartBuilder.create().uv(30, 52).cuboid(-4.0F, 1.0F, 1.0F, 2.0F, 13.0F, 1.0F, new Dilation(0.0F))
				.uv(56, 60).cuboid(-1.0F, 2.0F, 1.0F, 1.0F, 11.0F, 1.0F, new Dilation(0.0F))
				.uv(58, 0).cuboid(1.0F, 2.0F, 1.0F, 1.0F, 12.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -2.0F, -6.0F, 0.0436F, 0.0F, 0.0F));

		ModelPartData cube_r31 = hair.addChild("cube_r31", ModelPartBuilder.create().uv(20, 67).cuboid(-1.0F, 8.0F, 1.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(1.0F, -7.0F, -6.0F, 0.0436F, 0.0F, 0.0F));

		ModelPartData cube_r32 = hair.addChild("cube_r32", ModelPartBuilder.create().uv(66, 64).cuboid(-1.0F, 8.0F, 1.0F, 1.0F, 9.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -8.0F, -6.0F, 0.0436F, 0.0F, 0.0F));

		ModelPartData bone5 = head.addChild("bone5", ModelPartBuilder.create().uv(73, 48).cuboid(-3.0F, -1.0F, 1.0F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(26, 19).cuboid(-2.0F, 0.0F, 1.0F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, 1.0F, 0.0F, 0.0F, 0.1309F, 0.0F));

		ModelPartData bone6 = head.addChild("bone6", ModelPartBuilder.create().uv(83, 37).cuboid(-2.0F, -1.0F, 1.0F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(76, 69).cuboid(-2.0F, -2.0F, 1.0F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(4.0F, 2.0F, 0.0F, 0.0F, -0.1309F, 0.0F));

		ModelPartData bone4 = head.addChild("bone4", ModelPartBuilder.create().uv(17, 87).cuboid(0.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(87, 19).cuboid(-1.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(22, 87).cuboid(1.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(96, 30).cuboid(1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(35, 96).cuboid(1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(5, 91).cuboid(1.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 31).cuboid(2.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(93, 27).cuboid(2.0F, -2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(25, 96).cuboid(2.0F, -2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 46).cuboid(2.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(93, 0).cuboid(3.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 63).cuboid(2.0F, -4.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 72).cuboid(3.0F, -4.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(25, 93).cuboid(4.0F, -10.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(10, 94).cuboid(5.0F, -9.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(94, 52).cuboid(5.0F, -8.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(79, 94).cuboid(5.0F, -7.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(52, 86).cuboid(4.0F, -6.0F, 0.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(93, 24).cuboid(4.0F, -5.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(20, 93).cuboid(3.0F, -3.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(15, 93).cuboid(3.0F, -4.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 66).cuboid(3.0F, -5.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(94, 93).cuboid(3.0F, -6.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(59, 92).cuboid(4.0F, -5.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 49).cuboid(4.0F, -10.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(69, 85).cuboid(4.0F, -9.0F, 0.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(93, 43).cuboid(4.0F, -7.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(42, 93).cuboid(4.0F, -8.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(93, 40).cuboid(4.0F, -9.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(74, 92).cuboid(5.0F, -7.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 69).cuboid(5.0F, -8.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(69, 92).cuboid(5.0F, -9.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(62, 85).cuboid(4.0F, -8.0F, 0.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(84, 47).cuboid(4.0F, -7.0F, 0.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(92, 37).cuboid(4.0F, -6.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 12).cuboid(3.0F, -7.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 15).cuboid(3.0F, -10.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(32, 92).cuboid(2.0F, -12.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(64, 92).cuboid(3.0F, -11.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 18).cuboid(3.0F, -9.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 21).cuboid(3.0F, -8.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(10, 91).cuboid(0.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(72, 95).cuboid(0.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(67, 95).cuboid(-1.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(15, 96).cuboid(-1.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 75).cuboid(2.0F, -5.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(79, 91).cuboid(1.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 78).cuboid(1.0F, -5.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 81).cuboid(1.0F, -6.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.0F, 1.0F, 0.0F, 0.0873F, 0.0F));

		ModelPartData bone = head.addChild("bone", ModelPartBuilder.create().uv(90, 34).cuboid(0.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(74, 89).cuboid(0.0F, 0.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(57, 95).cuboid(1.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(20, 96).cuboid(1.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(62, 95).cuboid(1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(39, 90).cuboid(-1.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(44, 90).cuboid(-1.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(84, 93).cuboid(-1.0F, -3.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(93, 60).cuboid(-1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(93, 57).cuboid(-1.0F, -2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 91).cuboid(0.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(49, 90).cuboid(-1.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(54, 90).cuboid(-1.0F, -4.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(92, 9).cuboid(0.0F, -5.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(86, 90).cuboid(0.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -3.0F, 2.0F, -0.0873F, 0.0F, 0.0F));

		ModelPartData bone3 = head.addChild("bone3", ModelPartBuilder.create().uv(45, 87).cuboid(0.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(87, 22).cuboid(1.0F, 0.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(87, 51).cuboid(0.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(94, 6).cuboid(-2.0F, -4.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(5, 94).cuboid(-2.0F, -3.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(94, 3).cuboid(0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(30, 95).cuboid(1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(95, 34).cuboid(1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 94).cuboid(-1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(89, 93).cuboid(-1.0F, -2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(88, 0).cuboid(-1.0F, -3.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(88, 40).cuboid(-1.0F, -4.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, -2.0F, 0.0F, 0.0F, -0.1745F, 0.0F));

		ModelPartData bone2 = head.addChild("bone2", ModelPartBuilder.create().uv(91, 84).cuboid(0.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(91, 87).cuboid(0.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(27, 90).cuboid(0.0F, 0.0F, 2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, -4.0F, 1.0F));

		ModelPartData right_arm = Goddess.addChild("right_arm", ModelPartBuilder.create().uv(23, 22).cuboid(-8.0F, -4.0F, 0.0F, 3.0F, 15.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-12.0F, 0.0F, -3.0F));

		ModelPartData bone11 = right_arm.addChild("bone11", ModelPartBuilder.create(), ModelTransform.of(-10.0F, -2.0F, 2.0F, 3.1416F, 0.0F, 3.0543F));

		ModelPartData cube_r33 = bone11.addChild("cube_r33", ModelPartBuilder.create().uv(40, 82).cuboid(-1.6358F, -5.8059F, 0.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
				.uv(81, 80).cuboid(-1.6358F, -5.8059F, -1.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
				.uv(76, 81).cuboid(-1.6358F, -5.8059F, -2.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r34 = bone11.addChild("cube_r34", ModelPartBuilder.create().uv(51, 55).cuboid(-2.6358F, -5.8022F, 0.2855F, 1.0F, 14.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0262F, 0.0F, 0.1309F));

		ModelPartData cube_r35 = bone11.addChild("cube_r35", ModelPartBuilder.create().uv(75, 15).cuboid(-2.6358F, -5.8059F, 0.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
				.uv(75, 5).cuboid(-2.6358F, -5.8059F, 4.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
				.uv(79, 25).cuboid(-1.6358F, -5.8059F, 2.0F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
				.uv(78, 37).cuboid(-1.6358F, -5.8059F, 1.0F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
				.uv(20, 78).cuboid(-1.6358F, -5.8059F, 3.0F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r36 = bone11.addChild("cube_r36", ModelPartBuilder.create().uv(46, 55).cuboid(-2.6358F, -5.8022F, -0.2855F, 1.0F, 14.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 2.0F, -0.0262F, 0.0F, 0.1309F));

		ModelPartData cube_r37 = bone11.addChild("cube_r37", ModelPartBuilder.create().uv(53, 34).cuboid(-1.6358F, -4.8059F, 0.0F, 1.0F, 14.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData bone12 = right_arm.addChild("bone12", ModelPartBuilder.create(), ModelTransform.of(-10.0F, 4.0F, 2.0F, 3.1416F, 0.0F, 2.8798F));

		ModelPartData cube_r38 = bone12.addChild("cube_r38", ModelPartBuilder.create().uv(86, 72).cuboid(-1.6358F, -5.8059F, 0.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
				.uv(86, 63).cuboid(-1.6358F, -5.8059F, -1.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
				.uv(34, 86).cuboid(-1.6358F, -5.8059F, -2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.1309F));

		ModelPartData cube_r39 = bone12.addChild("cube_r39", ModelPartBuilder.create().uv(83, 40).cuboid(-1.6358F, -5.8059F, 0.0F, 1.0F, 5.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));
		return TexturedModelData.of(modelData, 128, 128);
	}
	/**
	 * 将 Animation 对象中的关键帧变换应用到 ModelPart 实例上。
	 * 解决之前日志中显示动画已启动但未显示的问题。
	 * * @param animationState 客户端 AnimationState 实例 (用于获取时间)
	 * @param animation 静态 Animation 定义 (包含变换数据)
	 * @param animationProgress 客户端的动画进度（通常是 tick/部分 tick）
	 * @param speed 动画播放速度
	 */
	public void updateAnimation(AnimationState animationState, Animation animation, float animationProgress, float speed) {

		long runningTimeMs = animationState.getTimeRunning();

		// 2. 核心：调用 AnimationHelper 的精确方法
		// AnimationHelper.animate(model, animation, runningTime, scale, tempVec);

		AnimationHelper.animate(
				this,                 // model: SinglePartEntityModel<?>
				animation,            // animation: Animation
				runningTimeMs,        // runningTime: long (毫秒)
				speed,                // scale: float (即您传入的 speed)
				this.tempVec          // tempVec: Vector3f
		);
	}

	// ---------------------------------------------------------------------
	// 您原有的 setAngles 方法（使用上面修复的 updateAnimation）
	// ---------------------------------------------------------------------

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		this.getPart().traverse().forEach(ModelPart::resetTransform);

		GoddessEntity.AttackState state = entity.getAttackState();

		if (state == GoddessEntity.AttackState.IN || state == GoddessEntity.AttackState.COUNTER_ATTACK) {
			entity.DASH_END.startIfNotRunning(entity.age);
			entity.DASH_END.update(animationProgress, 1.0F);
			this.updateAnimation(entity.DASH_END, GoddessAnimation.DASH_END, animationProgress, 1.0F);

			entity.idleAnimation.stop();
			entity.ATTACK_ANIMATION.stop();
			return;
		}

		if (state == GoddessEntity.AttackState.SWIPE) {
			entity.ATTACK_ANIMATION.startIfNotRunning(entity.age);
			entity.ATTACK_ANIMATION.update(animationProgress, 1.0F);
			this.updateAnimation(entity.ATTACK_ANIMATION, GoddessAnimation.ATTACKANIMATION, animationProgress, 1F);

			entity.idleAnimation.stop();
			entity.DASH_END.stop();
			return;
		}

		if (state == GoddessEntity.AttackState.IDLE || state == GoddessEntity.AttackState.WINDUP || state == GoddessEntity.AttackState.RECOVERY) {
			entity.idleAnimation.startIfNotRunning(entity.age);
			entity.idleAnimation.update(animationProgress, 1.0F);
			this.updateAnimation(entity.idleAnimation, GoddessAnimation.IDLE_ANIMATION, animationProgress, 1.0F);

			entity.ATTACK_ANIMATION.stop();
			entity.DASH_END.stop();
			this.setHeadAngles(headYaw, headPitch);
			return;
		}

		if (state == GoddessEntity.AttackState.FADE_OUT || state == GoddessEntity.AttackState.OUT) {
			entity.ATTACK_ANIMATION.stop();
			entity.DASH_END.stop();
			entity.idleAnimation.stop();
			return;
		}

		entity.ATTACK_ANIMATION.stop();
		entity.DASH_END.stop();
		entity.idleAnimation.stop();
		this.setHeadAngles(headYaw, headPitch);
	}

	private void setHeadAngles(float headYaw, float headPitch) {
		headYaw = MathHelper.clamp(headYaw, -30f, 30f);
		headPitch = MathHelper.clamp(headPitch, -25f, 45f);
		this.head.yaw = headYaw * 0.017453292F;
		this.head.pitch = headPitch * 0.017453292F;
	}

	@Override
	public ModelPart getPart() {
		return this.Goddess;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		final float SCALE_FACTOR = 1.2F;
		matrices.scale(SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);
		matrices.translate(-0.8F, -2.1F, 0.0F);
		this.Goddess.yaw += MathHelper.PI;
		this.Goddess.render(matrices, vertices, light, overlay, color);
	}
}