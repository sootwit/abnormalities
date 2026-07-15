package com.abnormalities;

import com.abnormalities.client.K3wCrashOverlay;
import com.abnormalities.client.NurFlickerOverlay;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.entity.NurHorrorCycle;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModEvents;
import com.abnormalities.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AbnormalitiesMod.MODID)
public class AbnormalitiesMod {
    public static final String MODID = "abnormalities";
    public AbnormalitiesMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AbnormalitiesConfig.SPEC, "abnormalities.toml");
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        modBus.register(ModEntities.class);
        MinecraftForge.EVENT_BUS.register(ModEvents.class);
        MinecraftForge.EVENT_BUS.register(NurHorrorCycle.class);
        MinecraftForge.EVENT_BUS.register(NurFlickerOverlay.class);
        MinecraftForge.EVENT_BUS.register(K3wCrashOverlay.class);
        MinecraftForge.EVENT_BUS.register(K3wActionTracker.class);
        MinecraftForge.EVENT_BUS.register(AbnormalitiesCommands.class);
    }
}
