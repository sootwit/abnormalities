package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.entity.DistantEntity;
import com.abnormalities.entity.DistantRenderer;
import com.abnormalities.entity.HimEntity;
import com.abnormalities.entity.HimRenderer;
import com.abnormalities.entity.FriendEntity;
import com.abnormalities.entity.FriendRenderer;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.entity.NurRenderer;
import com.abnormalities.entity.TheMotherEntity;
import com.abnormalities.entity.TheMotherRenderer;
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
    public static final RegistryObject<EntityType<FriendEntity>> FRIEND = ENTITIES.register("friend", () ->
            EntityType.Builder.of(FriendEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .fireImmune()
                    .build("friend"));
    public static final RegistryObject<EntityType<TheMotherEntity>> XYZ = ENTITIES.register("the_mother", () ->
            EntityType.Builder.of(TheMotherEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 64.0f)
                    .clientTrackingRange(128)
                    .fireImmune()
                    .build("the_mother"));
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
        event.put(FRIEND.get(), FriendEntity.createAttributes().build());
        event.put(XYZ.get(), TheMotherEntity.createAttributes().build());
        event.put(HIM.get(), HimEntity.createAttributes().build());
        event.put(HIM_SIGN.get(), HimSignEntity.createAttributes().build());
        event.put(DISTANT.get(), DistantEntity.createAttributes().build());
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NUR.get(), NurRenderer::new);
        event.registerEntityRenderer(FRIEND.get(), FriendRenderer::new);
        event.registerEntityRenderer(XYZ.get(), TheMotherRenderer::new);
        event.registerEntityRenderer(HIM.get(), HimRenderer::new);
        event.registerEntityRenderer(HIM_SIGN.get(), HimSignEntity.HimSignRenderer::new);
        event.registerEntityRenderer(DISTANT.get(), DistantRenderer::new);
    }
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.abnormalities.entity.FriendModel.LAYER_LOCATION, com.abnormalities.entity.FriendModel::createBodyLayer);
        event.registerLayerDefinition(com.abnormalities.entity.FriendModel.LAYER_LOCATION_SLIM, com.abnormalities.entity.FriendModel::createSlimBodyLayer);
    }
}
