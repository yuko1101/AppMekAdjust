package io.github.yuko1101.appmekadjust.neoforge.qio;

import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import mekanism.common.content.qio.QIODriveData;
import net.minecraft.world.item.ItemStack;

public class QIOStorageCellData extends QIODriveData {
    private final ItemStack cellStack;
    private final IBasicCellItem cellItem;
    private final StorageCell delegate;

    public QIOStorageCellData(QIODriveData.QIODriveKey key) {
        super(key);
        this.cellStack = key.getDriveStack();
        this.cellItem = (IBasicCellItem) cellStack.getItem();

        this.delegate = StorageCells.getCellInventory(cellStack, null);
        if (delegate == null) {
            throw new IllegalStateException("Failed to create storage cell for " + cellStack);
        }
    }

    public static long getCountCapacity(ItemStack cellStack, IBasicCellItem cellItem) {
        return (cellItem.getBytes(cellStack) - (long) getTypeCapacity(cellStack, cellItem)) * 8;
    }

    public static int getTypeCapacity(ItemStack cellStack, IBasicCellItem cellItem) {
        return cellItem.getTotalTypes(cellStack);
    }
}
