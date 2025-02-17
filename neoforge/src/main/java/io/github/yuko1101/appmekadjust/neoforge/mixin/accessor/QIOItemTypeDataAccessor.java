package io.github.yuko1101.appmekadjust.neoforge.mixin.accessor;

import mekanism.common.content.qio.QIODriveData;
import mekanism.common.content.qio.QIOFrequency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(QIOFrequency.QIOItemTypeData.class)
public interface QIOItemTypeDataAccessor {
    @Invoker("addFromDrive")
    void invokeAddFromDrive(QIODriveData data, long toAdd);
}
