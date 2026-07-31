package com.abnormalities.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class St1llPacket {
    private final boolean mute;

    public St1llPacket(boolean mute) {
        this.mute = mute;
    }

    public static void encode(St1llPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.mute);
    }

    public static St1llPacket decode(FriendlyByteBuf buf) {
        return new St1llPacket(buf.readBoolean());
    }

    public static void handle(St1llPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                if (msg.mute) {
                    mc.getSoundManager().stop();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
