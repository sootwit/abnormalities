package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WarningAckPacket {
    private final boolean dontShowAgain;

    public WarningAckPacket(boolean dontShowAgain) {
        this.dontShowAgain = dontShowAgain;
    }

    public static void encode(WarningAckPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.dontShowAgain);
    }

    public static WarningAckPacket decode(FriendlyByteBuf buf) {
        return new WarningAckPacket(buf.readBoolean());
    }

    public static void handle(WarningAckPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (msg.dontShowAgain) {
                player.getPersistentData().putBoolean("abnormalities:seen_warning", true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
