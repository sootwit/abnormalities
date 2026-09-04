package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class K3wOverlayPacket {
    public K3wOverlayPacket() {}

    public static void encode(K3wOverlayPacket msg, FriendlyByteBuf buf) {}

    public static K3wOverlayPacket decode(FriendlyByteBuf buf) {
        return new K3wOverlayPacket();
    }

    public static void handle(K3wOverlayPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.abnormalities.client.K3wCrashOverlay.triggerApparition();
        });
        ctx.get().setPacketHandled(true);
    }
}
