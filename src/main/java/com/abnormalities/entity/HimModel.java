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

public class HimModel extends EntityModel<HimEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("abnormalities", "him"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart legL;
    private final ModelPart legR;

    public HimModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.armL = this.body.getChild("armL");
        this.armR = this.body.getChild("armR");
        this.legL = root.getChild("legL");
        this.legR = root.getChild("legR");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        body.addOrReplaceChild("armL",
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(6.0F, -12.0F, 0.0F));

        body.addOrReplaceChild("armR",
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-6.0F, -12.0F, 0.0F));

        root.addOrReplaceChild("legL",
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("legR",
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HimEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.body.y = 12.0F;
        this.body.xRot = 0.0F;
        this.body.zRot = 0.0F;
        this.head.xRot = 0.0F;
        this.head.yRot = 0.0F;
        this.armL.xRot = 0.0F;
        this.armR.xRot = 0.0F;
        this.legL.xRot = 0.0F;
        this.legR.xRot = 0.0F;

        float coll = entity.collapseProgress();
        if (coll > 0.0F) {
            float t = Math.min(1.0F, coll);
            this.body.xRot = t * -1.35F;
            this.body.y = 12.0F - t * 2.0F;
            this.head.xRot = t * 0.7F;
            this.armL.xRot = t * -1.6F;
            this.armR.xRot = t * -1.6F;
            this.armL.zRot = t * 0.5F;
            this.armR.zRot = t * -0.5F;
            this.legL.xRot = t * 1.2F;
            this.legR.xRot = t * 1.2F;
            return;
        }

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        this.armL.xRot = (float) Math.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.armR.xRot = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legL.xRot = (float) Math.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        this.legR.xRot = (float) Math.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        legL.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        legR.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}