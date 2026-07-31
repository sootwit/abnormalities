package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class WrongCraftManager {
    private static final Map<UUID, Long> CURSED_UNTIL = new HashMap<>();
    private static final List<String> WHISPERS = List.of(
            "why did you make me.",
            "you should not have.",
            "i came from your hands.",
            "you built me wrong.",
            "i am yours. forever.",
            "do not put me down."
    );

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!AbnormalitiesConfig.WR0NG_ENABLED.get()) return;
        if (player.level().getServer() == null) return;
        long currentDay = player.level().getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (player.level().random.nextInt(AbnormalitiesConfig.WR0NG_CHANCE.get()) != 0) return;
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) return;
        if (result.getCount() != 1) return;
        CompoundTag tag = result.getOrCreateTag();
        tag.putBoolean("abnormalities:cursed", true);
        result.setTag(tag);
        var lore = result.getOrCreateTagElement("display").getList("Lore", 8);
        lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(WHISPERS.get(player.level().random.nextInt(WHISPERS.size()))).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC))));
        result.getOrCreateTagElement("display").put("Lore", lore);
        long until = player.level().getGameTime() + AbnormalitiesConfig.WR0NG_DURATION.get();
        CURSED_UNTIL.put(player.getUUID(), until);
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        ItemStack stack = event.getEntity().getItem();
        if (stack.getTag() != null && stack.getTag().getBoolean("abnormalities:cursed")) {
            event.setCanceled(true);
            event.getEntity().discard();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.WR0NG_ENABLED.get()) return;
        if (overworld.getGameTime() % 100 != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            Long until = CURSED_UNTIL.get(sp.getUUID());
            if (until == null) continue;
            if (overworld.getGameTime() >= until) {
                CURSED_UNTIL.remove(sp.getUUID());
                continue;
            }
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack stack = sp.getInventory().getItem(i);
                if (stack.getTag() != null && stack.getTag().getBoolean("abnormalities:cursed")) {
                    if (overworld.random.nextInt(30) == 0) {
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.WHISPER_SOUND.get()),
                            net.minecraft.sounds.SoundSource.MASTER, sp.getX(), sp.getY(), sp.getZ(), 2.0f, 0.7f, 0));
                    }
                }
            }
        }
    }

    public static void forceCurse(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("abnormalities:cursed", true);
            stack.setTag(tag);
            CURSED_UNTIL.put(player.getUUID(), player.level().getGameTime() + AbnormalitiesConfig.WR0NG_DURATION.get());
            return;
        }
    }
}
