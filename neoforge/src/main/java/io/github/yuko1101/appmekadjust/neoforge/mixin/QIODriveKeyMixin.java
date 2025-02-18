package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.storage.cells.ICellWorkbenchItem;
import mekanism.common.content.qio.QIODriveData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QIODriveData.QIODriveKey.class)
public abstract class QIODriveKeyMixin {
    @Shadow public abstract ItemStack getDriveStack();

    @Inject(method = "updateMetadata", at = @At("HEAD"), cancellable = true)
    private void updateMetadata(QIODriveData data, CallbackInfo ci) {
        if (this.getDriveStack().getItem() instanceof ICellWorkbenchItem) {
            ci.cancel();
        }
    }
}
