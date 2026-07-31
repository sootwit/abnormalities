package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToxicPacket {
    public static final int STATE_END = 0;
    public static final int STATE_START = 1;
    private final int state;

    public ToxicPacket(int state) {
        this.state = state;
    }

    public static void encode(ToxicPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.state);
    }

    public static ToxicPacket decode(FriendlyByteBuf buf) {
        return new ToxicPacket(buf.readByte());
    }

    public static void handle(ToxicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                com.abnormalities.client.ToxicClientHandler.setActive(msg.state == STATE_START);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
