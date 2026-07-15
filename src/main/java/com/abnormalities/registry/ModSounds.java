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
}
