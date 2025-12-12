package com.sakyrhythm.psychosis.entity.client;

import com.sakyrhythm.psychosis.entity.custom.DWitherEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class DegenerateWitherModel<T extends DWitherEntity> extends SinglePartEntityModel<T> {
    private static final String RIBCAGE = "ribcage";
    private static final String CENTER_HEAD = "center_head";
    private static final float RIBCAGE_PITCH_OFFSET = 0.065F;
    private static final float TAIL_PITCH_OFFSET = 0.265F;
    private final ModelPart root;
    private final ModelPart centerHead;
    private final ModelPart ribcage;
    private final ModelPart tail;

    public DegenerateWitherModel(ModelPart root) {
        this.root = root;
        this.ribcage = root.getChild("ribcage");
        this.tail = root.getChild("tail");
        this.centerHead = root.getChild("center_head");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("shoulders",
                ModelPartBuilder.create()
                        .uv(0, 16)
                        .cuboid(-7.0F, 3.9F, -0.5F, 14.0F, 3.0F, 3.0F, Dilation.NONE),
                ModelTransform.NONE);

        float ribcagePitch = 0.20420352F;

        root.addChild(RIBCAGE,
                ModelPartBuilder.create()
                        .uv(0, 22).cuboid(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F, Dilation.NONE)
                        .uv(24, 22).cuboid(-4.0F, 1.5F, 0.5F, 11.0F, 2.0F, 2.0F, Dilation.NONE)
                        .uv(24, 22).cuboid(-4.0F, 4.0F, 0.5F, 11.0F, 2.0F, 2.0F, Dilation.NONE)
                        .uv(24, 22).cuboid(-4.0F, 6.5F, 0.5F, 11.0F, 2.0F, 2.0F, Dilation.NONE),
                ModelTransform.of(-2.0F, 6.9F, -0.5F, ribcagePitch, 0.0F, 0.0F));

        root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(12, 22)
                        .cuboid(0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 3.0F, Dilation.NONE),
                ModelTransform.of(-2.0F, 6.9F + MathHelper.cos(ribcagePitch) * 10.0F, -0.5F + MathHelper.sin(ribcagePitch) * 10.0F, 0.83252203F, 0.0F, 0.0F));

        // 中心头部保持原样
        root.addChild(CENTER_HEAD,
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, Dilation.NONE),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(T witherEntity, float f, float g, float h, float i, float j) {
        float k = MathHelper.cos(h * 0.1F);
        this.ribcage.pitch = (0.065F + 0.05F * k) * (float)Math.PI;
        this.tail.setPivot(-2.0F, 6.9F + MathHelper.cos(this.ribcage.pitch) * 10.0F, -0.5F + MathHelper.sin(this.ribcage.pitch) * 10.0F);
        this.tail.pitch = (0.265F + 0.1F * k) * (float)Math.PI;
        this.centerHead.yaw = i * ((float)Math.PI / 180F);
        this.centerHead.pitch = j * ((float)Math.PI / 180F);
    }

    @Override
    public void animateModel(T witherEntity, float f, float g, float h) {
    }
}