package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.entity.K3wEntity;
import com.abnormalities.entity.K3wRenderer;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.entity.NurRenderer;
import com.abnormalities.entity.XyzEntity;
import com.abnormalities.entity.XyzRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AbnormalitiesMod.MODID);
    public static final RegistryObject<EntityType<NurEntity>> NUR = ENTITIES.register("nur", () ->
            EntityType.Builder.of(NurEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 5.34f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("nur"));
    public static final RegistryObject<EntityType<K3wEntity>> K3W = ENTITIES.register("k3w", () ->
            EntityType.Builder.of(K3wEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("k3w"));
    public static final RegistryObject<EntityType<XyzEntity>> XYZ = ENTITIES.register("xyz", () ->
            EntityType.Builder.of(XyzEntity::new, MobCategory.MONSTER)
                    .sized(4.0f, 60.0f)
                    .clientTrackingRange(128)
                    .fireImmune()
                    .build("xyz"));
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(NUR.get(), NurEntity.createAttributes().build());
        event.put(K3W.get(), K3wEntity.createAttributes().build());
        event.put(XYZ.get(), XyzEntity.createAttributes().build());
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NUR.get(), NurRenderer::new);
        event.registerEntityRenderer(K3W.get(), K3wRenderer::new);
        event.registerEntityRenderer(XYZ.get(), XyzRenderer::new);
    }
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.abnormalities.entity.K3wModel.LAYER_LOCATION, com.abnormalities.entity.K3wModel::createBodyLayer);
    }
}
