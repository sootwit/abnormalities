package com.abnormalities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FakeAchievementPacket {
    private final String name;

    public FakeAchievementPacket(String name) {
        this.name = name;
    }

    public static void encode(FakeAchievementPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
    }

    public static FakeAchievementPacket decode(FriendlyByteBuf buf) {
        return new FakeAchievementPacket(buf.readUtf());
    }

    public static void handle(FakeAchievementPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.abnormalities.client.FakeAchievementClient.show(msg.name));
        ctx.get().setPacketHandled(true);
    }
}