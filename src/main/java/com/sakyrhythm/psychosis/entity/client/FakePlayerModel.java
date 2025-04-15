package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.PlayerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * 自定义玩家实体双模型渲染器
 * 根据皮肤类型动态切换Steve/Slim两种模型进行渲染
 */
public class FakePlayerModel<T extends PlayerEntity> extends EntityModel<T> {
	// 模型实例容器
	final PlayerModelCopy<T> steve;  // 经典Steve体型模型
	final PlayerModelCopy<T> slim;   // 细手臂Slim体型模型
	boolean isSlim;  // 当前使用的模型标识

	/**
	 * 构造器初始化双模型系统
	 * @param steve 经典模型部件(宽手臂)
	 * @param slim  Slim模型部件(细手臂)
	 */
	public FakePlayerModel(ModelPart steve, ModelPart slim) {
		this.steve = new PlayerModelCopy<>(steve, false);  // 初始化经典模型
		this.slim = new PlayerModelCopy<>(slim, true);     // 初始化Slim模型
	}

	/**
	 * 更新模型角度和姿势
	 * @param entity          目标实体实例
	 * @param limbAngle       肢体摆动角度
	 * @param limbDistance    肢体摆动幅度
	 * @param animationProgress 动画进度
	 * @param headYaw        头部偏转角度
	 * @param headPitch      头部俯仰角度
	 */
	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		// 从实体获取皮肤模型类型
		isSlim = entity.clientGetters().isSlim();
		//呵,长大了!
		getModel().child=child;
		// 将参数传递到当前激活的模型
		getModel().setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
	}

	/**
	 * 执行模型渲染
	 * @param matrices  矩阵栈(用于变换操作)
	 * @param vertices  顶点缓冲
	 * @param light     光照等级
	 * @param overlay   覆盖层参数
	 * @param color     颜色参数
	 */
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		// 委托给当前激活的模型进行渲染
		getModel().render(matrices, vertices, light, overlay, color);
	}

	/**
	 * 获取当前激活的模型
	 * @return 根据isSlim标志返回对应模型实例
	 */
	public EntityModel<T> getModel() {
		return isSlim ? slim : steve;
	}
}