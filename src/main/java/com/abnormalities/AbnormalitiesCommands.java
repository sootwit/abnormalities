package com.abnormalities;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.FriendActionTracker;
import com.abnormalities.entity.TheMotherEntity;
import com.abnormalities.horror.HorrorEventPool;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModEvents;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AbnormalitiesCommands {
    private static final List<String> BASE_EVENTS = List.of("nur", "nur_sign", "friend", "the_mother", "him", "him_boss", "skinwalker", "segfault", "segfault_stargazed", "v1s1t", "hush", "w4k3", "m1sl4y", "m1n3r", "s1gn", "br34th", "h01d", "c1rcl", "chat_disabled", "chat_enabled", "ang3r", "fake_ach", "f4k3", "f4k3_join", "b3drock", "0x0000_pillar", "0x0000_chunk", "0x0000", "0x0000_farlands", "0x0000_furtherlands", "slowed_music", "animal_noise", "peak_bad", "peak_good", "sign_enter", "sign_exit", "depths", "0x0000_apparition", "0x0000_curse_biome", "0x0000_curse_house", "dark_area", "distant", "distant_circle", "distant_air", "distant_air_circle", "screen_shake", "thunder_to_player");
    private static final Random RNG = new Random();

    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEY_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(configAllKeys(), builder);
    private static final SuggestionProvider<CommandSourceStack> CONFIG_VALUE_SUGGESTIONS =
            (ctx, builder) -> {
                String key = StringArgumentType.getString(ctx, "key");
                List<String> vals = configValueSuggestions(key);
                return SharedSuggestionProvider.suggest(vals.isEmpty() ? List.of() : vals, builder);
            };

    private static List<String> allEvents() {
        List<String> all = new ArrayList<>(BASE_EVENTS);
        all.addAll(HorrorEventPool.getRegistered().stream().map(e -> e.getName()).collect(Collectors.toList()));
        return all;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abnormalities")
                .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("event")
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
                                            fireEvent(player, eventName, 0);
                                            src.sendSuccess(() -> Component.literal("triggered: " + eventName).withStyle(ChatFormatting.GREEN), false);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("intensity", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> {
                                                    CommandSourceStack src = ctx.getSource();
                                                    if (!(src.getEntity() instanceof ServerPlayer player)) {
                                                        src.sendFailure(Component.literal("must be a player"));
                                                        return 0;
                                                    }
                                                    String eventName = StringArgumentType.getString(ctx, "name");
                                                    int intensity = IntegerArgumentType.getInteger(ctx, "intensity");
                                                    List<String> events = allEvents();
                                                    if (!events.contains(eventName)) {
                                                        src.sendFailure(Component.literal("unknown event. valid: " + String.join(", ", events)));
                                                        return 0;
                                                    }
                                                    fireEvent(player, eventName, intensity);
                                                    src.sendSuccess(() -> Component.literal("triggered: " + eventName + " intensity=" + intensity).withStyle(ChatFormatting.GREEN), false);
                                                    return Command.SINGLE_SUCCESS;
                                                }))))
                .then(Commands.literal("random")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (!(src.getEntity() instanceof ServerPlayer player)) {
                                src.sendFailure(Component.literal("must be a player"));
                                return 0;
                            }
                            List<String> events = allEvents();
                            String eventName = events.get(RNG.nextInt(events.size()));
                            fireEvent(player, eventName, 0);
                            src.sendSuccess(() -> Component.literal("triggered: " + eventName).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("rep")
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
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 2500))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    ReputationManager.setRep(target, value);
                                                    ctx.getSource().sendSuccess(() ->
                                                        Component.literal("set " + target.getName().getString() + " rep to " + value), true);
                                                    return Command.SINGLE_SUCCESS;
                                                })))))
                .then(Commands.literal("config")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return 0;
                            com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                new com.abnormalities.network.ConfigScreenOpenPacket());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("advancedConfig")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) return 0;
                            com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                new com.abnormalities.network.AdvancedConfigOpenPacket());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(CONFIG_KEY_SUGGESTIONS)
                                .executes(ctx -> configShowOne(ctx.getSource(), StringArgumentType.getString(ctx, "key")))
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests(CONFIG_VALUE_SUGGESTIONS)
                                        .executes(ctx -> configSetOne(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "key"),
                                                StringArgumentType.getString(ctx, "value")))))));
    }

    private static void fireEvent(ServerPlayer player, String eventName, int intensity) {
        switch (eventName) {
            case "nur" -> ModEvents.forceNurSpawn(player);
            case "nur_sign" -> ModEvents.forceNurSignSpawn(player);
            case "friend" -> FriendActionTracker.forceFriendSpawn(player);
            case "the_mother" -> forceTheMotherSpawn(player);
            case "him" -> {
                if (!ModEvents.forceHimSpawn(player, false)) {
                    player.displayClientMessage(Component.literal("him couldn't spawn here").withStyle(ChatFormatting.GRAY), false);
                }
            }
            case "him_boss" -> {
                if (!ModEvents.forceHimSpawn(player, true)) {
                    player.displayClientMessage(Component.literal("him couldn't spawn here").withStyle(ChatFormatting.GRAY), false);
                }
            }
            case "skinwalker" -> forceSkinwalkerSpawn(player);
            case "segfault" -> com.abnormalities.horror.SegfaultController.forceStart(player);
            case "segfault_stargazed" -> com.abnormalities.horror.SegfaultController.forceStartStargazed(player);
            case "v1s1t" -> com.abnormalities.horror.V1s1tManager.forceVisit(player);
            case "hush" -> com.abnormalities.horror.HushController.forceStart(player);
            case "w4k3" -> com.abnormalities.horror.WakeDisplacementEvent.forceDisplace(player);
            case "m1sl4y" -> com.abnormalities.horror.MisplaceManager.forceMisplace(player);
            case "m1n3r" -> com.abnormalities.horror.MinerController.forceStart(player);
            case "s1gn" -> com.abnormalities.horror.SignManager.forceSign(player);
            case "br34th" -> com.abnormalities.horror.PhantomDrownManager.forceDrown(player);
            case "h01d" -> com.abnormalities.horror.StillnessManager.forceTrigger(player);
            case "c1rcl" -> com.abnormalities.horror.CircleManager.forceRing(player);
            case "fake_ach" -> com.abnormalities.horror.FakeAchievementManager.give(player);
            case "f4k3" -> com.abnormalities.horror.FakeChatManager.forceChat(player);
            case "f4k3_join" -> com.abnormalities.horror.FakeChatManager.forceJoinLeave();

            case "b3drock" -> com.abnormalities.horror.B3drockManager.forceB3drock(player);
            case "0x0000_pillar" -> com.abnormalities.hexnil.HexNilController.forcePillar(player);
            case "0x0000_chunk" -> com.abnormalities.hexnil.HexNilChunkManager.forceChunk(player);
            case "0x0000" -> com.abnormalities.hexnil.HexNilController.forceRandom(player);
            case "0x0000_farlands" -> com.abnormalities.hexnil.HexNilFarlandsManager.forceFarlands(player);
            case "0x0000_furtherlands" -> com.abnormalities.hexnil.HexNilFurtherlandsManager.forceFurtherlands(player);
            case "slowed_music" -> com.abnormalities.horror.SlowedMusicManager.forcePlay();
            case "animal_noise" -> com.abnormalities.horror.AnimalNoiseManager.forcePlay(player);
            case "chat_disabled" -> com.abnormalities.horror.ChatLockEvent.forceDisabled(player);
            case "chat_enabled" -> com.abnormalities.horror.ChatLockEvent.forceEnabled(player);
            case "ang3r" -> com.abnormalities.horror.AngerEvent.forceAnger(player);
            case "peak_bad" -> com.abnormalities.horror.PeakDayManager.forceBad(player);
            case "peak_good" -> com.abnormalities.horror.PeakDayManager.forceGood(player);
            case "0x0000_curse_house" -> {
                if (!com.abnormalities.horror.CursedHouseManager.forceCurse(player)) {
                    player.displayClientMessage(Component.literal("couldn't find a house").withStyle(ChatFormatting.GRAY), false);
                }
            }
            case "0x0000_curse_biome" -> {
                if (!com.abnormalities.horror.CursedBiomeManager.forceCursed(player)) {
                    player.displayClientMessage(Component.literal("couldn't determine biome").withStyle(ChatFormatting.GRAY), false);
                }
            }
            case "0x0000_apparition" -> com.abnormalities.horror.ApparitionManager.forceSpawn(player);
            case "sign_enter" -> com.abnormalities.sign.SignDimension.teleportToSign(player, player.blockPosition());
            case "sign_exit" -> com.abnormalities.sign.SignDimension.returnFromSign(player);
            case "depths" -> com.abnormalities.horror.DepthsManager.forceDepths(player);
            case "dark_area" -> com.abnormalities.horror.DarkAreaSoundManager.forceDarkArea(player);
            case "distant" -> com.abnormalities.horror.DistantManager.forceSpawn(player);
            case "distant_circle" -> com.abnormalities.horror.DistantManager.forceCircle(player);
            case "distant_air" -> com.abnormalities.horror.DistantManager.forceSpawnAir(player);
            case "distant_air_circle" -> com.abnormalities.horror.DistantManager.forceAirCircle(player);
            case "screen_shake" -> com.abnormalities.hexnil.ScreenShakeManager.sendShake(player, Math.max(1.0f, intensity), 120);
            case "thunder_to_player" -> com.abnormalities.horror.ThunderToPlayerEvent.forceThunder(player);
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

    public static void forceTheMotherSpawn(ServerPlayer player) {
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        var chosenItem = TheMotherEntity.pickNearbyItem(level, player.getX(), player.getZ());
        int maxStack = chosenItem.getMaxStackSize();
        int amount;
        if (AbnormalitiesConfig.THE_MOTHER_STATIC_AMOUNT.get()) {
            amount = Math.min(maxStack, AbnormalitiesConfig.THE_MOTHER_STATIC_ITEM_COUNT.get());
        } else {
            int min = Math.min(maxStack, AbnormalitiesConfig.THE_MOTHER_MIN_ITEMS.get());
            int max = Math.min(maxStack, AbnormalitiesConfig.THE_MOTHER_MAX_ITEMS.get());
            amount = max > min ? min + level.random.nextInt(max - min + 1) : min;
        }
        int envCount = TheMotherEntity.countNearbyBlocks(level, player.getX(), player.getZ(), chosenItem);
        if (envCount > 0 && amount > envCount) {
            amount = Math.max(1, envCount);
        }

        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 25.0D + level.random.nextDouble() * 20.0D;
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        BlockPos spawnPos = BlockPos.containing(sx, sy, sz);
        if (!level.getBlockState(spawnPos.below()).canOcclude()) {
            player.displayClientMessage(Component.literal("the mother couldn't find ground here").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        if (!level.getBlockState(spawnPos).canBeReplaced()) {
            player.displayClientMessage(Component.literal("the mother couldn't fit here").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        if (level.getBlockState(spawnPos).liquid()) {
            player.displayClientMessage(Component.literal("the mother won't spawn in water").withStyle(ChatFormatting.GRAY), false);
            return;
        }

        TheMotherEntity theMother = ModEntities.XYZ.get().create(level);
        if (theMother != null) {
            theMother.moveTo(sx + 0.5, sy, sz + 0.5, 0, 0);
            theMother.setTargetPlayer(player);
            int xc = ((int)Math.floor(sx)) >> 4;
            int zc = ((int)Math.floor(sz)) >> 4;
            level.setChunkForced(xc, zc, true);
            level.addFreshEntity(theMother);
            int seconds;
            if (AbnormalitiesConfig.THE_MOTHER_STATIC_WAIT.get()) {
                seconds = AbnormalitiesConfig.THE_MOTHER_STATIC_WAIT_SECONDS.get();
            } else {
                int min = AbnormalitiesConfig.THE_MOTHER_MIN_WAIT.get();
                int max = AbnormalitiesConfig.THE_MOTHER_MAX_WAIT.get();
                seconds = min + (max > min ? level.random.nextInt(max - min + 1) : 0);
            }
            theMother.startRequest(amount, chosenItem, seconds);

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
            theMother.setMessageSent(true);
        }
    }

    public static void forceSkinwalkerSpawn(ServerPlayer player) {
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        var nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64.0D),
            e -> e.getPersistentData().getBoolean("abnormalities:skinwalker"));
        if (nearby.size() >= 64) {
            player.sendSystemMessage(Component.literal("too many skinwalkers nearby"));
            return;
        }
        EntityType<?> disguise = ModEvents.pickRandomDisguise(level.random);
        if (disguise == null) {
            player.sendSystemMessage(Component.literal("no valid disguise found"));
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 10.0D + level.random.nextDouble() * 10.0D;
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        BlockPos spawnAt = BlockPos.containing(sx, sy + 1, sz);
        if (!level.getBlockState(spawnAt).isAir()) {
            spawnAt = player.blockPosition();
        }
        Entity raw = disguise.create(level);
        if (raw instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
            mob.getPersistentData().putBoolean("abnormalities:skinwalker", true);
            mob.goalSelector.addGoal(1, new com.abnormalities.entity.skinwalker.NurSkinwalkerApproachGoal(mob));
            mob.moveTo(spawnAt.getX() + 0.5, spawnAt.getY(), spawnAt.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0);
            level.addFreshEntity(mob);
            int cx = spawnAt.getX() >> 4;
            int cz = spawnAt.getZ() >> 4;
            level.setChunkForced(cx, cz, true);
            com.abnormalities.registry.ModEvents.registerSkinwalkerChunk(mob.getUUID(), cx, cz);
        }
    }

    private static List<String> configAllKeys() {
        return new ArrayList<>(configFlatten(AbnormalitiesConfig.SPEC.getValues(), "").keySet());
    }

    private static List<String> configValueSuggestions(String key) {
        var all = configFlatten(AbnormalitiesConfig.SPEC.getValues(), "");
        ForgeConfigSpec.ConfigValue<?> cv = all.get(key);
        if (cv == null) return List.of();
        if (cv instanceof ForgeConfigSpec.BooleanValue) return List.of("true", "false");
        if (cv instanceof ForgeConfigSpec.EnumValue) {
            ForgeConfigSpec.ValueSpec vs = configSpecFor(key);
            Class<?> clazz = vs == null ? null : vs.getClazz();
            if (clazz != null && clazz.isEnum()) {
                List<String> out = new ArrayList<>();
                for (Object c : clazz.getEnumConstants()) out.add(((Enum<?>) c).name().toLowerCase());
                return out;
            }
        }
        return List.of();
    }

    private static Map<String, ForgeConfigSpec.ConfigValue<?>> configFlatten(UnmodifiableConfig cfg, String prefix) {
        Map<String, ForgeConfigSpec.ConfigValue<?>> out = new LinkedHashMap<>();
        for (var entry : cfg.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object v = entry.getValue();
            if (v instanceof UnmodifiableConfig nested) {
                out.putAll(configFlatten(nested, key));
            } else if (v instanceof ForgeConfigSpec.ConfigValue<?> cv) {
                out.put(key, cv);
            }
        }
        return out;
    }

    private static ForgeConfigSpec.ValueSpec configSpecFor(String key) {
        Object cur = AbnormalitiesConfig.SPEC.getSpec();
        for (String p : key.split("\\.")) {
            if (!(cur instanceof UnmodifiableConfig uc)) return null;
            cur = uc.get(p);
        }
        return cur instanceof ForgeConfigSpec.ValueSpec vs ? vs : null;
    }

    private static int configListAll(CommandSourceStack src) {
        var all = configFlatten(AbnormalitiesConfig.SPEC.getValues(), "");
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

    private static int configShowOne(CommandSourceStack src, String key) {
        ForgeConfigSpec.ConfigValue<?> cv = configFlatten(AbnormalitiesConfig.SPEC.getValues(), "").get(key);
        if (cv == null) {
            src.sendFailure(Component.literal("no config key '" + key + "'. use /abnormalities advancedConfig to list all"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(key + " = " + cv.get()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int configSetOne(CommandSourceStack src, String key, String raw) {
        ForgeConfigSpec.ConfigValue<?> cv = configFlatten(AbnormalitiesConfig.SPEC.getValues(), "").get(key);
        if (cv == null) {
            src.sendFailure(Component.literal("no config key '" + key + "'. use /abnormalities advancedConfig to list all"));
            return 0;
        }
        Object parsed = configParseValue(src, cv, key, raw);
        if (parsed == null) return 0;
        ((ForgeConfigSpec.ConfigValue<Object>) cv).set(parsed);
        AbnormalitiesConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("set " + key + " = " + parsed + " (saved to disk)").withStyle(ChatFormatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static Object configParseValue(CommandSourceStack src, ForgeConfigSpec.ConfigValue<?> cv, String key, String raw) {
        ForgeConfigSpec.ValueSpec vs = configSpecFor(key);
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
                    src.sendFailure(Component.literal(key + " rejected: " + configRangeHint(vs)));
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
                    src.sendFailure(Component.literal(key + " rejected: " + configRangeHint(vs)));
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

    private static String configRangeHint(ForgeConfigSpec.ValueSpec vs) {
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
