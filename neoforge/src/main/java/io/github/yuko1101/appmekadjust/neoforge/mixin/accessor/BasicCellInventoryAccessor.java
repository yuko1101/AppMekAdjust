package io.github.yuko1101.appmekadjust.neoforge.mixin.accessor;

import appeng.api.stacks.AEKey;
import appeng.me.cells.BasicCellInventory;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BasicCellInventory.class)
public interface BasicCellInventoryAccessor {
    @Invoker("getCellItems")
    Object2LongMap<AEKey> invokeGetCellItems();
}
