package com.abnormalities.entity;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;

public class ChoppyAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
    private final int fps;

    public ChoppyAnimationController(T animatable, String name, int transitionTickLength, AnimationStateHandler<T> stateHandler, int fps) {
        super(animatable, name, transitionTickLength, stateHandler);
        this.fps = fps;
    }

    @Override
    protected double adjustTick(double tick) {
        double step = 20.0 / fps;
        return Math.floor(tick / step) * step;
    }
}
