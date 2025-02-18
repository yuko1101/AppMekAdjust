package io.github.yuko1101.appmekadjust.neoforge.extension;

import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import mekanism.api.Action;
import mekanism.common.content.qio.QIODriveData;

import java.util.HashMap;

public interface QIOFrequencyExtension {
    HashMap<AEKey, HashMap<QIODriveData.QIODriveKey, Long>> appMekAdjust$getAE2ItemMap();
    long appMekAdjust$getAE2ItemCount();

    long appMekAdjust$massInsertAE2Items(AEKey key, long amount, Action action, IActionSource source);
    long appMekAdjust$massExtractAE2Items(AEKey key, long amount, Action action, IActionSource source);

    long appMekAdjust$getTotalItemCapacity();
    long appMekAdjust$getTotalTypeCapacity(boolean remote);

    void appMekAdjust$decreaseItemCapacity(long amount);
}
