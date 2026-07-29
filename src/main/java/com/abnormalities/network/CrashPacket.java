package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CrashPacket {
    public static void encode(CrashPacket msg, FriendlyByteBuf buf) {}
    public static CrashPacket decode(FriendlyByteBuf buf) { return new CrashPacket(); }
    public static void handle(CrashPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> mc.stop());
    }
}
