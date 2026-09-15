package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FriendPossessPacket {
    private final int phase;
    private final int friendEntityId;

    public FriendPossessPacket(int phase, int friendEntityId) {
        this.phase = phase;
        this.friendEntityId = friendEntityId;
    }

    public static void encode(FriendPossessPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase);
        buf.writeInt(msg.friendEntityId);
    }

    public static FriendPossessPacket decode(FriendlyByteBuf buf) {
        return new FriendPossessPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(FriendPossessPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.abnormalities.client.FriendPossessClient.handle(msg.phase, msg.friendEntityId);
        });
        ctx.get().setPacketHandled(true);
    }
}