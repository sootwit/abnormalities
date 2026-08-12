package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WindErrorDismissPacket {
    public static void encode(WindErrorDismissPacket msg, FriendlyByteBuf buf) {}
    public static WindErrorDismissPacket decode(FriendlyByteBuf buf) { return new WindErrorDismissPacket(); }
    public static void handle(WindErrorDismissPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                com.abnormalities.thewind.WindErrorHandler.onDismiss(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
