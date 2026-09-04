package com.abnormalities.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HexNilShakePacket {
    private final float intensity;
    private final int duration;

    public HexNilShakePacket(float intensity, int duration) {
        this.intensity = intensity;
        this.duration = duration;
    }

    public static void encode(HexNilShakePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.intensity);
        buf.writeShort(msg.duration);
    }

    public static HexNilShakePacket decode(FriendlyByteBuf buf) {
        return new HexNilShakePacket(buf.readFloat(), buf.readShort());
    }

    public static void handle(HexNilShakePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                com.abnormalities.hexnil.HexNilShakeHandler.triggerShake(player, msg.intensity, msg.duration);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
