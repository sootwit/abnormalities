package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DepthsPacket {
    private final boolean active;

    public DepthsPacket(boolean active) {
        this.active = active;
    }

    public static void encode(DepthsPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static DepthsPacket decode(FriendlyByteBuf buf) {
        return new DepthsPacket(buf.readBoolean());
    }

    public static void handle(DepthsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                com.abnormalities.client.DepthsClient.handle(msg.active);
            });
        }
        ctx.get().setPacketHandled(true);
    }
}