package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.NailEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class NailModel<T extends NailEntity> extends SinglePartEntityModel<T> {
	private final ModelPart bone;
	public NailModel(ModelPart root) {
		this.bone = root.getChild("bone");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData bone = modelPartData.addChild("bone", ModelPartBuilder.create(), ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

		ModelPartData cube_r1 = bone.addChild("cube_r1", ModelPartBuilder.create().uv(28, 22).cuboid(-0.5F, -1.5F, -1.1579F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-12.7261F, -23.0665F, 1.9679F, -2.6116F, -1.0621F, -2.653F));

		ModelPartData cube_r2 = bone.addChild("cube_r2", ModelPartBuilder.create().uv(28, 17).cuboid(-0.5F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-13.8261F, -21.3165F, 0.4679F, -2.1321F, -0.189F, 3.0509F));

		ModelPartData cube_r3 = bone.addChild("cube_r3", ModelPartBuilder.create().uv(12, 28).cuboid(-0.5F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-13.4261F, -20.0665F, -1.8321F, -2.0108F, 0.3414F, 2.8481F));

		ModelPartData cube_r4 = bone.addChild("cube_r4", ModelPartBuilder.create().uv(28, 0).cuboid(-0.8F, -0.8793F, -0.0777F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-12.1734F, -18.6207F, -3.3223F, -2.1496F, 1.3685F, 2.5293F));

		ModelPartData cube_r5 = bone.addChild("cube_r5", ModelPartBuilder.create().uv(26, 27).cuboid(-0.5F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-8.9731F, -19.4269F, -3.0817F, 1.5041F, 1.1092F, 0.0079F));

		ModelPartData cube_r6 = bone.addChild("cube_r6", ModelPartBuilder.create().uv(20, 27).cuboid(-1.5F, -2.5F, -0.4142F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-6.821F, -18.0861F, -0.0858F, 1.276F, -0.1602F, -0.0687F));

		ModelPartData cube_r7 = bone.addChild("cube_r7", ModelPartBuilder.create().uv(12, 0).cuboid(-0.5F, -1.5F, -1.5F, 1.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-1.2745F, 1.0066F, -0.5F, 0.0F, 0.0F, -0.8727F));

		ModelPartData cube_r8 = bone.addChild("cube_r8", ModelPartBuilder.create().uv(12, 9).cuboid(-0.5F, -1.4F, -1.5F, 1.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(1.7255F, 0.0F, -0.5F, 0.0F, 0.0F, -0.0873F));

		ModelPartData cube_r9 = bone.addChild("cube_r9", ModelPartBuilder.create().uv(20, 0).cuboid(1.0F, -4.0F, -2.0F, 1.0F, 4.0F, 3.0F, new Dilation(0.0F))
		.uv(12, 17).cuboid(1.0F, -4.0F, -2.0F, 1.0F, 4.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.6981F));

		ModelPartData cube_r10 = bone.addChild("cube_r10", ModelPartBuilder.create().uv(20, 17).cuboid(1.0F, -3.0F, -2.0F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F))
		.uv(12, 24).cuboid(-2.0F, -3.0F, -3.0F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F))
		.uv(20, 23).cuboid(-2.0F, -3.0F, 1.0F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F))
		.uv(0, 0).cuboid(-2.0F, -26.0F, -2.0F, 3.0F, 26.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));

		ModelPartData cube_r11 = bone.addChild("cube_r11", ModelPartBuilder.create().uv(20, 12).cuboid(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, -3.0F, 1.9199F, 0.0F, -0.5236F));

		ModelPartData cube_r12 = bone.addChild("cube_r12", ModelPartBuilder.create().uv(20, 7).cuboid(-1.0F, -0.05F, -2.3F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 1.2217F, 0.0F, -0.5236F));
		return TexturedModelData.of(modelData, 64, 64);
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		bone.render(matrices, vertexConsumer,light,overlay,color);
	}

	@Override
	public ModelPart getPart() {
		return this.bone;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

	}
}