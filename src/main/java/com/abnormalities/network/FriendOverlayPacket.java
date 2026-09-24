package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FriendOverlayPacket {
    public FriendOverlayPacket() {}

    public static void encode(FriendOverlayPacket msg, FriendlyByteBuf buf) {}

    public static FriendOverlayPacket decode(FriendlyByteBuf buf) {
        return new FriendOverlayPacket();
    }

    public static void handle(FriendOverlayPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                com.abnormalities.client.FriendCrashOverlay.triggerApparition();
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
