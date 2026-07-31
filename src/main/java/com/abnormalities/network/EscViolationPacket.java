package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EscViolationPacket {
    public EscViolationPacket() {}

    public static void encode(EscViolationPacket msg, FriendlyByteBuf buf) {}

    public static EscViolationPacket decode(FriendlyByteBuf buf) {
        return new EscViolationPacket();
    }

    public static void handle(EscViolationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                var sender = ctx.get().getSender();
                if (sender != null) {
                    com.abnormalities.horror.Vr9pController.escViolation(sender);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
