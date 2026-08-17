package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WarningShowPacket {
    public static void encode(WarningShowPacket msg, FriendlyByteBuf buf) {}
    public static WarningShowPacket decode(FriendlyByteBuf buf) { return new WarningShowPacket(); }
    public static void handle(WarningShowPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            com.abnormalities.client.WarningClientHandler.queueShow();
        });
    }
}
