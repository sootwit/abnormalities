package com.abnormalities.client;

import com.abnormalities.config.AbnormalitiesConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class FadedClient {
    private static final ResourceLocation DESAT = new ResourceLocation("abnormalities", "textures/gui/faded_overlay.png");
    private static final Random RNG = new Random();
    private static int framesLeft = 0;
    private static BlockPos target = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        if (!AbnormalitiesConfig.F4DED_ENABLED.get()) return;
        if (mc.level.getGameTime() % 120 != 0) return;
        if (RNG.nextInt(AbnormalitiesConfig.F4DED_CHANCE.get()) != 0) return;
        BlockPos center = mc.player.blockPosition();
        target = center.offset(RNG.nextInt(9) - 4, RNG.nextInt(5) - 1, RNG.nextInt(9) - 4);
        framesLeft = 3 + RNG.nextInt(4);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        if (framesLeft <= 0 || target == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        framesLeft--;
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Vec3 cam = event.getCamera().getPosition();
        pose.translate(target.getX() - cam.x, target.getY() - cam.y, target.getZ() - cam.z);
        float alpha = 0.25F + RNG.nextFloat() * 0.2F;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucentNoCrumbling());
        var m = pose.last().pose();
        var n = pose.last().normal();
        vc.vertex(m, 0, 0, 0.01F).color(200, 200, 200, (int) (alpha * 255)).uv(0, 0).uv2(15728880).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 1, 0, 0.01F).color(200, 200, 200, (int) (alpha * 255)).uv(1, 0).uv2(15728880).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 1, 1, 0.01F).color(200, 200, 200, (int) (alpha * 255)).uv(1, 1).uv2(15728880).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 0, 1, 0.01F).color(200, 200, 200, (int) (alpha * 255)).uv(0, 1).uv2(15728880).normal(n, 0, 1, 0).endVertex();
        pose.popPose();
    }
}
