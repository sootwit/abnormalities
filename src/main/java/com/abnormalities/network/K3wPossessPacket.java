package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class K3wPossessPacket {
    private final int phase;
    private final int k3wEntityId;

    public K3wPossessPacket(int phase, int k3wEntityId) {
        this.phase = phase;
        this.k3wEntityId = k3wEntityId;
    }

    public static void encode(K3wPossessPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase);
        buf.writeInt(msg.k3wEntityId);
    }

    public static K3wPossessPacket decode(FriendlyByteBuf buf) {
        return new K3wPossessPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(K3wPossessPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.abnormalities.client.K3wPossessClient.handle(msg.phase, msg.k3wEntityId);
        });
        ctx.get().setPacketHandled(true);
    }
}