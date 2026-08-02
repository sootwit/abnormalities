package com.abnormalities.client;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;

public class FakeAchievementClient {
    public static void show(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ItemStack icon = new ItemStack(Items.BLACK_CONCRETE);
        Component title = Component.literal(name);
        DisplayInfo display = new DisplayInfo(icon, title, Component.literal(""), null,
                FrameType.TASK, true, false, false);
        Advancement fake = new Advancement(
                new ResourceLocation("abnormalities", "fake_" + UUID.randomUUID()),
                null, display, AdvancementRewards.EMPTY, Map.of(), new String[0][0], false);
        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                SoundEvents.UI_TOAST_IN, 1.0F));
        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5F));
        mc.getToasts().addToast(new AdvancementToast(fake));
    }
}