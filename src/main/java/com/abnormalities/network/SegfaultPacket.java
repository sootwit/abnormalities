package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SegfaultPacket {
    public static final int STATE_STOP = 0;
    public static final int STATE_CONTINUE = 1;
    public static final int STATE_END = 2;
    public static final int STATE_STARGAZED_STOP = 3;
    public static final int STATE_STARGAZED_CONTINUE = 4;

    private final int state;
    private final int duration;

    public SegfaultPacket(int state, int duration) {
        this.state = state;
        this.duration = duration;
    }

    public static void encode(SegfaultPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.state);
        buf.writeShort(msg.duration);
    }

    public static SegfaultPacket decode(FriendlyByteBuf buf) {
        return new SegfaultPacket(buf.readByte(), buf.readShort());
    }

    public static void handle(SegfaultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.abnormalities.client.SegfaultOverlay.currentState = msg.state;
            com.abnormalities.client.SegfaultOverlay.lastPacketTime = System.currentTimeMillis();
        });
        ctx.get().setPacketHandled(true);
    }
}
