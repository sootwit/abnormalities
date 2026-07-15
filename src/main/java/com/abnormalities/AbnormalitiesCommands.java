package com.abnormalities;

import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.entity.XyzEntity;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModEvents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbnormalitiesCommands {
    private static final List<String> EVENTS = List.of("nurSpawns", "k3wSpawns", "xyzSpawns");
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abnorm_forcerandomevent")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    if (!(src.getEntity() instanceof ServerPlayer player)) {
                        src.sendFailure(Component.literal("must be a player"));
                        return 0;
                    }
                    String eventName = EVENTS.get(RNG.nextInt(EVENTS.size()));
                    fireEvent(player, eventName);
                    src.sendSuccess(() -> Component.literal("triggered event: " + eventName).withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }));

        event.getDispatcher().register(Commands.literal("abnorm_callevent")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (!(src.getEntity() instanceof ServerPlayer player)) {
                                src.sendFailure(Component.literal("must be a player"));
                                return 0;
                            }
                            String eventName = StringArgumentType.getString(ctx, "name");
                            if (!EVENTS.contains(eventName)) {
                                src.sendFailure(Component.literal("unknown event. valid: " + String.join(", ", EVENTS)));
                                return 0;
                            }
                            fireEvent(player, eventName);
                            src.sendSuccess(() -> Component.literal("triggered event: " + eventName).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static void fireEvent(ServerPlayer player, String eventName) {
        switch (eventName) {
            case "nurSpawns" -> ModEvents.forceNurSpawn(player);
            case "k3wSpawns" -> K3wActionTracker.forceK3wSpawn(player);
            case "xyzSpawns" -> forceXyzSpawn(player);
        }
    }

    public static void forceXyzSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("abnormalities", "xyz_items"));
        var items = new ArrayList<net.minecraft.world.item.Item>();
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            items.add(holder.value());
        }
        if (items.isEmpty()) {
            player.sendSystemMessage(Component.literal("no items in xyz_items tag!").withStyle(ChatFormatting.RED));
            return;
        }
        net.minecraft.world.item.Item chosenItem = items.get(level.random.nextInt(items.size()));
        int amount = level.random.nextBoolean() ? 1 : (2 + level.random.nextInt(15));

        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 25.0D + level.random.nextDouble() * 20.0D;
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);

        XyzEntity xyz = ModEntities.XYZ.get().create(level);
        if (xyz != null) {
            xyz.moveTo(sx + 0.5, sy, sz + 0.5, 0, 0);
            xyz.setTargetPlayer(player);
            level.addFreshEntity(xyz);
            int seconds = 60 + level.random.nextInt(181);
            xyz.startRequest(amount, chosenItem, seconds);

            String itemName = new net.minecraft.world.item.ItemStack(chosenItem).getHoverName().getString();
            String msg;
            if (amount == 1) {
                String prefix = "aeiou".indexOf(Character.toLowerCase(itemName.charAt(0))) >= 0 ? "an" : "a";
                msg = player.getName().getString() + ", bring me " + prefix + " " + itemName + " in " + seconds + "s";
            } else {
                msg = player.getName().getString() + ", bring me " + amount + " " + itemName + "s in " + seconds + "s";
            }
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                    Component.literal(msg).withStyle(ChatFormatting.LIGHT_PURPLE), false));
            xyz.setMessageSent(true);
        }
    }
}
