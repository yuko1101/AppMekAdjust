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
import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveData;
import mekanism.common.lib.inventory.HashedItem;
import net.minecraft.world.item.ItemStack;

public class QIOStorageCellData extends QIODriveData {
    private final ItemStack cellStack;
    private final IBasicCellItem cellItem;
    private final StorageCell delegate;

    private long ae2ItemCount;

    public QIOStorageCellData(QIODriveKey key) {
        super(key);
        this.cellStack = key.getDriveStack();
        this.cellItem = (IBasicCellItem) cellStack.getItem();

        this.delegate = StorageCells.getCellInventory(cellStack, null);
        if (delegate == null) {
            throw new IllegalStateException("Failed to create storage cell for " + cellStack);
        }

        this.ae2ItemCount = ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().values().longStream().sum();
    }

    public long insert(AEKey key, long amount, Action action, IActionSource source) {
        if (key instanceof AEItemKey itemKey) {
            var remaining = add(HashedItem.create(itemKey.toStack()), amount, action);
            return amount - remaining;
        }
        var inserted = delegate.insert(key, amount, Actionable.of(action.toFluidAction()), source);
        if (action.execute()) {
            ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().compute(key, (k, v) -> (v == null ? 0 : v) + inserted);
            ae2ItemCount += inserted;
        }

        return inserted;
    }

    public long extract(AEKey key, long amount, Action action, IActionSource source) {
        if (key instanceof AEItemKey itemKey) {
            var remaining = remove(HashedItem.create(itemKey.toStack()), amount, action);
            return amount - remaining;
        }
        var extracted = delegate.extract(key, amount, Actionable.of(action.toFluidAction()), source);
        if (action.execute()) {
            ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().compute(key, (k, v) -> {
                var newValue = (v == null ? 0 : v) - extracted;
                if (newValue < 0) throw new IllegalStateException("Sync error: " + key + " is extracted more than stored (Requested: " + amount + ", Extracted: " + extracted + ", Stored: " + (v == null ? 0 : v) + ")");
                return newValue;
            });
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

    public static long getItemCountCapacity(ItemStack cellStack, IBasicCellItem cellItem) {
        if (cellItem instanceof BasicStorageCell basicStorageCell && basicStorageCell.getKeyType() == AEKeyType.items()) {
            return (cellItem.getBytes(cellStack) - (long) getItemTypeCapacity(cellStack, cellItem) * 8) * 8;
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
