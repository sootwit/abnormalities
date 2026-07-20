package com.abnormalities;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.entity.NurHorrorCycle;
import com.abnormalities.horror.*;
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
        MinecraftForge.EVENT_BUS.register(K3wActionTracker.class);
        MinecraftForge.EVENT_BUS.register(AbnormalitiesCommands.class);
        MinecraftForge.EVENT_BUS.register(ReputationManager.class);
        MinecraftForge.EVENT_BUS.register(ActionLogger.class);
        MinecraftForge.EVENT_BUS.register(HorrorEventPool.class);
        MinecraftForge.EVENT_BUS.register(MirrorStageEvent.class);
        MinecraftForge.EVENT_BUS.register(SomeoneElsesBuildEvent.class);
        MinecraftForge.EVENT_BUS.register(TheTallyEvent.class);
        MinecraftForge.EVENT_BUS.register(CountTheKnocksEvent.class);

        HorrorEventPool.register(new FogKnowsYourNameEvent());
        HorrorEventPool.register(new DontOpenYourEyesEvent());
        HorrorEventPool.register(new CountTheKnocksEvent());
        HorrorEventPool.register(new MirrorStageEvent());
        HorrorEventPool.register(new SomeoneElsesBuildEvent());
        HorrorEventPool.register(new TheTallyEvent());
        HorrorEventPool.register(new ItRemembersEvent());
        HorrorEventPool.register(new HotbarNurEvent());
        HorrorEventPool.register(new HelpHotbarEvent());
    }
}
