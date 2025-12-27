package com.sakyrhythm.psychosis.entity.animation;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public class ScytheAnimation {
	public static final Animation ANIMATION = Animation.Builder.create(0.1145F).looping()
			// --- bone 的变换 ---
			.addBoneAnimation("bone", new Transformation(Transformation.Targets.TRANSLATE,
					// position: [0, -1, 0.75]
					new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, -1.0F, 0.75F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("bone", new Transformation(Transformation.Targets.SCALE,
					// scale: [1, 1.3, 1.3]
					new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0F, 1.3F, 1.3F), Transformation.Interpolations.LINEAR)
			))

			// --- bone5 的变换 ---
			.addBoneAnimation("bone5", new Transformation(Transformation.Targets.TRANSLATE,
					// position: [1.25, 0, -0.5]
					new Keyframe(0.0F, AnimationHelper.createTranslationalVector(1.25F, 0.0F, -0.5F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("bone5", new Transformation(Transformation.Targets.SCALE,
					// scale: [1.1, 1, 1.2]
					new Keyframe(0.0F, AnimationHelper.createScalingVector(1.1F, 1.0F, 1.2F), Transformation.Interpolations.LINEAR)
			))
			.build();
}