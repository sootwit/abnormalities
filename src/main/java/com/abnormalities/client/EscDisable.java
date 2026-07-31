package com.abnormalities.client;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.network.EscViolationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class EscDisable {
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (!(event.getNewScreen() instanceof PauseScreen)) return;
        boolean stargazed = Vr9pOverlay.currentState == 3 || Vr9pOverlay.currentState == 4;
        boolean nurChasing = false;
        for (NurEntity nur : mc.level.getEntitiesOfClass(NurEntity.class, mc.player.getBoundingBox().inflate(256.0D))) {
            if (nur.isChasing()) { nurChasing = true; break; }
        }
        if (!stargazed && !nurChasing) return;
        event.setCanceled(true);
        mc.getSoundManager().resume();
        if (stargazed) {
            AbnormalitiesMod.CHANNEL.sendToServer(new EscViolationPacket());
        }
    }
}
