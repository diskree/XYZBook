package com.diskree.xyzbook.extensions;

import net.minecraft.network.chat.Component;

public interface BookSignScreenExtension {
    boolean xyzbook$isXYZBook();
    void xyzbook$setCoordinates(Component coordinates);
    String xyzbook$buildModifiedPageWithEntry(String entryName);
}
