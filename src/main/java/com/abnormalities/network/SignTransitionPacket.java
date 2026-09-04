package com.abnormalities.network;

import com.abnormalities.sign.SignTransitionOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SignTransitionPacket {
    public static void encode(SignTransitionPacket msg, FriendlyByteBuf buf) {}
    public static SignTransitionPacket decode(FriendlyByteBuf buf) { return new SignTransitionPacket(); }
    public static void handle(SignTransitionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SignTransitionOverlay::show));
    }
}
