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
    public static final RegistryObject<SoundEvent> K3W_CRASH1 = SOUNDS.register("k3w_crash1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "k3w_crash1")));
    public static final RegistryObject<SoundEvent> K3W_CRASH2 = SOUNDS.register("k3w_crash2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "k3w_crash2")));
    public static final RegistryObject<SoundEvent> K3W_CRASH3 = SOUNDS.register("k3w_crash3",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "k3w_crash3")));
    public static final RegistryObject<SoundEvent> K3W_CRASH4 = SOUNDS.register("k3w_crash4",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "k3w_crash4")));
    public static final RegistryObject<SoundEvent> WHISPER_SOUND = SOUNDS.register("whisper_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "whisper_sound")));
    public static final RegistryObject<SoundEvent> HEARTBEAT_SOUND = SOUNDS.register("heartbeat_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "heartbeat_sound")));
    public static final RegistryObject<SoundEvent> TINNITUS_SOUND = SOUNDS.register("tinnitus_sound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "tinnitus_sound")));
    public static final RegistryObject<SoundEvent> VR9P_STOP = SOUNDS.register("vr9p_stop",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "vr9p_stop")));
    public static final RegistryObject<SoundEvent> VR9P_CONTINUE = SOUNDS.register("vr9p_continue",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "vr9p_continue")));
    public static final RegistryObject<SoundEvent> VR9P_AMBIENCE = SOUNDS.register("vr9p_ambience",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AbnormalitiesMod.MODID, "vr9p_ambience")));
}
