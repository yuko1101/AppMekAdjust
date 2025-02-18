package io.github.yuko1101.appmekadjust.neoforge.qio;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import appeng.items.storage.BasicStorageCell;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIODriveDataExtension;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIOFrequencyExtension;
import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveData;
import mekanism.common.lib.inventory.HashedItem;
import net.minecraft.world.item.ItemStack;

public class QIOStorageCellData extends QIODriveData {
    private final ItemStack cellStack;
    private final IBasicCellItem cellItem;
    private final StorageCell delegate;

    private long ae2ItemCount;
    private long decreasedItemCapacity;

    public QIOStorageCellData(QIODriveKey key) {
        super(key);
        this.cellStack = key.getDriveStack();
        this.cellItem = (IBasicCellItem) cellStack.getItem();

        this.delegate = StorageCells.getCellInventory(cellStack, null);
        if (delegate == null) {
            throw new IllegalStateException("Failed to create storage cell for " + cellStack);
        }

        this.ae2ItemCount = ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().values().longStream().sum();
        this.decreasedItemCapacity = this.getCountCapacity() - getItemCountCapacity(cellStack, cellItem, getItemMap().size());
    }

    @Override
    public long add(HashedItem type, long amount, Action action) {
        var isNewType = !getItemMap().containsKey(type);
        var insertedIntoAE2 = delegate.insert(AEItemKey.of(type.getInternalStack()), amount, Actionable.of(action.toFluidAction()), IActionSource.empty());
        var inserted = insertedIntoAE2 - super.add(type, insertedIntoAE2, action);
        if (inserted != insertedIntoAE2) {
            throw new IllegalStateException("Sync error: " + type + " is inserted less than AE2 (Requested: " + amount + ", Inserted: " + inserted + ", InsertedIntoAE2: " + insertedIntoAE2 + ")");
        }
        if (action.execute() && inserted > 0 && isNewType) {
            decreaseItemCapacity(getDecrementItemCapacityPerType());
        }
        return amount - inserted;
    }

    @Override
    public long remove(HashedItem type, long amount, Action action) {
        var extractedFromAE2 = delegate.extract(AEItemKey.of(type.getInternalStack()), amount, Actionable.of(action.toFluidAction()), IActionSource.empty());
        var extracted = super.remove(type, extractedFromAE2, action);
        if (extracted != extractedFromAE2) {
            throw new IllegalStateException("Sync error: " + type + " is extracted less than AE2 (Requested: " + amount + ", Extracted: " + extracted + ", ExtractedFromAE2: " + extractedFromAE2 + ")");
        }
        if (action.execute() && extracted > 0 && getItemMap().getOrDefault(type, 0) == 0) {
            decreaseItemCapacity(-getDecrementItemCapacityPerType());
        }
        return extracted;
    }

    public long insert(AEKey key, long amount, Action action, IActionSource source) {
        if (amount <= 0) return 0;
        if (key instanceof AEItemKey itemKey) {
            return amount - add(HashedItem.create(itemKey.toStack()), amount, action);
        }
        var inserted = delegate.insert(key, amount, Actionable.of(action.toFluidAction()), source);
        if (action.execute()) {
            var ae2ItemMap = ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap();
            ae2ItemMap.compute(key, (k, v) -> (v == null ? 0 : v) + inserted);
            ae2ItemCount += inserted;
        }

        return inserted;
    }

    public long extract(AEKey key, long amount, Action action, IActionSource source) {
        if (amount <= 0) return 0;
        if (key instanceof AEItemKey itemKey) {
            return remove(HashedItem.create(itemKey.toStack()), amount, action);
        }
        var extracted = delegate.extract(key, amount, Actionable.of(action.toFluidAction()), source);
        if (action.execute()) {
            var ae2ItemMap = ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap();
            ae2ItemMap.compute(key, (k, v) -> {
                var newValue = (v == null ? 0 : v) - extracted;
                if (newValue < 0) throw new IllegalStateException("Sync error: " + key + " is extracted more than stored (Requested: " + amount + ", Extracted: " + extracted + ", Stored: " + (v == null ? 0 : v) + ")");
                return newValue;
            });
            if (ae2ItemMap.getLong(key) == 0) ae2ItemMap.removeLong(key);

            ae2ItemCount -= extracted;
        }

        return extracted;
    }

    public long getAE2ItemCount() {
        return ae2ItemCount;
    }

    public int getAE2TypeCount() {
        return ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().size();
    }

    public long getAE2ItemCapacity() {
        if (cellItem instanceof BasicStorageCell basicStorageCell && basicStorageCell.getKeyType() != AEKeyType.items()) {
            return (cellItem.getBytes(cellStack) - (long) getAE2TypeCount() * 8) * 8;
        }
        return 0;
    }

    public int getAE2TypeCapacity() {
        if (cellItem instanceof BasicStorageCell basicStorageCell && basicStorageCell.getKeyType() != AEKeyType.items()) {
            return cellItem.getTotalTypes(cellStack);
        }
        return 0;
    }

    public long getDecrementItemCapacityPerType() {
        return 64;
    }

    public long getDecreasedItemCapacity() {
        return decreasedItemCapacity;
    }

    private void decreaseItemCapacity(long amount) {
        // self
        this.decreasedItemCapacity += amount;

        // holder
        var freq = this.getKey().holder().getQIOFrequency();
        if (freq == null) return;
        var freqExtension = (QIOFrequencyExtension) freq;
        freqExtension.appMekAdjust$decreaseItemCapacity(amount);
    }

    public static long getItemCountCapacity(ItemStack cellStack, IBasicCellItem cellItem, int usedTypes) {
        if (cellItem instanceof BasicStorageCell basicStorageCell && basicStorageCell.getKeyType() == AEKeyType.items()) {
            return (cellItem.getBytes(cellStack) - (long) usedTypes * 8) * 8;
        }
        // TODO: check if other cell items can store items
        return 0;
    }

    public static int getItemTypeCapacity(ItemStack cellStack, IBasicCellItem cellItem) {
        if (cellItem instanceof BasicStorageCell basicStorageCell && basicStorageCell.getKeyType() == AEKeyType.items()) {
            return cellItem.getTotalTypes(cellStack);
        }
        // TODO: check if other cell items can store items
        return 0;
    }
}
