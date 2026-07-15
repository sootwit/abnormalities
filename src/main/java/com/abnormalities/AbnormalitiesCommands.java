package com.abnormalities;

import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.registry.ModEvents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class AbnormalitiesCommands {
    private static final List<String> EVENTS = List.of("nurSpawns", "k3wSpawns");
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
        }
    }
}
