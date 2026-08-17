package com.abnormalities.horror;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.K3wActionTracker;

public class SisterActivityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|SisterActivity");

    private static final int DELAY_MIN = 40;
    private static final int DELAY_MAX = 100;

    private static final List<String> PARKOUR_LINES = List.of(
        "He likes it when you jump.",
        "Every gap is a choice. You keep choosing wrong.",
        "You're fast. Not fast enough. That's the trick.",
        "Careful. The ground remembers you.",
        "Nice jump. He clapped. You didn't hear it.",
        "You're not running from him. You're running toward the next mistake.",
        "Almost missed that one. Almost.",
        "He counts your steps. He's patient like that.",
        "Keep going. It's more interesting when you're tired.",
        "You jump well. Doesn't matter. He walks."
    );

    private static final List<String> BRIDGE_LINES = List.of(
        "Block after block. He walks through most of them.",
        "You can't build faster than he can wait.",
        "That wall won't hold. Nothing does.",
        "He's not chasing you. He's herding you.",
        "Funny. You build cages and call them safety.",
        "He'll be through in a second. Maybe two.",
        "Keep placing. It's not working. Keep placing anyway.",
        "That's not a bridge. That's a countdown.",
        "He's patient. You're not. That's the game.",
        "Told you so."
    );

    private static final List<String> STARE_LINES = List.of(
        "Don't blink. He's counting on it.",
        "He's still. You're still. Neither of you means it.",
        "That's not a staring contest. That's a truce.",
        "He'll wait longer than you will. He always does.",
        "Your eyes are watering. His aren't. He doesn't have any.",
        "Keep looking. Looking away is the only mistake here.",
        "He's not moving. That's the whole trick.",
        "You blinked. I saw it. He probably did too.",
        "Good. Hold it. It's the only thing keeping you alive.",
        "Barely."
    );

    private static final List<String> HUSH_LINES = List.of(
        "Everything's watching. Nothing's moving. Except you.",
        "Bold. Or stupid. Usually both.",
        "They're not asleep. They're waiting to see what you do.",
        "Keep walking. It's the only sound left.",
        "You're the loudest thing in the room. And you're barely breathing.",
        "They see you. They just don't care yet.",
        "That's not silence. That's patience.",
        "Walk slow. Slow doesn't help. But walk slow anyway.",
        "Nobody's blinking. You should try it.",
        "Almost through. Almost."
    );

    private static final List<String> LURE_LINES = List.of(
        "That's not music. That's an invitation.",
        "You hear it and you go. That's the whole trick.",
        "Keep walking. It gets quieter the closer you get. That's the joke.",
        "He didn't ask you to come. The box did that for him.",
        "You know this is a bad idea. You're still walking.",
        "That song doesn't end. Neither does this.",
        "Closer now. He likes closer.",
        "Funny how curiosity never learns.",
        "Almost there. That's worse.",
        "Told you not to follow it. You followed it anyway."
    );

    private static final List<String> MINER_LINES = List.of(
        "You're digging down. He's digging toward you. Guess who finishes first.",
        "Every swing you take, he takes two.",
        "That's not an echo. That's someone else's pickaxe.",
        "He's close. You can hear it. You're ignoring it. That's fine too.",
        "Mine faster. It won't help. Mine faster anyway.",
        "He doesn't get tired. You will.",
        "That wall's getting thin. So is your luck.",
        "Keep digging. He's almost done with his side.",
        "You found ore. He found you.",
        "Barely missed him. Barely."
    );

    private static final List<String> BED_LINES = List.of(
        "That bed remembers you. You don't remember it.",
        "He visited last time. You slept anyway.",
        "Same bed. Same mistake. That's the pattern.",
        "You'll dream about the last visit. You always do.",
        "He likes repeat customers.",
        "Sleep well. Or don't. He's not picky.",
        "That pillow's seen more than you have.",
        "You keep choosing this bed. He keeps noticing.",
        "Third time now. He's starting to expect you.",
        "Told you so."
    );

    private static final List<String> STILL_LINES = List.of(
        "Nothing's moving. Not even you. Good.",
        "That's the smartest thing you've done all day.",
        "They're all watching. You're all ignoring it. That works, for now.",
        "Stillness is the only trick that never fails. Mostly.",
        "Hold it. Hold it. Hold it.",
        "You're barely breathing. Keep it that way.",
        "Nobody blinks first in this room. Try to be nobody.",
        "That's not peace. That's a pause.",
        "Good. Stay boring. Boring survives.",
        "Almost over. Almost."
    );

    private static final List<String> GONE_LINES = List.of(
        "That torch won't last. Nothing does here.",
        "You're lighting a room he's already emptied.",
        "Place it anyway. It's not for you. It's for your nerves.",
        "That's not light. That's a suggestion.",
        "He'll take it eventually. He takes everything eventually.",
        "Funny. You keep trying to hold back the dark with sticks.",
        "That torch is already dimmer. You just haven't noticed.",
        "Keep placing them. It's cheaper than admitting it's not working.",
        "The dark doesn't leave. It just waits somewhere else.",
        "That's the trick. It's always the trick."
    );

    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        if (!AbnormalitiesConfig.SISTER_ENABLED.get()) return;
        if (!SisterController.isJoined()) return;

        for (var sp : srv.getPlayerList().getPlayers()) {
            UUID uuid = sp.getUUID();
            if (!SisterCooldownManager.canActivityComment(uuid)) continue;

            checkParkour(sp, uuid);
            checkBridge(sp, uuid);
            checkStare(sp, uuid);
            checkHush(sp, uuid);
            checkLure(sp, uuid);
            checkMiner(sp, uuid);
            checkBed(sp, uuid);
            checkStill(sp, uuid);
            checkGone(sp, uuid);
        }
    }

    private static void schedule(ServerPlayer player, List<String> pool, BooleanSupplier valid) {
        String msg = pool.get(RNG.nextInt(pool.size()));
        int delay = DELAY_MIN + RNG.nextInt(DELAY_MAX - DELAY_MIN);
        SisterCooldownManager.scheduleActivity(player, msg, delay, valid);
    }

    private static void checkParkour(ServerPlayer player, UUID uuid) {
        if (!ParkourTracker.isParkouring(uuid)) return;
        schedule(player, PARKOUR_LINES, () -> ParkourTracker.isParkouring(uuid));
    }

    private static void checkBridge(ServerPlayer player, UUID uuid) {
        if (!K3wActionTracker.hasActiveClone(uuid)) return;
        schedule(player, BRIDGE_LINES, () -> K3wActionTracker.hasActiveClone(uuid));
    }

    private static void checkStare(ServerPlayer player, UUID uuid) {
        if (!isStaringAtIt(player)) return;
        schedule(player, STARE_LINES, () -> isStaringAtIt(player));
    }

    private static void checkHush(ServerPlayer player, UUID uuid) {
        if (!HushController.isActive(uuid)) return;
        if (player.getDeltaMovement().horizontalDistanceSqr() < 0.001) return;
        schedule(player, HUSH_LINES, () -> HushController.isActive(uuid));
    }

    private static void checkLure(ServerPlayer player, UUID uuid) {
        if (!LureController.isActive(uuid)) return;
        BlockPos lurePos = LureController.getLurePosition(uuid);
        if (lurePos == null) return;
        double dx = player.getX() - (lurePos.getX() + 0.5);
        double dy = player.getY() - (lurePos.getY() + 0.5);
        double dz = player.getZ() - (lurePos.getZ() + 0.5);
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 20) return;
        schedule(player, LURE_LINES, () -> {
            if (!LureController.isActive(uuid)) return false;
            BlockPos lp = LureController.getLurePosition(uuid);
            if (lp == null) return false;
            double ldx = player.getX() - (lp.getX() + 0.5);
            double ldy = player.getY() - (lp.getY() + 0.5);
            double ldz = player.getZ() - (lp.getZ() + 0.5);
            return Math.sqrt(ldx * ldx + ldy * ldy + ldz * ldz) <= 20;
        });
    }

    private static void checkMiner(ServerPlayer player, UUID uuid) {
        if (!MinerController.hasSession(uuid)) return;
        schedule(player, MINER_LINES, () -> MinerController.hasSession(uuid));
    }

    private static void checkBed(ServerPlayer player, UUID uuid) {
        if (!player.isSleeping()) return;
        BlockPos bedPos = player.blockPosition();
        if (!BedMemoryManager.hasVisitedBed(uuid, bedPos)) return;
        schedule(player, BED_LINES, () -> player.isSleeping());
    }

    private static void checkStill(ServerPlayer player, UUID uuid) {
        if (!StillnessManager.isActive(uuid)) return;
        schedule(player, STILL_LINES, () -> StillnessManager.isActive(uuid));
    }

    private static void checkGone(ServerPlayer player, UUID uuid) {
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        if (!GoneController.wasRecentlyActive(uuid, srv.getTickCount())) return;
        schedule(player, GONE_LINES, () -> {
            var s = ServerLifecycleHooks.getCurrentServer();
            return s != null && GoneController.wasRecentlyActive(uuid, s.getTickCount());
        });
    }

    private static boolean isStaringAtIt(ServerPlayer player) {
        var entities = player.level().getEntitiesOfClass(
            com.abnormalities.entity.ItEntity.class,
            player.getBoundingBox().inflate(16),
            e -> e.isAlive()
        );
        if (entities.isEmpty()) return false;
        var it = entities.get(0);
        var look = player.getLookAngle();
        var toEntity = it.position().add(0, it.getBbHeight() / 2, 0).subtract(player.getEyePosition());
        double dot = look.normalize().dot(toEntity.normalize());
        return dot > 0.92;
    }
}
