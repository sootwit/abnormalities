package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AbnormalitiesMod.MODID);
    public static final RegistryObject<SoundEvent> NUR_SOUND = SOUNDS.register("nur_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "nur_sound")));
    public static final RegistryObject<SoundEvent> FRIEND_CRASH1 = SOUNDS.register("friend_crash1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "friend_crash1")));
    public static final RegistryObject<SoundEvent> FRIEND_CRASH2 = SOUNDS.register("friend_crash2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "friend_crash2")));
    public static final RegistryObject<SoundEvent> FRIEND_CRASH3 = SOUNDS.register("friend_crash3",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "friend_crash3")));
    public static final RegistryObject<SoundEvent> FRIEND_CRASH4 = SOUNDS.register("friend_crash4",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "friend_crash4")));
    public static final RegistryObject<SoundEvent> WHISPER_SOUND = SOUNDS.register("whisper_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "whisper_sound")));
    public static final RegistryObject<SoundEvent> HEARTBEAT_SOUND = SOUNDS.register("heartbeat_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "heartbeat_sound")));
    public static final RegistryObject<SoundEvent> TINNITUS_SOUND = SOUNDS.register("tinnitus_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "tinnitus_sound")));
    public static final RegistryObject<SoundEvent> SEGFAULT_STOP = SOUNDS.register("segfault_stop",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "segfault_stop")));
    public static final RegistryObject<SoundEvent> SEGFAULT_CONTINUE = SOUNDS.register("segfault_continue",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "segfault_continue")));
    public static final RegistryObject<SoundEvent> SEGFAULT_AMBIENCE = SOUNDS.register("segfault_ambience",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "segfault_ambience")));
    public static final RegistryObject<SoundEvent> HIM_BOSS1 = SOUNDS.register("himboss1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "himboss1")));
    public static final RegistryObject<SoundEvent> HIM_BOSS2 = SOUNDS.register("himboss2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "himboss2")));
    public static final RegistryObject<SoundEvent> HIM_BOSS3 = SOUNDS.register("himboss3",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "himboss3")));
    public static final RegistryObject<SoundEvent> HIM_BOSS4 = SOUNDS.register("himboss4",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "himboss4")));
    public static final RegistryObject<SoundEvent> PILLAR_ALARM = SOUNDS.register("pillaralarm",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "pillaralarm")));
    public static final RegistryObject<SoundEvent> WARNING = SOUNDS.register("warning",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "warning")));
    public static final RegistryObject<SoundEvent> THUMP = SOUNDS.register("thump",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "thump")));
    public static final RegistryObject<SoundEvent> LOWFREQ = SOUNDS.register("lowfreq",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "lowfreq")));
}
