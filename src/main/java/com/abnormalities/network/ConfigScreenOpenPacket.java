package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigScreenOpenPacket {
    public static void encode(ConfigScreenOpenPacket msg, FriendlyByteBuf buf) {}
    public static ConfigScreenOpenPacket decode(FriendlyByteBuf buf) { return new ConfigScreenOpenPacket(); }
    public static void handle(ConfigScreenOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.abnormalities.client.ConfigScreen());
            });
        }
    }
}
