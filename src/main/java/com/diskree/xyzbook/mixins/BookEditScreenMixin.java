package com.diskree.xyzbook.mixins;

import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BookEditScreen.class)
public class BookEditScreenMixin {

    @Shadow
    @Final
    private ItemStack itemStack;

    @Unique
    private boolean isXYZBook() {
        if (itemStack == null) {
            return false;
        }
        Text name = itemStack.getName();
        if (name == null) {
            return false;
        }
        return name.toString().toLowerCase().contains("xyz");
    }
}
