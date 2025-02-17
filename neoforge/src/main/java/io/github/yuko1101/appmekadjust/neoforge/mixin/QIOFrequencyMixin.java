package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.IBasicCellItem;
import io.github.yuko1101.appmekadjust.neoforge.mixin.accessor.QIOItemTypeDataAccessor;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIODriveDataExtension;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIOFrequencyExtension;
import io.github.yuko1101.appmekadjust.neoforge.qio.QIOStorageCellData;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveData;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.lib.inventory.HashedItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.SequencedMap;

@Mixin(QIOFrequency.class)
public abstract class QIOFrequencyMixin implements QIOFrequencyExtension {
    @Shadow @Final private SequencedMap<QIODriveData.QIODriveKey, QIODriveData> driveMap;

    @Shadow public abstract void removeDrive(QIODriveData.QIODriveKey key, boolean updateItemMap);

    @Shadow private long totalCountCapacity;

    @Shadow private int totalTypeCapacity;

    @Shadow @Final private SequencedMap<HashedItem, QIOFrequency.QIOItemTypeData> itemDataMap;

    @Shadow protected abstract void markForUpdate(HashedItem changedItem);

    @Shadow protected abstract QIOFrequency.QIOItemTypeData createTypeDataForAbsent(HashedItem type);

    @Shadow protected abstract void setNeedsUpdate();

    @Unique
    private final HashMap<AEKey, HashMap<QIODriveData.QIODriveKey, Long>> appMekAdjust$ae2ItemMap = new HashMap<>();

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
            for (Object2LongMap.Entry<AEKey> entry : ((QIODriveDataExtension) data).appMekAdjust$getAE2ItemMap().object2LongEntrySet()) {
                AEKey aeKey = entry.getKey();
                appMekAdjust$ae2ItemMap.computeIfAbsent(aeKey, k -> new HashMap<>()).put(key, entry.getLongValue());
            }
            setNeedsUpdate();
        }
    }

    @Inject(method = "removeDrive", at = @At(value = "INVOKE", target = "Lmekanism/common/content/qio/QIOFrequency;setNeedsUpdate()V"))
    private void onRemoveDrive(QIODriveData.QIODriveKey key, boolean updateItemMap, CallbackInfo ci) {
        QIODriveDataExtension data = ((QIODriveDataExtension) this.driveMap.get(key));

        for (Object2LongMap.Entry<AEKey> entry : data.appMekAdjust$getAE2ItemMap().object2LongEntrySet()) {
            AEKey aeKey = entry.getKey();
            HashMap<QIODriveData.QIODriveKey, Long> map = appMekAdjust$ae2ItemMap.get(aeKey);
            if (map == null) return;
            map.remove(key);
            if (map.isEmpty()) {
                appMekAdjust$ae2ItemMap.remove(aeKey);
            }
        }
    }

    @Override
    public HashMap<AEKey, HashMap<QIODriveData.QIODriveKey, Long>> appMekAdjust$getAE2ItemMap() {
        return appMekAdjust$ae2ItemMap;
    }

    @Override
    public long appMekAdjust$massInsertAE2Items(AEKey key, long amount, Action action, IActionSource source) {
        if (amount <= 0) return 0;

        var remaining = amount;
        var map = this.appMekAdjust$ae2ItemMap.computeIfAbsent(key, k -> new HashMap<>());

        for (var driveKey : this.driveMap.keySet()) {
            if (remaining <= 0) break;

            var driveData = this.driveMap.get(driveKey);
            if (driveData == null) continue;

            if (driveData instanceof QIOStorageCellData cellData) {
                var inserted = cellData.insert(key, remaining, action, source);
                if (action.execute()) {
                    map.put(driveKey, map.getOrDefault(driveKey, 0L) + inserted);
                }
                remaining -= inserted;
            }
        }

        return amount - remaining;
    }

    @Override
    public long appMekAdjust$massExtractAE2Items(AEKey key, long amount, Action action, IActionSource source) {
        if (amount <= 0) return 0;

        var remaining = amount;
        var map = this.appMekAdjust$ae2ItemMap.computeIfAbsent(key, k -> new HashMap<>());

        for (var driveEntry : map.entrySet()) {
            if (remaining <= 0) break;

            var drive = (QIOStorageCellData) this.driveMap.get(driveEntry.getKey());
            if (drive == null) continue;

            var extracted = drive.extract(key, remaining, action, source);
            if (action.execute()) {
                var newAmount = driveEntry.getValue() - extracted;
                if (newAmount < 0) throw new IllegalStateException("Sync error: " + key + " is extracted more than stored (Requested: " + amount + ", Extracted: " + extracted + ", Stored: " + driveEntry.getValue() + ")");
                map.put(driveEntry.getKey(), newAmount);
            }
            remaining -= extracted;
        }

        return amount - remaining;
    }
}
