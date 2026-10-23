package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NoIdeaPacket {
    private final int phase;
    private final int durationTicks;

    public NoIdeaPacket(int phase, int durationTicks) {
        this.phase = phase;
        this.durationTicks = durationTicks;
    }

    public static void encode(NoIdeaPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.phase);
        buf.writeInt(msg.durationTicks);
    }

    public static NoIdeaPacket decode(FriendlyByteBuf buf) {
        int phase = buf.readByte();
        if (phase < 0 || phase > 1) phase = 0;
        int duration = buf.readInt();
        if (duration < 1 || duration > 12000) duration = 200;
        return new NoIdeaPacket(phase, duration);
    }

    public static void handle(NoIdeaPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                if (msg.phase == 0) {
                    com.abnormalities.client.NoIdeaClient.start(msg.durationTicks);
                } else {
                    com.abnormalities.client.NoIdeaClient.end();
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
