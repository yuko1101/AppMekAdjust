package io.github.yuko1101.appmekadjust.neoforge.extension;

import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.StorageCell;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

public interface QIODriveDataExtension {
    Object2LongMap<AEKey> appMekAdjust$getAE2ItemMap();
    StorageCell appMekAdjust$getCellInventory();
}
