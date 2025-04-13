package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;

// Made with Blockbench 4.12.2
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class PlayerModel<T extends PlayerEntity> extends SinglePartEntityModel<T> {
	private final ModelPart head;
	private final ModelPart player;
	public PlayerModel(ModelPart root) {
		this.head = root.getChild("head");
		this.player = root.getChild("player");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData head = modelPartData.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData player = modelPartData.addChild("player", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -9.0F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));
		return TexturedModelData.of(modelData, 16, 16);
	}

	@Override
	public ModelPart getPart() {
		return this.player;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		player.render(matrices, vertices, light, overlay, color);
	}
}