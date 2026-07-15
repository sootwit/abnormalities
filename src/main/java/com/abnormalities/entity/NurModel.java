package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class NurModel extends EntityModel<NurEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("abnormalities", "nur"), "main");
    private final ModelPart plane;
    public NurModel(ModelPart root) {
        this.plane = root.getChild("plane");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("plane",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.5F, -2.0F, -0.075F, 3.0F, 4.0F, 0.15F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 48, 64);
    }
    @Override
    public void setupAnim(NurEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float shake = (float) Math.sin(ageInTicks * 0.3F) * 0.02F;
        plane.xRot = shake;
        plane.zRot = shake * 0.5F;
    }
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        plane.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
