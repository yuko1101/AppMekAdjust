package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.storage.cells.IBasicCellItem;
import mekanism.common.inventory.slot.QIODriveSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QIODriveSlot.class)
public class QIODriveSlotMixin {
    @Inject(method = "lambda$static$0(Lnet/minecraft/world/item/ItemStack;)Z", at = @At(value = "RETURN"), cancellable = true)
    private static void onCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() || stack.getItem() instanceof IBasicCellItem);
    }
}
