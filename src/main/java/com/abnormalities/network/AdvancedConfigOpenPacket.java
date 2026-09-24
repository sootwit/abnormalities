package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdvancedConfigOpenPacket {
    public static void encode(AdvancedConfigOpenPacket msg, FriendlyByteBuf buf) {}
    public static AdvancedConfigOpenPacket decode(FriendlyByteBuf buf) { return new AdvancedConfigOpenPacket(); }
    public static void handle(AdvancedConfigOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.abnormalities.client.AdvancedConfigScreen());
            });
        }
    }
}
