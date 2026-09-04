package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.entity.DistantEntity;
import com.abnormalities.entity.DistantRenderer;
import com.abnormalities.entity.HimEntity;
import com.abnormalities.entity.HimRenderer;
import com.abnormalities.entity.K3wEntity;
import com.abnormalities.entity.K3wRenderer;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.entity.NurRenderer;
import com.abnormalities.entity.XyzEntity;
import com.abnormalities.entity.XyzRenderer;
import com.abnormalities.sign.HimSignEntity;
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
    public static final RegistryObject<EntityType<HimEntity>> HIM = ENTITIES.register("him", () ->
            EntityType.Builder.of(HimEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.2f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("him"));
    public static final RegistryObject<EntityType<HimSignEntity>> HIM_SIGN = ENTITIES.register("him_sign", () ->
            EntityType.Builder.of(HimSignEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.2f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("him_sign"));
    public static final RegistryObject<EntityType<DistantEntity>> DISTANT = ENTITIES.register("distant", () ->
            EntityType.Builder.of(DistantEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("distant"));
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(NUR.get(), NurEntity.createAttributes().build());
        event.put(K3W.get(), K3wEntity.createAttributes().build());
        event.put(XYZ.get(), XyzEntity.createAttributes().build());
        event.put(HIM.get(), HimEntity.createAttributes().build());
        event.put(HIM_SIGN.get(), HimSignEntity.createAttributes().build());
        event.put(DISTANT.get(), DistantEntity.createAttributes().build());
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NUR.get(), NurRenderer::new);
        event.registerEntityRenderer(K3W.get(), K3wRenderer::new);
        event.registerEntityRenderer(XYZ.get(), XyzRenderer::new);
        event.registerEntityRenderer(HIM.get(), HimRenderer::new);
        event.registerEntityRenderer(HIM_SIGN.get(), HimSignEntity.HimSignRenderer::new);
        event.registerEntityRenderer(DISTANT.get(), DistantRenderer::new);
    }
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.abnormalities.entity.K3wModel.LAYER_LOCATION, com.abnormalities.entity.K3wModel::createBodyLayer);
        event.registerLayerDefinition(com.abnormalities.entity.K3wModel.LAYER_LOCATION_SLIM, com.abnormalities.entity.K3wModel::createSlimBodyLayer);
    }
}
