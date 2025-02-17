package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.ramidzkh.mekae2.qio.QioStorageAdapter;
import mekanism.api.Action;
import mekanism.api.inventory.qio.IQIOFrequency;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(QioStorageAdapter.class)
public abstract class QioStorageAdapterMixin {
    @Shadow public abstract @Nullable IQIOFrequency getFrequency();

    @WrapMethod(method = "insert")
    private long insert(AEKey what, long amount, Actionable mode, IActionSource source, Operation<Long> original) {
        if (amount <= 0L) {
            return 0L;
        }

        IQIOFrequency freq = this.getFrequency();
        if (freq == null) {
            return 0L;
        }

        AtomicLong remaining = new AtomicLong(amount);

        // try to insert into the storage cell in the QIO drive first
        HashMap<ItemStack, Long> notPreferredCells = new HashMap<>();

        HashMap<ItemStack, Long> extractedCells = new HashMap<>();
        ArrayList<ItemStack> modifiedCells = new ArrayList<>();

        freq.forAllStored((itemStack, value) -> {
            if (remaining.get() <= 0) return;
            if (itemStack.getItem() instanceof IBasicCellItem) {
                for (long i = 0; i < value; i++) {
                    var is = itemStack.copy();
                    if (remaining.get() <= 0) return;
                    StorageCell cellInv = StorageCells.getCellInventory(is, null);
                    if (cellInv == null) return;

                    // check if it's preferred only for the first cell in the stack
                    if (i == 0 && cellInv.extract(what, 1, Actionable.SIMULATE, source) == 0) {
                        notPreferredCells.put(itemStack, value);
                        return;
                    }

                    var inserted = cellInv.insert(what, remaining.get(), mode, source);
                    remaining.addAndGet(-inserted);
                    extractedCells.compute(itemStack, (key, val) -> val == null ? 1 : val + 1);
                    modifiedCells.add(is);
                }
            }
        });

        if (remaining.get() > 0) {
            for (var entry : notPreferredCells.entrySet()) cellLoop: {
                ItemStack itemStack = entry.getKey();
                long value = entry.getValue();
                for (long i = 0; i < value; i++) {
                    var is = entry.getKey().copy();
                    if (remaining.get() <= 0) break cellLoop;
                    StorageCell cellInv = StorageCells.getCellInventory(is, null);
                    if (cellInv == null) break;

                    var inserted = cellInv.insert(what, remaining.get(), mode, source);
                    remaining.addAndGet(-inserted);
                    extractedCells.compute(itemStack, (key, val) -> val == null ? 1 : val + 1);
                    modifiedCells.add(is);
                }
            }
        }

        for (var entry : extractedCells.entrySet()) {
            freq.massExtract(entry.getKey(), entry.getValue(), Action.fromFluidAction(mode.getFluidAction()));
        }

        for (var is : modifiedCells) {
            freq.massInsert(is, 1, Action.fromFluidAction(mode.getFluidAction()));
        }

        if (remaining.get() > 0) {
            remaining.addAndGet(-original.call(what, remaining.get(), mode, source));
        }

        return amount - remaining.get();
    }

    @WrapMethod(method = "extract")
    private long extract(AEKey what, long amount, Actionable mode, IActionSource source, Operation<Long> original) {
        if (amount <= 0L) {
            return 0L;
        }

        IQIOFrequency freq = this.getFrequency();
        if (freq == null) {
            return 0L;
        }

        AtomicLong remaining = new AtomicLong(amount);

        // try to extract from the QIO drive itself first
        remaining.addAndGet(-original.call(what, remaining.get(), mode, source));
        if (remaining.get() <= 0) return amount;

        HashMap<ItemStack, Long> extractedCells = new HashMap<>();
        ArrayList<ItemStack> modifiedCells = new ArrayList<>();

        freq.forAllStored((itemStack, value) -> {
            if (remaining.get() <= 0) return;
            if (itemStack.getItem() instanceof IBasicCellItem) {
                for (long i = 0; i < value; i++) {
                    var is = itemStack.copy();
                    if (remaining.get() <= 0) return;
                    StorageCell cellInv = StorageCells.getCellInventory(is, null);
                    if (cellInv == null) return;

                    var extracted = cellInv.extract(what, remaining.get(), mode, source);
                    remaining.addAndGet(-extracted);
                    extractedCells.compute(itemStack, (key, val) -> val == null ? 1 : val + 1);
                    modifiedCells.add(is);
                }
            }
        });

        for (var entry : extractedCells.entrySet()) {
            freq.massExtract(entry.getKey(), entry.getValue(), Action.fromFluidAction(mode.getFluidAction()));
        }

        for (var is : modifiedCells) {
            freq.massInsert(is, 1, Action.fromFluidAction(mode.getFluidAction()));
        }

        return amount - remaining.get();
    }
}
