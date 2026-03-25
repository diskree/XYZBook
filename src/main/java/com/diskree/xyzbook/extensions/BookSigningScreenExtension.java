package com.diskree.xyzbook.extensions;

import net.minecraft.text.Text;

public interface BookSigningScreenExtension {
    boolean xyzbook$isXYZBook();
    void xyzbook$setCoordinates(Text coordinates);
    String xyzbook$prepareModifiedPage(String entryName);
}
