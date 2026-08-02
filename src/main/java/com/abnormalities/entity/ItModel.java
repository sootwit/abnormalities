package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class ItModel extends EntityModel<ItEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("abnormalities", "it"), "main");
    private static final float FPS = 12.0F;
    private static final float FRAME = 1.0F / (FPS / 20.0F);

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart legL;
    private final ModelPart legR;

    public ItModel(ModelPart root) {
        this.torso = root.getChild("torso");
        this.head = root.getChild("head");
        this.armL = root.getChild("armL");
        this.armR = root.getChild("armR");
        this.legL = root.getChild("legL");
        this.legR = root.getChild("legR");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -34.0F, 0.0F, 16.0F, 34.0F, 0.0F),
                PartPose.offset(0.0F, -10.0F, 0.0F));

        root.addOrReplaceChild("legL",
                CubeListBuilder.create().texOffs(16, 34)
                        .addBox(0.0F, -34.0F, 0.0F, 8.0F, 34.0F, 0.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        root.addOrReplaceChild("legR",
                CubeListBuilder.create().texOffs(0, 34)
                        .addBox(-8.0F, -34.0F, -1.0F, 8.0F, 34.0F, 0.0F),
                PartPose.offset(0.0F, 24.0F, 1.0F));

        root.addOrReplaceChild("armL",
                CubeListBuilder.create().texOffs(32, 34)
                        .addBox(0.0F, -34.0F, 0.0F, 8.0F, 47.0F, 0.0F),
                PartPose.offset(8.0F, -10.0F, 0.0F));

        root.addOrReplaceChild("armR",
                CubeListBuilder.create().texOffs(48, 34)
                        .addBox(-8.0F, -34.0F, -1.0F, 8.0F, 54.0F, 0.0F),
                PartPose.offset(-8.0F, -10.0F, 1.0F));

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-8.0F, -34.0F, 0.0F, 16.0F, 34.0F, 0.0F),
                PartPose.offset(0.0F, -44.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ItEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float swing = limbSwing;
        float walk = 1.0F;
        if (entity.isFrozen()) {
            swing = 0.75F;
            walk = 0.0F;
        } else {
            swing = snapFrame(limbSwing);
            walk = limbSwingAmount;
        }
        float t = swing * 0.6662F;

        float legA = (float) Math.cos(t) * 1.2F * (0.25F + walk * 0.9F);
        float legB = (float) Math.cos(t + (float) Math.PI) * 1.2F * (0.25F + walk * 0.9F);

        legL.xRot = legA;
        legR.xRot = legB;

        armL.xRot = (float) Math.cos(t + (float) Math.PI) * 1.0F * (0.2F + walk * 0.8F);
        armR.xRot = (float) Math.cos(t) * 1.0F * (0.2F + walk * 0.8F);

        float bob = walk > 0.01F ? 0.6F * wrapTest(snapFrame(limbSwing) * 2.0F) : 0.0F;
        torso.y = -10.0F + bob;
        head.y = -44.0F + bob;
        armL.y = -10.0F + bob;
        armR.y = -10.0F + bob;
        legL.y = 24.0F + bob;
        legR.y = 24.0F + bob;
    }

    private float levelSwing(float amount) {
        return Math.min(1.0F, amount * 3.0F);
    }

    private float snapFrame(float x) {
        return Math.round(x * FRAME) / FRAME;
    }

    private float wrapTest(float v) {
        return (float) Math.sin(v * (float) Math.PI);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        torso.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        armL.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        armR.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        legL.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        legR.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}