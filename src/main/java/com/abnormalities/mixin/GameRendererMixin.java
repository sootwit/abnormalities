package com.abnormalities.mixin;

import com.abnormalities.client.OverlayManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    Minecraft minecraft;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void abnormalities$renderOverlays(float partialTick, long finishTime, PoseStack poseStack, CallbackInfo ci) {
        if (!OverlayManager.hasOverlays()) return;
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Matrix4f prevProj = RenderSystem.getProjectionMatrix();
        try {
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0, (float)w, (float)h, 0, 1000, -1000),
                    VertexSorting.ORTHOGRAPHIC_Z);
            OverlayManager.renderAll(w, h);
        } finally {
            RenderSystem.setProjectionMatrix(prevProj, VertexSorting.ORTHOGRAPHIC_Z);
        }
    }
}
