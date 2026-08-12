package com.abnormalities.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WindErrorPacket {
    private final String title;
    private final String message;
    private final float eventMultiplier;

    public WindErrorPacket(String title, String message, float eventMultiplier) {
        this.title = title;
        this.message = message;
        this.eventMultiplier = eventMultiplier;
    }

    public static void encode(WindErrorPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.title, 64);
        buf.writeUtf(msg.message, 256);
        buf.writeFloat(msg.eventMultiplier);
    }

    public static WindErrorPacket decode(FriendlyByteBuf buf) {
        return new WindErrorPacket(buf.readUtf(64), buf.readUtf(256), buf.readFloat());
    }

    public static void handle(WindErrorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                com.abnormalities.client.WindErrorClient.triggerError(msg.title, msg.message, msg.eventMultiplier);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
