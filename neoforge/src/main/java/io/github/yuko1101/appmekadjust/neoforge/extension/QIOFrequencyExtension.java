package io.github.yuko1101.appmekadjust.neoforge.extension;

import appeng.api.stacks.AEKey;
import mekanism.common.content.qio.QIODriveData;

import java.util.HashMap;

public interface QIOFrequencyExtension {
    HashMap<AEKey, HashMap<QIODriveData.QIODriveKey, Long>> appMekAdjust$getAE2ItemMap();
}
