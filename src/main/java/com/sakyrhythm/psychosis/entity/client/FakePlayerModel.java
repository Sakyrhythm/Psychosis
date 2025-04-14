package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class FakePlayerModel<T extends PlayerEntity> extends EntityModel<T> {
	final PlayerModelCopy<T> steve;
	final PlayerModelCopy<T> slim;
	boolean isSlim;

	public FakePlayerModel(ModelPart steve,ModelPart slim) {
		this.steve=new PlayerModelCopy<>(steve,false);
		this.slim=new PlayerModelCopy<>(slim,true);
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		isSlim= entity.clientGetters().isSlim();
		getModel().setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		getModel().render(matrices, vertices, light, overlay, color);
	}
	public EntityModel<T> getModel(){
		return isSlim?slim:steve;
	}
}