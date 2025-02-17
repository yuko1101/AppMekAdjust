package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.storage.cells.IBasicCellItem;
import io.github.yuko1101.appmekadjust.neoforge.mixin.accessor.QIOItemTypeDataAccessor;
import io.github.yuko1101.appmekadjust.neoforge.qio.QIOStorageCellData;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import mekanism.common.content.qio.QIODriveData;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.lib.inventory.HashedItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;

@Mixin(QIOFrequency.class)
public abstract class QIOFrequencyMixin {
    @Shadow @Final private SequencedMap<QIODriveData.QIODriveKey, QIODriveData> driveMap;

    @Shadow public abstract void removeDrive(QIODriveData.QIODriveKey key, boolean updateItemMap);

    @Shadow private long totalCountCapacity;

    @Shadow private int totalTypeCapacity;

    @Shadow @Final private SequencedMap<HashedItem, QIOFrequency.QIOItemTypeData> itemDataMap;

    @Shadow protected abstract void markForUpdate(HashedItem changedItem);

    @Shadow protected abstract QIOFrequency.QIOItemTypeData createTypeDataForAbsent(HashedItem type);

    @Shadow protected abstract void setNeedsUpdate();

    @Inject(method = "addDrive", at = @At("HEAD"))
    private void onAddDrive(QIODriveData.QIODriveKey key, CallbackInfo ci) {
        if (key.getDriveStack().getItem() instanceof IBasicCellItem) {
            if (driveMap.containsKey(key)) {
                removeDrive(key, true);
            }

            QIOStorageCellData data = new QIOStorageCellData(key);
            totalCountCapacity += data.getCountCapacity();
            totalTypeCapacity += data.getTypeCapacity();
            driveMap.put(key, data);
            for (Object2LongMap.Entry<HashedItem> entry : data.getItemMap().object2LongEntrySet()) {
                HashedItem storedKey = entry.getKey();
                ((QIOItemTypeDataAccessor) itemDataMap.computeIfAbsent(storedKey, this::createTypeDataForAbsent)).invokeAddFromDrive(data, entry.getLongValue());
                markForUpdate(storedKey);
            }
            setNeedsUpdate();
        }
    }
}
