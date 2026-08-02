package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class ItModel extends EntityModel<ItEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("abnormalities", "it"), "main");
    private static final float FPS = 12.0F;
    private static final float FRAME = 1.0F / (FPS / 20.0F);

    private final ModelPart torso;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart head;

    public ItModel(ModelPart root) {
        this.torso = root.getChild("torso");
        this.legL = root.getChild("legL");
        this.legR = root.getChild("legR");
        this.armL = root.getChild("armL");
        this.armR = root.getChild("armR");
        this.head = root.getChild("head");
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
        if (entity.isFrozen()) {
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.armL.xRot = 0.0F;
            this.armR.xRot = 0.0F;
            this.legL.xRot = 0.0F;
            this.legR.xRot = 0.0F;
            return;
        }
        float snapped = snapFrame(limbSwing * 2.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);

        this.armL.xRot = (float) Math.cos(snapped * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.armR.xRot = (float) Math.cos(snapped * 0.6662F) * 1.4F * limbSwingAmount;
        this.legL.xRot = (float) Math.cos(snapped * 0.6662F) * 1.2F * limbSwingAmount;
        this.legR.xRot = (float) Math.cos(snapped * 0.6662F + (float) Math.PI) * 1.2F * limbSwingAmount;
    }

    private float snapFrame(float x) {
        return Math.round(x * FRAME) / FRAME;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        torso.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        legL.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        legR.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        armL.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        armR.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
