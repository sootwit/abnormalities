package com.abnormalities;

import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.entity.XyzEntity;
import com.abnormalities.horror.HorrorEventPool;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModEvents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AbnormalitiesCommands {
    private static final List<String> BASE_EVENTS = List.of("nurSpawns", "k3wSpawns", "xyzSpawns");
    private static final Random RNG = new Random();

    private static List<String> allEvents() {
        List<String> all = new ArrayList<>(BASE_EVENTS);
        all.addAll(HorrorEventPool.getRegistered().stream().map(e -> e.getName()).collect(Collectors.toList()));
        return all;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abnorm_forcerandomevent")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    if (!(src.getEntity() instanceof ServerPlayer player)) {
                        src.sendFailure(Component.literal("must be a player"));
                        return 0;
                    }
                    List<String> events = allEvents();
                    String eventName = events.get(RNG.nextInt(events.size()));
                    fireEvent(player, eventName);
                    src.sendSuccess(() -> Component.literal("triggered event: " + eventName).withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }));

        event.getDispatcher().register(Commands.literal("abnorm_callevent")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String s : allEvents()) {
                                builder.suggest(s);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (!(src.getEntity() instanceof ServerPlayer player)) {
                                src.sendFailure(Component.literal("must be a player"));
                                return 0;
                            }
                            String eventName = StringArgumentType.getString(ctx, "name");
                            List<String> events = allEvents();
                            if (!events.contains(eventName)) {
                                src.sendFailure(Component.literal("unknown event. valid: " + String.join(", ", events)));
                                return 0;
                            }
                            fireEvent(player, eventName);
                            src.sendSuccess(() -> Component.literal("triggered event: " + eventName).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })));

        event.getDispatcher().register(Commands.literal("abnorm_rep")
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int rep = ReputationManager.getRep(target);
                                    String tier = ReputationManager.getTierLabel(rep);
                                    ctx.getSource().sendSuccess(() ->
                                        Component.literal(target.getName().getString() + " rep: " + rep + " (" + tier + ")"), false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 2500))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            ReputationManager.setRep(target, value);
                                            ctx.getSource().sendSuccess(() ->
                                                Component.literal("set " + target.getName().getString() + " rep to " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))));
    }

    private static void fireEvent(ServerPlayer player, String eventName) {
        switch (eventName) {
            case "nurSpawns" -> ModEvents.forceNurSpawn(player);
            case "k3wSpawns" -> K3wActionTracker.forceK3wSpawn(player);
            case "xyzSpawns" -> forceXyzSpawn(player);
            default -> {
                var match = HorrorEventPool.getRegistered().stream()
                    .filter(e -> e.getName().equals(eventName))
                    .findFirst();
                if (match.isPresent()) {
                    HorrorEventPool.fireEvent(player, match.get());
                }
            }
        }
    }

    public static void forceXyzSpawn(ServerPlayer player) {
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        var tag = net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("abnormalities", "xyz_items"));
        var items = new java.util.ArrayList<net.minecraft.world.item.Item>();
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            items.add(holder.value());
        }
        if (items.isEmpty()) {
            player.sendSystemMessage(Component.literal("no items in xyz_items tag!").withStyle(ChatFormatting.RED));
            return;
        }
        var chosenItem = items.get(level.random.nextInt(items.size()));
        int maxStack = chosenItem.getMaxStackSize();
        int amount = maxStack <= 1 ? 1 : (level.random.nextBoolean() ? 1 : Math.min(maxStack, 2 + level.random.nextInt(15)));

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
