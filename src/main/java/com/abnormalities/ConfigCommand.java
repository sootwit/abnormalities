package com.abnormalities;

import com.abnormalities.config.AbnormalitiesConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ConfigCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abnorm_config")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    UnmodifiableConfig values = AbnormalitiesConfig.SPEC.getValues();
                    var list = new java.util.ArrayList<String>();
                    for (var entry : values.entrySet()) {
                        Object v = entry.getValue();
                        if (v instanceof net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> cv) {
                            list.add(entry.getKey() + " = " + cv.get());
                        }
                    }
                    java.util.Collections.sort(list);
                    if (list.isEmpty()) {
                        src.sendSuccess(() -> Component.literal("no config values found"), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    src.sendSuccess(() -> Component.literal("abnormalities config (" + list.size() + " values):")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), false);
                    for (String line : list) {
                        src.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("key", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            String key = StringArgumentType.getString(ctx, "key");
                            UnmodifiableConfig values = AbnormalitiesConfig.SPEC.getValues();
                            Object v = values.get(key);
                            if (v instanceof net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> cv) {
                                src.sendSuccess(() -> Component.literal(key + " = " + cv.get())
                                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
                            } else {
                                src.sendFailure(Component.literal("no config key '" + key + "'. use /abnorm_config to list all"));
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
    }
}
