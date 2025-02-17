package io.github.yuko1101.appmekadjust.neoforge.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.yuko1101.appmekadjust.neoforge.extension.QIOFrequencyExtension;
import me.ramidzkh.mekae2.qio.QioStorageAdapter;
import mekanism.api.Action;
import mekanism.api.inventory.qio.IQIOFrequency;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QioStorageAdapter.class)
public abstract class QioStorageAdapterMixin {
    @Shadow public abstract @Nullable IQIOFrequency getFrequency();

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void onInsert(AEKey what, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> cir) {
        if (amount <= 0) {
            cir.setReturnValue(0L);
            return;
        }
        if (!(what instanceof AEItemKey)) {
            QIOFrequencyExtension freq = ((QIOFrequencyExtension) this.getFrequency());
            if (freq == null) {
                cir.setReturnValue(0L);
                return;
            }

            var inserted = freq.appMekAdjust$massInsertAE2Items(what, amount, Action.fromFluidAction(mode.getFluidAction()), source);
            cir.setReturnValue(inserted);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void onExtract(AEKey what, long amount, Actionable mode, IActionSource source, CallbackInfoReturnable<Long> cir) {
        if (amount <= 0) {
            cir.setReturnValue(0L);
            return;
        }
        if (!(what instanceof AEItemKey)) {
            QIOFrequencyExtension freq = ((QIOFrequencyExtension) this.getFrequency());
            if (freq == null) {
                cir.setReturnValue(0L);
                return;
            }

            cir.setReturnValue(freq.appMekAdjust$massExtractAE2Items(what, amount, Action.fromFluidAction(mode.getFluidAction()), source));
        }
    }
}
