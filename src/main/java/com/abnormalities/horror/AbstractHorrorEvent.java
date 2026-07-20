package com.abnormalities.horror;

import net.minecraft.server.level.ServerPlayer;

public abstract class AbstractHorrorEvent {
    protected final String name;
    protected final int baseWeight;
    protected final double hostilityFactor;
    protected final int minRep;
    protected final int maxRep;
    protected final int cooldownTicks;
    protected final boolean allowOngoing;

    protected AbstractHorrorEvent(String name, int baseWeight, double hostilityFactor) {
        this(name, baseWeight, hostilityFactor, 0, 2500, 36000, false);
    }

    protected AbstractHorrorEvent(String name, int baseWeight, double hostilityFactor, int minRep, int maxRep, int cooldownTicks, boolean allowOngoing) {
        this.name = name;
        this.baseWeight = baseWeight;
        this.hostilityFactor = hostilityFactor;
        this.minRep = minRep;
        this.maxRep = maxRep;
        this.cooldownTicks = cooldownTicks;
        this.allowOngoing = allowOngoing;
    }

    public String getName() { return name; }
    public int getBaseWeight() { return baseWeight; }
    public double getHostilityFactor() { return hostilityFactor; }
    public int getMinRep() { return minRep; }
    public int getMaxRep() { return maxRep; }
    public int getCooldownTicks() { return cooldownTicks; }
    public boolean allowsOngoing() { return allowOngoing; }

    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return !player.level().isClientSide;
    }

    public abstract void execute(ServerPlayer player);

    public void onPlayerTick(ServerPlayer player) {}

    public void onCleanup(ServerPlayer player) {}
}
