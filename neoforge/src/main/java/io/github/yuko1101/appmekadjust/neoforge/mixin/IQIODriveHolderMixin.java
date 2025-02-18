package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.storage.cells.IBasicCellItem;
import mekanism.common.content.qio.IQIODriveHolder;
import mekanism.common.content.qio.QIODriveData;
import mekanism.common.inventory.slot.QIODriveSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(IQIODriveHolder.class)
public interface IQIODriveHolderMixin {
    @Shadow List<QIODriveSlot> getDriveSlots();

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    default void save(int slot, QIODriveData data, CallbackInfo ci) {
        ItemStack stack = this.getDriveSlots().get(slot).getStack();
        if (stack.getItem() instanceof IBasicCellItem) {
            ci.cancel();
        }
    }
}
