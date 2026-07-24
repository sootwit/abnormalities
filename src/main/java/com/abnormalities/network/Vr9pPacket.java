package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Vr9pPacket {
    public static final int STATE_STOP = 0;
    public static final int STATE_CONTINUE = 1;
    public static final int STATE_END = 2;

    private final int state;
    private final int duration;

    public Vr9pPacket(int state, int duration) {
        this.state = state;
        this.duration = duration;
    }

    public static void encode(Vr9pPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.state);
        buf.writeShort(msg.duration);
    }

    public static Vr9pPacket decode(FriendlyByteBuf buf) {
        return new Vr9pPacket(buf.readByte(), buf.readShort());
    }

    public static void handle(Vr9pPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.abnormalities.client.Vr9pOverlay.currentState = msg.state;
            com.abnormalities.client.Vr9pOverlay.stateStartTime = System.currentTimeMillis();
            com.abnormalities.client.Vr9pOverlay.stateDuration = msg.duration;
        });
        ctx.get().setPacketHandled(true);
    }
}
