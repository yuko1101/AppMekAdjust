package io.github.yuko1101.appmekadjust.fabric;

import io.github.yuko1101.appmekadjust.AppMekAdjust;
import net.fabricmc.api.ModInitializer;

public final class AppMekAdjustFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AppMekAdjust.init();
    }
}
