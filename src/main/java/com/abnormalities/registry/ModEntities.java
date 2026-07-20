package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.entity.K3wEntity;
import com.abnormalities.entity.K3wRenderer;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.entity.NurRenderer;
import com.abnormalities.entity.XyzEntity;
import com.abnormalities.entity.XyzRenderer;
import com.abnormalities.entity.skinwalker.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
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
    public static final RegistryObject<EntityType<ChickenNurEntity>> CHICKEN_NUR = ENTITIES.register("chicken_nur", () ->
            EntityType.Builder.of(ChickenNurEntity::new, MobCategory.CREATURE)
                    .sized(0.4f, 0.7f)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("chicken_nur"));
    public static final RegistryObject<EntityType<CowNurEntity>> COW_NUR = ENTITIES.register("cow_nur", () ->
            EntityType.Builder.of(CowNurEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 1.4f)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("cow_nur"));
    public static final RegistryObject<EntityType<SheepNurEntity>> SHEEP_NUR = ENTITIES.register("sheep_nur", () ->
            EntityType.Builder.of(SheepNurEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 1.3f)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("sheep_nur"));
    public static final RegistryObject<EntityType<PigNurEntity>> PIG_NUR = ENTITIES.register("pig_nur", () ->
            EntityType.Builder.of(PigNurEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("pig_nur"));
    public static final RegistryObject<EntityType<VillagerNurEntity>> VILLAGER_NUR = ENTITIES.register("villager_nur", () ->
            EntityType.Builder.of(VillagerNurEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("villager_nur"));
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(NUR.get(), NurEntity.createAttributes().build());
        event.put(K3W.get(), K3wEntity.createAttributes().build());
        event.put(XYZ.get(), XyzEntity.createAttributes().build());
        event.put(CHICKEN_NUR.get(), ChickenNurEntity.createAttributes().build());
        event.put(COW_NUR.get(), CowNurEntity.createAttributes().build());
        event.put(SHEEP_NUR.get(), SheepNurEntity.createAttributes().build());
        event.put(PIG_NUR.get(), PigNurEntity.createAttributes().build());
        event.put(VILLAGER_NUR.get(), VillagerNurEntity.createAttributes().build());
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NUR.get(), NurRenderer::new);
        event.registerEntityRenderer(K3W.get(), K3wRenderer::new);
        event.registerEntityRenderer(XYZ.get(), XyzRenderer::new);
        event.registerEntityRenderer(CHICKEN_NUR.get(), ChickenRenderer::new);
        event.registerEntityRenderer(COW_NUR.get(), CowRenderer::new);
        event.registerEntityRenderer(SHEEP_NUR.get(), SheepRenderer::new);
        event.registerEntityRenderer(PIG_NUR.get(), PigRenderer::new);
        event.registerEntityRenderer(VILLAGER_NUR.get(), VillagerRenderer::new);
    }
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.abnormalities.entity.K3wModel.LAYER_LOCATION, com.abnormalities.entity.K3wModel::createBodyLayer);
    }
}
