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
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
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

    @Shadow public abstract long getTotalItemCountCapacity();

    @Shadow public abstract int getTotalItemTypes(boolean remote);

    @Unique
    private final HashMap<AEKey, HashMap<QIODriveData.QIODriveKey, Long>> appMekAdjust$ae2ItemMap = new HashMap<>();

    @Unique
    private long appMekAdjust$ae2ItemCount = 0;

    @Inject(method = "addDrive", at = @At("HEAD"))
    private void onAddDrive(QIODriveData.QIODriveKey key, CallbackInfo ci) {
        if (key.getDriveStack().getItem() instanceof IBasicCellItem) {
            if (driveMap.containsKey(key)) {
                removeDrive(key, true);
            }

            QIOStorageCellData data = new QIOStorageCellData(key);
            totalCountCapacity += data.getCountCapacity();
            totalCountCapacity -= data.getDecreasedItemCapacity();
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
                appMekAdjust$ae2ItemCount += entry.getLongValue();
            }
            setNeedsUpdate();
        }
    }

    @Inject(method = "removeDrive", at = @At(value = "INVOKE", target = "Lmekanism/common/content/qio/QIOFrequency;setNeedsUpdate()V"))
    private void onRemoveDrive(QIODriveData.QIODriveKey key, boolean updateItemMap, CallbackInfo ci) {
        QIODriveData drive = this.driveMap.get(key);
        QIODriveDataExtension driveExtension = (QIODriveDataExtension) drive;

        for (Object2LongMap.Entry<AEKey> entry : driveExtension.appMekAdjust$getAE2ItemMap().object2LongEntrySet()) {
            AEKey aeKey = entry.getKey();
            HashMap<QIODriveData.QIODriveKey, Long> map = appMekAdjust$ae2ItemMap.get(aeKey);
            if (map == null) return;
            map.remove(key);
            if (map.isEmpty()) {
                appMekAdjust$ae2ItemMap.remove(aeKey);
            }
            appMekAdjust$ae2ItemCount -= entry.getLongValue();
        }

        if (drive instanceof QIOStorageCellData cellData) {
            totalCountCapacity += cellData.getDecreasedItemCapacity();
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

        // first we try to add the items to an already-containing drive
        for (var driveEntry : map.entrySet()) {
            if (remaining <= 0) break;

            var cellData = (QIOStorageCellData) this.driveMap.get(driveEntry.getKey());
            if (cellData == null) continue;

            var inserted = cellData.insert(key, remaining, action, source);
            if (action.execute()) {
                var newAmount = driveEntry.getValue() + inserted;
                map.put(driveEntry.getKey(), newAmount);
                appMekAdjust$ae2ItemCount += inserted;
            }
            remaining -= inserted;
        }

        // next, we add the items to any drive that will take it
        if (remaining > 0) {
            for (var driveKey : this.driveMap.keySet()) {
                if (remaining <= 0) break;

                var driveData = this.driveMap.get(driveKey);
                if (driveData == null) continue;

                if (driveData instanceof QIOStorageCellData cellData) {
                    var inserted = cellData.insert(key, remaining, action, source);
                    if (action.execute()) {
                        map.put(driveKey, map.getOrDefault(driveKey, 0L) + inserted);
                        appMekAdjust$ae2ItemCount += inserted;
                    }
                    remaining -= inserted;
                }
            }
        }

        return amount - remaining;
    }

    @Override
    public long appMekAdjust$massExtractAE2Items(AEKey key, long amount, Action action, IActionSource source) {
        if (amount <= 0) return 0;

        var remaining = amount;
        var map = this.appMekAdjust$ae2ItemMap.computeIfAbsent(key, k -> new HashMap<>());

        var keysToRemove = new ArrayList<QIODriveData.QIODriveKey>();
        for (var driveEntry : map.entrySet()) {
            if (remaining <= 0) break;

            var cellData = (QIOStorageCellData) this.driveMap.get(driveEntry.getKey());
            if (cellData == null) continue;

            var extracted = cellData.extract(key, remaining, action, source);
            if (action.execute()) {
                var newAmount = driveEntry.getValue() - extracted;
                if (newAmount < 0) throw new IllegalStateException("Sync error: " + key + " is extracted more than stored (Requested: " + amount + ", Extracted: " + extracted + ", Stored: " + driveEntry.getValue() + ")");

                if (newAmount == 0) keysToRemove.add(driveEntry.getKey());
                else map.put(driveEntry.getKey(), newAmount);

                appMekAdjust$ae2ItemCount -= extracted;
            }
            remaining -= extracted;
        }
        keysToRemove.forEach(map::remove);

        return amount - remaining;
    }

    @Override
    public long appMekAdjust$getAE2ItemCount() {
        return appMekAdjust$ae2ItemCount;
    }

    @Override
    public long appMekAdjust$getTotalItemCapacity() {
        var ae2ItemCapacity = 0L;
        for (var drive : this.driveMap.values()) {
            if (drive instanceof QIOStorageCellData cellData) {
                ae2ItemCapacity += cellData.getCountCapacity();
            }
        }
        return getTotalItemCountCapacity() + ae2ItemCapacity;
    }

    @Override
    public long appMekAdjust$getTotalTypeCapacity(boolean remote) {
        if (remote) throw new NotImplementedException("Remote mode is not supported yet");
        var ae2TypeCapacity = 0;
        for (var drive : this.driveMap.values()) {
            if (drive instanceof QIOStorageCellData cellData) {
                ae2TypeCapacity += cellData.getTypeCapacity();
            }
        }
        return getTotalItemTypes(remote) + ae2TypeCapacity;
    }

    @Override
    public void appMekAdjust$decreaseItemCapacity(long amount) {
        totalCountCapacity -= amount;
    }
}
