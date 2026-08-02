package com.abnormalities;

import com.abnormalities.config.AbnormalitiesConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigCommand {
    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(allKeys(), builder);

    private static final SuggestionProvider<CommandSourceStack> VALUE_SUGGESTIONS =
            (ctx, builder) -> {
                String key = StringArgumentType.getString(ctx, "key");
                List<String> vals = valueSuggestions(key);
                return SharedSuggestionProvider.suggest(vals.isEmpty() ? List.of() : vals, builder);
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abnorm_config")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> listAll(ctx.getSource()))
                .then(Commands.argument("key", StringArgumentType.word())
                        .suggests(KEY_SUGGESTIONS)
                        .executes(ctx -> showOne(ctx.getSource(), StringArgumentType.getString(ctx, "key")))
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests(VALUE_SUGGESTIONS)
                                .executes(ctx -> setOne(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "key"),
                                        StringArgumentType.getString(ctx, "value"))))));
    }

    private static List<String> allKeys() {
        return new ArrayList<>(flatten(AbnormalitiesConfig.SPEC.getValues(), "").keySet());
    }

    private static List<String> valueSuggestions(String key) {
        var all = flatten(AbnormalitiesConfig.SPEC.getValues(), "");
        ForgeConfigSpec.ConfigValue<?> cv = all.get(key);
        if (cv == null) return List.of();
        if (cv instanceof ForgeConfigSpec.BooleanValue) return List.of("true", "false");
        if (cv instanceof ForgeConfigSpec.EnumValue) {
            ForgeConfigSpec.ValueSpec vs = specFor(key);
            Class<?> clazz = vs == null ? null : vs.getClazz();
            if (clazz != null && clazz.isEnum()) {
                List<String> out = new ArrayList<>();
                for (Object c : clazz.getEnumConstants()) out.add(((Enum<?>) c).name().toLowerCase());
                return out;
            }
        }
        return List.of();
    }

    private static Map<String, ForgeConfigSpec.ConfigValue<?>> flatten(UnmodifiableConfig cfg, String prefix) {
        Map<String, ForgeConfigSpec.ConfigValue<?>> out = new LinkedHashMap<>();
        for (var entry : cfg.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object v = entry.getValue();
            if (v instanceof UnmodifiableConfig nested) {
                out.putAll(flatten(nested, key));
            } else if (v instanceof ForgeConfigSpec.ConfigValue<?> cv) {
                out.put(key, cv);
            }
        }
        return out;
    }

    private static ForgeConfigSpec.ValueSpec specFor(String key) {
        Object cur = AbnormalitiesConfig.SPEC.getSpec();
        for (String p : key.split("\\.")) {
            if (!(cur instanceof UnmodifiableConfig uc)) return null;
            cur = uc.get(p);
        }
        return cur instanceof ForgeConfigSpec.ValueSpec vs ? vs : null;
    }

    private static int listAll(CommandSourceStack src) {
        var all = flatten(AbnormalitiesConfig.SPEC.getValues(), "");
        var keys = new ArrayList<>(all.keySet());
        keys.sort(Comparator.naturalOrder());
        if (keys.isEmpty()) {
            src.sendSuccess(() -> Component.literal("no config values found"), false);
            return Command.SINGLE_SUCCESS;
        }
        src.sendSuccess(() -> Component.literal("abnormalities config (" + keys.size() + " values):")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        for (String key : keys) {
            src.sendSuccess(() -> Component.literal(key + " = " + all.get(key).get()).withStyle(ChatFormatting.GRAY), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showOne(CommandSourceStack src, String key) {
        ForgeConfigSpec.ConfigValue<?> cv = flatten(AbnormalitiesConfig.SPEC.getValues(), "").get(key);
        if (cv == null) {
            src.sendFailure(Component.literal("no config key '" + key + "'. use /abnorm_config to list all"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(key + " = " + cv.get()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setOne(CommandSourceStack src, String key, String raw) {
        ForgeConfigSpec.ConfigValue<?> cv = flatten(AbnormalitiesConfig.SPEC.getValues(), "").get(key);
        if (cv == null) {
            src.sendFailure(Component.literal("no config key '" + key + "'. use /abnorm_config to list all"));
            return 0;
        }
        Object parsed = parseValue(src, cv, key, raw);
        if (parsed == null) return 0;
        ((ForgeConfigSpec.ConfigValue<Object>) cv).set(parsed);
        AbnormalitiesConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("set " + key + " = " + parsed + " (saved to disk)").withStyle(ChatFormatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static Object parseValue(CommandSourceStack src, ForgeConfigSpec.ConfigValue<?> cv, String key, String raw) {
        ForgeConfigSpec.ValueSpec vs = specFor(key);
        if (cv instanceof ForgeConfigSpec.BooleanValue) {
            if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                src.sendFailure(Component.literal("expected true or false for " + key));
                return null;
            }
            return Boolean.parseBoolean(raw);
        }
        if (cv instanceof ForgeConfigSpec.IntValue) {
            try {
                int i = Integer.parseInt(raw);
                if (vs != null && !vs.test(i)) {
                    src.sendFailure(Component.literal(key + " rejected: " + rangeHint(vs)));
                    return null;
                }
                return i;
            } catch (NumberFormatException e) {
                src.sendFailure(Component.literal("expected an integer for " + key));
                return null;
            }
        }
        if (cv instanceof ForgeConfigSpec.DoubleValue) {
            try {
                double d = Double.parseDouble(raw);
                if (vs != null && !vs.test(d)) {
                    src.sendFailure(Component.literal(key + " rejected: " + rangeHint(vs)));
                    return null;
                }
                return d;
            } catch (NumberFormatException e) {
                src.sendFailure(Component.literal("expected a number for " + key));
                return null;
            }
        }
        if (cv instanceof ForgeConfigSpec.EnumValue) {
            Class<?> clazz = vs != null ? vs.getClazz() : null;
            if (clazz != null && clazz.isEnum()) {
                for (Object c : clazz.getEnumConstants()) {
                    if (((Enum<?>) c).name().equalsIgnoreCase(raw)) return c;
                }
                src.sendFailure(Component.literal(key + " must be one of: " + java.util.Arrays.toString(clazz.getEnumConstants())));
                return null;
            }
        }
        src.sendFailure(Component.literal("unsupported config type for " + key));
        return null;
    }

    private static String rangeHint(ForgeConfigSpec.ValueSpec vs) {
        try {
            Object r = vs.getRange();
            if (r != null) {
                Object min = r.getClass().getMethod("getMin").invoke(r);
                Object max = r.getClass().getMethod("getMax").invoke(r);
                return "must be in [" + min + ", " + max + "]";
            }
        } catch (Exception e) {
        }
        return "value not accepted by this config";
    }
}
