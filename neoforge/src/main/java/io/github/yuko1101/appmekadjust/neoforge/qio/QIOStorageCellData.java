package io.github.yuko1101.appmekadjust.neoforge.qio;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIODriveDataExtension;
import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveData;
import net.minecraft.world.item.ItemStack;

public class QIOStorageCellData extends QIODriveData {
    private final ItemStack cellStack;
    private final IBasicCellItem cellItem;
    private final StorageCell delegate;

    public QIOStorageCellData(QIODriveKey key) {
        super(key);
        this.cellStack = key.getDriveStack();
        this.cellItem = (IBasicCellItem) cellStack.getItem();

        this.delegate = StorageCells.getCellInventory(cellStack, null);
        if (delegate == null) {
            throw new IllegalStateException("Failed to create storage cell for " + cellStack);
        }
    }

    public long insert(AEKey key, long amount, Action action, IActionSource source) {
        var inserted = delegate.insert(key, amount, Actionable.of(action.toFluidAction()), source);
        if (action.execute()) ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().compute(key, (k, v) -> (v == null ? 0 : v) + inserted);
        return inserted;
    }

    public long extract(AEKey key, long amount, Action action, IActionSource source) {
        var extracted = delegate.extract(key, amount, Actionable.of(action.toFluidAction()), source);
        if (action.execute()) ((QIODriveDataExtension) this).appMekAdjust$getAE2ItemMap().compute(key, (k, v) -> {
            var newValue = (v == null ? 0 : v) - extracted;
            if (newValue < 0) throw new IllegalStateException("Sync error: " + key + " is extracted more than stored (Requested: " + amount + ", Extracted: " + extracted + ", Stored: " + (v == null ? 0 : v) + ")");
            return newValue;
        });
        return extracted;
    }

    public static long getCountCapacity(ItemStack cellStack, IBasicCellItem cellItem) {
        return (cellItem.getBytes(cellStack) - (long) getTypeCapacity(cellStack, cellItem)) * 8;
    }

    public static int getTypeCapacity(ItemStack cellStack, IBasicCellItem cellItem) {
        return cellItem.getTotalTypes(cellStack);
    }
}
