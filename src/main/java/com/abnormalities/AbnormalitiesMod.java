package com.abnormalities;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.K3wActionTracker;
import com.abnormalities.entity.NurHorrorCycle;
import com.abnormalities.horror.*;
import com.abnormalities.network.CrashPacket;
import com.abnormalities.network.Vr9pPacket;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModEvents;
import com.abnormalities.registry.ModSounds;
import com.abnormalities.registry.ModBlocks;
import com.abnormalities.registry.ModBlockEntities;
import com.abnormalities.sign.SignChunkGenerator;
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
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
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
        MinecraftForge.EVENT_BUS.register(V1s1tManager.class);
        MinecraftForge.EVENT_BUS.register(HushController.class);
        MinecraftForge.EVENT_BUS.register(WakeDisplacementEvent.class);
        MinecraftForge.EVENT_BUS.register(Vr9pStrictListener.class);
        MinecraftForge.EVENT_BUS.register(MisplaceManager.class);
        MinecraftForge.EVENT_BUS.register(MinerController.class);
        MinecraftForge.EVENT_BUS.register(SignManager.class);
        MinecraftForge.EVENT_BUS.register(StillnessManager.class);
        MinecraftForge.EVENT_BUS.register(CircleManager.class);
        MinecraftForge.EVENT_BUS.register(PhantomDrownManager.class);
        MinecraftForge.EVENT_BUS.register(ChatLockEvent.class);
        MinecraftForge.EVENT_BUS.register(FakeAchievementManager.class);
        MinecraftForge.EVENT_BUS.register(FakeChatManager.class);

        MinecraftForge.EVENT_BUS.register(B3drockManager.class);
        MinecraftForge.EVENT_BUS.register(SlowedMusicManager.class);
        MinecraftForge.EVENT_BUS.register(AnimalNoiseManager.class);
        MinecraftForge.EVENT_BUS.register(PeakDayManager.class);
        MinecraftForge.EVENT_BUS.register(CursedHouseManager.class);
        MinecraftForge.EVENT_BUS.register(CursedBiomeManager.class);
        MinecraftForge.EVENT_BUS.register(ApparitionManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.entity.HimTracker.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.hexnil.HexNilController.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.hexnil.HexNilPillarManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.hexnil.HexNilFarlandsManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.hexnil.HexNilFurtherlandsManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.hexnil.HexNilBorderManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.sign.SignDimension.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.horror.DarkAreaSoundManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.horror.DepthsManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.horror.DistantManager.class);
        MinecraftForge.EVENT_BUS.register(com.abnormalities.horror.NurSleepBlocker.class);

        modBus.addListener((net.minecraftforge.registries.RegisterEvent event) -> {
            event.register(net.minecraft.core.registries.Registries.CHUNK_GENERATOR, registry -> {
                registry.register(new ResourceLocation(MODID, "sign_generator"), SignChunkGenerator.CODEC);
            });
        });

        HorrorEventPool.register(new FogKnowsYourNameEvent());
        HorrorEventPool.register(new DontOpenYourEyesEvent());
        HorrorEventPool.register(new CountTheKnocksEvent());
        HorrorEventPool.register(new MirrorStageEvent());
        HorrorEventPool.register(new SomeoneElsesBuildEvent());
        HorrorEventPool.register(new TheTallyEvent());
        HorrorEventPool.register(new ItRemembersEvent());
        HorrorEventPool.register(new HotbarNurEvent());
        HorrorEventPool.register(new HelpHotbarEvent());
        HorrorEventPool.register(new ChatLockEvent(true));
        HorrorEventPool.register(new ChatLockEvent(false));
        HorrorEventPool.register(new VoidEvent());

        CHANNEL.registerMessage(0, Vr9pPacket.class, Vr9pPacket::encode, Vr9pPacket::decode, Vr9pPacket::handle);
        CHANNEL.registerMessage(1, CrashPacket.class, CrashPacket::encode, CrashPacket::decode, CrashPacket::handle);
        CHANNEL.registerMessage(2, com.abnormalities.network.EscViolationPacket.class,
                com.abnormalities.network.EscViolationPacket::encode, com.abnormalities.network.EscViolationPacket::decode,
                com.abnormalities.network.EscViolationPacket::handle);
        CHANNEL.registerMessage(5, com.abnormalities.network.FakeAchievementPacket.class,
                com.abnormalities.network.FakeAchievementPacket::encode, com.abnormalities.network.FakeAchievementPacket::decode,
                com.abnormalities.network.FakeAchievementPacket::handle);
        CHANNEL.registerMessage(6, com.abnormalities.network.HexNilShakePacket.class,
                com.abnormalities.network.HexNilShakePacket::encode, com.abnormalities.network.HexNilShakePacket::decode,
                com.abnormalities.network.HexNilShakePacket::handle);
        CHANNEL.registerMessage(9, com.abnormalities.network.ConfigScreenOpenPacket.class,
                com.abnormalities.network.ConfigScreenOpenPacket::encode, com.abnormalities.network.ConfigScreenOpenPacket::decode,
                com.abnormalities.network.ConfigScreenOpenPacket::handle);
        CHANNEL.registerMessage(10, com.abnormalities.network.AdvancedConfigOpenPacket.class,
                com.abnormalities.network.AdvancedConfigOpenPacket::encode, com.abnormalities.network.AdvancedConfigOpenPacket::decode,
                com.abnormalities.network.AdvancedConfigOpenPacket::handle);
        CHANNEL.registerMessage(11, com.abnormalities.network.K3wOverlayPacket.class,
                com.abnormalities.network.K3wOverlayPacket::encode, com.abnormalities.network.K3wOverlayPacket::decode,
                com.abnormalities.network.K3wOverlayPacket::handle);
        CHANNEL.registerMessage(12, com.abnormalities.network.SignTransitionPacket.class,
                com.abnormalities.network.SignTransitionPacket::encode, com.abnormalities.network.SignTransitionPacket::decode,
                com.abnormalities.network.SignTransitionPacket::handle);
        CHANNEL.registerMessage(13, com.abnormalities.network.K3wPossessPacket.class,
                com.abnormalities.network.K3wPossessPacket::encode, com.abnormalities.network.K3wPossessPacket::decode,
                com.abnormalities.network.K3wPossessPacket::handle);
        CHANNEL.registerMessage(14, com.abnormalities.network.DepthsPacket.class,
                com.abnormalities.network.DepthsPacket::encode, com.abnormalities.network.DepthsPacket::decode,
                com.abnormalities.network.DepthsPacket::handle);
        CHANNEL.registerMessage(15, com.abnormalities.network.DistantFlashbackPacket.class,
                com.abnormalities.network.DistantFlashbackPacket::encode, com.abnormalities.network.DistantFlashbackPacket::decode,
                com.abnormalities.network.DistantFlashbackPacket::handle);

    }
}
