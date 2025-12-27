package com.sakyrhythm.psychosis.entity.animation;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public class GoddessAnimation {
	public static final Animation ATTACKANIMATION = Animation.Builder.create(0.3333F).looping()
			.addBoneAnimation("body", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 20.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createRotationalVector(-2.3535F, 17.5374F, 0.4551F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createRotationalVector(-2.3535F, 17.5374F, 0.4551F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2444F, AnimationHelper.createRotationalVector(2.6465F, 17.5374F, 0.4551F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2667F, AnimationHelper.createRotationalVector(7.65F, 17.54F, 0.46F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("body", new Transformation(Transformation.Targets.TRANSLATE,
					new Keyframe(0.1778F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2444F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -1.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -1.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("left_leg", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createRotationalVector(-2.5F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createRotationalVector(-2.5F, 10.0F, 0.0F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2444F, AnimationHelper.createRotationalVector(-10.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("right_leg", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createRotationalVector(2.5F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createRotationalVector(2.5F, 10.0F, 0.0F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2444F, AnimationHelper.createRotationalVector(-5.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("head", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 10.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createRotationalVector(-2.4666F, 7.5094F, 0.2197F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createRotationalVector(-2.4666F, 7.5094F, 0.2197F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2444F, AnimationHelper.createRotationalVector(2.5334F, 7.5094F, 0.2197F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("head", new Transformation(Transformation.Targets.TRANSLATE,
					new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2444F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -2.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -2.4F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("right_arm", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.1778F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
					new Keyframe(0.2667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
			))
			.addBoneAnimation("left_arm", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(180.0F, -165.0F, -460.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1333F, AnimationHelper.createRotationalVector(180.0F, -165.0F, -460.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createRotationalVector(192.0012F, -170.9348F, -406.5451F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.199F, AnimationHelper.createRotationalVector(192.0012F, -170.9348F, -406.5451F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createRotationalVector(119.0348F, 216.991F, -159.0647F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2212F, AnimationHelper.createRotationalVector(119.0348F, 216.991F, -159.0647F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2222F, AnimationHelper.createRotationalVector(171.1166F, 258.6908F, -89.9777F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2434F, AnimationHelper.createRotationalVector(171.1166F, 258.6908F, -89.9777F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2444F, AnimationHelper.createRotationalVector(230.226F, 244.8568F, -31.779F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2657F, AnimationHelper.createRotationalVector(230.226F, 244.8568F, -31.779F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2667F, AnimationHelper.createRotationalVector(250.0824F, 225.3904F, -2.8108F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("left_arm", new Transformation(Transformation.Targets.TRANSLATE,
					new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 20.0F, -10.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1333F, AnimationHelper.createTranslationalVector(0.0F, 20.0F, -10.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createTranslationalVector(10.0F, 14.0F, -10.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.199F, AnimationHelper.createTranslationalVector(10.0F, 14.0F, -10.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createTranslationalVector(-21.08F, 2.48F, 5.99F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2434F, AnimationHelper.createTranslationalVector(-21.08F, 2.48F, 5.99F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2444F, AnimationHelper.createTranslationalVector(-22.0F, 5.0F, 5.33F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2657F, AnimationHelper.createTranslationalVector(-22.0F, 5.0F, 5.33F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2667F, AnimationHelper.createTranslationalVector(-21.0F, 4.0F, 1.33F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("hair", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.2667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3111F, AnimationHelper.createRotationalVector(1.88F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3333F, AnimationHelper.createRotationalVector(-7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("hair", new Transformation(Transformation.Targets.TRANSLATE,
					new Keyframe(0.2667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3111F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3333F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -1.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("hair", new Transformation(Transformation.Targets.SCALE,
					new Keyframe(0.2667F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3333F, AnimationHelper.createScalingVector(1.3F, 1.0F, 1.2F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("sword", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0434F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0444F, AnimationHelper.createRotationalVector(-5.2074F, 109.9802F, -0.4906F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0657F, AnimationHelper.createRotationalVector(-5.2074F, 109.9802F, -0.4906F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0667F, AnimationHelper.createRotationalVector(268.5823F, -279.1166F, -34.2407F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3333F, AnimationHelper.createRotationalVector(272.4519F, -276.8759F, -48.0975F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("sword", new Transformation(Transformation.Targets.TRANSLATE,
					new Keyframe(0.0F, AnimationHelper.createTranslationalVector(1.0F, 1.0F, -42.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0434F, AnimationHelper.createTranslationalVector(1.0F, 1.0F, -42.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0444F, AnimationHelper.createTranslationalVector(-3.0F, -3.0F, -44.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0657F, AnimationHelper.createTranslationalVector(-3.0F, -3.0F, -44.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.0667F, AnimationHelper.createTranslationalVector(-63.0F, -12.0F, 17.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.1778F, AnimationHelper.createTranslationalVector(-63.67F, -18.25F, 15.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2F, AnimationHelper.createTranslationalVector(-63.67F, -19.25F, 14.5F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2222F, AnimationHelper.createTranslationalVector(-63.73F, -20.21F, 13.75F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2444F, AnimationHelper.createTranslationalVector(-64.18F, -22.97F, 13.8F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.2667F, AnimationHelper.createTranslationalVector(-65.14F, -23.48F, 13.35F), Transformation.Interpolations.LINEAR),
					new Keyframe(0.3333F, AnimationHelper.createTranslationalVector(-64.0F, -29.0F, 13.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("sword", new Transformation(Transformation.Targets.SCALE,
					new Keyframe(0.0F, AnimationHelper.createScalingVector(2.0F, 2.0F, 2.0F), Transformation.Interpolations.LINEAR)
			))
			.build();

	public static final Animation IDLE_ANIMATION = Animation.Builder.create(1.7083F).looping()
		.addBoneAnimation("body", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("body", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_leg", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_leg", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("right_leg", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createRotationalVector(-7.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("right_leg", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("head", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.9167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.3F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("right_arm", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.4167F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_arm", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.4167F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.2917F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(1.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("hair", new Transformation(Transformation.Targets.SCALE,
			new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.5417F, AnimationHelper.createScalingVector(1.1F, 1.1F, 1.1F), Transformation.Interpolations.LINEAR),
			new Keyframe(1.0417F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR)
		))
		.addBoneAnimation("sword", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(179.4005F, 155.0181F, 179.6739F), Transformation.Interpolations.LINEAR)
		))
		.addBoneAnimation("sword", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(1.0F, -80.0F, -15.5F), Transformation.Interpolations.LINEAR)
		))
		.addBoneAnimation("sword", new Transformation(Transformation.Targets.SCALE,
			new Keyframe(0.0F, AnimationHelper.createScalingVector(2.0F, 2.0F, 2.0F), Transformation.Interpolations.LINEAR)
		))
		.build();

	public static final Animation DASH_END = Animation.Builder.create(0.7619F)
		.addBoneAnimation("body", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-25.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("body", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_leg", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-25.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_leg", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("right_leg", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-25.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createRotationalVector(-7.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("right_leg", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createTranslationalVector(0.0F, 0.2F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("head", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-25.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("head", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, -1.0F, 4.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.2857F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.6F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.6F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("right_arm", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.0238F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.5F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.5238F, AnimationHelper.createRotationalVector(-2.5F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7619F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_arm", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(-70.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.3095F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC),
			new Keyframe(0.7381F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("left_arm", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(-2.0F, 2.0F, 6.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3095F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.CUBIC)
		))
		.addBoneAnimation("sword", new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1191F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1429F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1657F, AnimationHelper.createRotationalVector(0.0F, 105.0F, 0.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1667F, AnimationHelper.createRotationalVector(9.1694F, 123.7443F, 4.7097F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1895F, AnimationHelper.createRotationalVector(9.1694F, 123.7443F, 4.7097F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1905F, AnimationHelper.createRotationalVector(17.6682F, 126.2965F, 12.566F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2133F, AnimationHelper.createRotationalVector(17.6682F, 126.2965F, 12.566F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2143F, AnimationHelper.createRotationalVector(59.6366F, 164.9116F, 58.2278F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2371F, AnimationHelper.createRotationalVector(59.6366F, 164.9116F, 58.2278F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2381F, AnimationHelper.createRotationalVector(172.0482F, 156.093F, 171.119F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3085F, AnimationHelper.createRotationalVector(172.0482F, 156.093F, 171.119F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3095F, AnimationHelper.createRotationalVector(179.4005F, 155.0181F, 179.6739F), Transformation.Interpolations.LINEAR)
		))
		.addBoneAnimation("sword", new Transformation(Transformation.Targets.TRANSLATE,
			new Keyframe(0.0F, AnimationHelper.createTranslationalVector(1.0F, 0.0F, -43.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1191F, AnimationHelper.createTranslationalVector(1.0F, 1.0F, -42.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1429F, AnimationHelper.createTranslationalVector(1.0F, 0.0F, -42.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1657F, AnimationHelper.createTranslationalVector(1.0F, 0.0F, -42.0F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(8.3F, -21.0F, -46.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1895F, AnimationHelper.createTranslationalVector(8.3F, -21.0F, -46.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.1905F, AnimationHelper.createTranslationalVector(14.7F, -23.0F, -45.7F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2133F, AnimationHelper.createTranslationalVector(14.7F, -23.0F, -45.7F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2143F, AnimationHelper.createTranslationalVector(7.1F, -65.0F, -33.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2371F, AnimationHelper.createTranslationalVector(7.1F, -65.0F, -33.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2381F, AnimationHelper.createTranslationalVector(3.1F, -79.0F, -17.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2609F, AnimationHelper.createTranslationalVector(3.1F, -79.0F, -17.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2619F, AnimationHelper.createTranslationalVector(3.1F, -79.0F, -17.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2847F, AnimationHelper.createTranslationalVector(3.1F, -79.0F, -17.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.2857F, AnimationHelper.createTranslationalVector(3.1F, -79.0F, -17.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3085F, AnimationHelper.createTranslationalVector(3.1F, -79.0F, -17.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3095F, AnimationHelper.createTranslationalVector(0.7F, -79.7F, -16.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3323F, AnimationHelper.createTranslationalVector(0.7F, -79.7F, -16.5F), Transformation.Interpolations.LINEAR),
			new Keyframe(0.3333F, AnimationHelper.createTranslationalVector(0.7F, -80.0F, -16.5F), Transformation.Interpolations.LINEAR)
		))
		.addBoneAnimation("sword", new Transformation(Transformation.Targets.SCALE,
			new Keyframe(0.0F, AnimationHelper.createScalingVector(2.0F, 2.0F, 2.0F), Transformation.Interpolations.LINEAR)
		))
		.build();
}