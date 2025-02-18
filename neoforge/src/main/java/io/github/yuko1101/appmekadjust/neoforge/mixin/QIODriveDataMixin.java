package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import appeng.me.cells.BasicCellInventory;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.yuko1101.appmekadjust.neoforge.mixin.accessor.BasicCellInventoryAccessor;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIODriveDataExtension;
import io.github.yuko1101.appmekadjust.neoforge.qio.QIOStorageCellData;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import mekanism.common.attachments.qio.DriveContents;
import mekanism.common.content.qio.IQIODriveItem;
import mekanism.common.content.qio.QIODriveData;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.registries.MekanismItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QIODriveData.class)
public abstract class QIODriveDataMixin implements QIODriveDataExtension {
    @Mutable
    @Shadow @Final private QIODriveData.QIODriveKey key;

    @Shadow @Final private Object2LongMap<HashedItem> itemMap;

    @Unique
    private final Object2LongMap<AEKey> appMekAdjust$ae2ItemMap = new Object2LongOpenHashMap<>();

    @Unique private BasicCellInventory appMekAdjust$cellInventory;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private Item initWrap(ItemStack instance, Operation<Item> original) {
        if (!(key.getDriveStack().getItem() instanceof IBasicCellItem)) {
            return original.call(instance);
        }
        return MekanismItems.BASE_QIO_DRIVE.get();
    }
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lmekanism/common/content/qio/IQIODriveItem;getCountCapacity(Lnet/minecraft/world/item/ItemStack;)J"))
    private long initWrapCountCapacity(IQIODriveItem instance, ItemStack itemStack, Operation<Long> original) {
        if (!(key.getDriveStack().getItem() instanceof IBasicCellItem)) {
            return original.call(instance, itemStack);
        }
        return QIOStorageCellData.getItemCountCapacity(itemStack, (IBasicCellItem) itemStack.getItem(), 0);
    }
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lmekanism/common/content/qio/IQIODriveItem;getTypeCapacity(Lnet/minecraft/world/item/ItemStack;)I"))
    private int initWrapTypeCapacity(IQIODriveItem instance, ItemStack itemStack, Operation<Integer> original) {
        if (!(key.getDriveStack().getItem() instanceof IBasicCellItem)) {
            return original.call(instance, itemStack);
        }
        return QIOStorageCellData.getItemTypeCapacity(itemStack, (IBasicCellItem) itemStack.getItem());
    }

    @WrapWithCondition(method = "<init>", at = @At(value = "INVOKE", target = "Lmekanism/common/attachments/qio/DriveContents;loadItemMap(Lmekanism/common/content/qio/QIODriveData;)V"))
    private boolean init(DriveContents instance, QIODriveData entry) {
        if (!(key.getDriveStack().getItem() instanceof IBasicCellItem)) {
            appMekAdjust$cellInventory = null;
            return true;
        }

        ItemStack cellStack = key.getDriveStack();

        // TODO: make not to occur cast exception
        this.appMekAdjust$cellInventory = (BasicCellInventory) StorageCells.getCellInventory(cellStack, null);
        if (this.appMekAdjust$cellInventory == null) {
            throw new IllegalStateException("Cell inventory is null");
        }

        var items = ((BasicCellInventoryAccessor) this.appMekAdjust$cellInventory).invokeGetCellItems();
        for (var e : items.object2LongEntrySet()) {
            if (e.getKey() instanceof AEItemKey itemKey) {
                HashedItem hashedItem = HashedItem.create(itemKey.toStack());
                itemMap.put(hashedItem, e.getLongValue());
            } else {
                appMekAdjust$ae2ItemMap.put(e.getKey(), e.getLongValue());
            }
        }
        return false;
    }

    @Override
    public Object2LongMap<AEKey> appMekAdjust$getAE2ItemMap() {
        return appMekAdjust$ae2ItemMap;
    }

    @Override
    public StorageCell appMekAdjust$getCellInventory() {
        return appMekAdjust$cellInventory;
    }
}
