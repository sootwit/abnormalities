package com.abnormalities;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.entity.NurHorrorCycle;
import com.abnormalities.horror.*;
import com.abnormalities.network.Vr9pPacket;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModEvents;
import com.abnormalities.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(AbnormalitiesMod.MODID)
public class AbnormalitiesMod {
    public static final String MODID = "abnormalities";
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

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
        MinecraftForge.EVENT_BUS.register(Vr9pController.class);

        HorrorEventPool.register(new FogKnowsYourNameEvent());
        HorrorEventPool.register(new DontOpenYourEyesEvent());
        HorrorEventPool.register(new CountTheKnocksEvent());
        HorrorEventPool.register(new MirrorStageEvent());
        HorrorEventPool.register(new SomeoneElsesBuildEvent());
        HorrorEventPool.register(new TheTallyEvent());
        HorrorEventPool.register(new ItRemembersEvent());
        HorrorEventPool.register(new HotbarNurEvent());
        HorrorEventPool.register(new HelpHotbarEvent());
        HorrorEventPool.register(new HighRepGateEvent());
        HorrorEventPool.register(new LowRepGateEvent());

        CHANNEL.registerMessage(0, Vr9pPacket.class, Vr9pPacket::encode, Vr9pPacket::decode, Vr9pPacket::handle);
    }
}
