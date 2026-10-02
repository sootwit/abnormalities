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
        buf.writeInt(msg.duration);
    }

    public static HexNilShakePacket decode(FriendlyByteBuf buf) {
        float intensity = buf.readFloat();
        if (Float.isNaN(intensity) || Float.isInfinite(intensity)) intensity = 1.0f;
        int duration = buf.readInt();
        if (duration < 0 || duration > 6000) duration = Math.max(0, Math.min(6000, duration));
        return new HexNilShakePacket(intensity, duration);
    }

    public static void handle(HexNilShakePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                com.abnormalities.hexnil.ScreenShakeManager.triggerShake(player, msg.intensity, msg.duration);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
