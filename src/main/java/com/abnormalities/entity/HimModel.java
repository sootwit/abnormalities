package com.abnormalities.entity;

import com.abnormalities.AbnormalitiesMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HimModel extends GeoModel<HimEntity> {
    @Override
    public ResourceLocation getModelResource(HimEntity object) {
        return new ResourceLocation(AbnormalitiesMod.MODID, "geo/him.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HimEntity object) {
        return object.isBoss()
            ? new ResourceLocation(AbnormalitiesMod.MODID, "textures/entity/himchase.png")
            : new ResourceLocation(AbnormalitiesMod.MODID, "textures/entity/him.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HimEntity object) {
        return new ResourceLocation(AbnormalitiesMod.MODID, "animations/him.animation.json");
    }
}
