package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DistantFlashbackPacket {
    private final int stage;

    public DistantFlashbackPacket(int stage) {
        this.stage = stage;
    }

    public static void encode(DistantFlashbackPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.stage);
    }

    public static DistantFlashbackPacket decode(FriendlyByteBuf buf) {
        return new DistantFlashbackPacket(buf.readInt());
    }

    public static void handle(DistantFlashbackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                com.abnormalities.client.DistantFlashbackClient.handleStage(msg.stage);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}