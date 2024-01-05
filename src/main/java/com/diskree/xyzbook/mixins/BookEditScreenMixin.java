package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.XYZBook;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Unique
    private static final Identifier XYZ_BOOK_TEXTURE = new Identifier(XYZBook.ID, "textures/gui/xyzbook.png");

    @Unique
    private ButtonWidget newEntryButton;

    @Unique
    private ButtonWidget newEntryDoneButton;

    @Unique
    private boolean isXYZBook;

    @Unique
    private String xyz;

    @Unique
    private void insertEntry(String entryName) {

    }

    protected BookEditScreenMixin() {
        super(null);
    }

    @Shadow
    @Final
    private ItemStack itemStack;

    @Shadow
    private boolean signing;

    @Shadow
    protected abstract void updateButtons();

    @Shadow
    private String title;

    @Shadow
    private ButtonWidget signButton;

    @Shadow
    private ButtonWidget finalizeButton;

    @Mutable
    @Shadow
    @Final
    private static Text EDIT_TITLE_TEXT;

    @Mutable
    @Shadow
    @Final
    private static Text FINALIZE_WARNING_TEXT;

    @Mutable
    @Shadow
    @Final
    private Text signedByText;

    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    public void identifyCustomBook(CallbackInfo ci) {
        if (itemStack != null) {
            String name = itemStack.getName().getString();
            if (name != null) {
                isXYZBook = name.toLowerCase().contains("xyz");
                if (isXYZBook) {
                    EDIT_TITLE_TEXT = Text.translatable("xyzbook.new_entry");
                    FINALIZE_WARNING_TEXT = Text.translatable("xyzbook.new_entry.note");
                    xyz = (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ();
                    signedByText = Text.of(xyz).copy().formatted(Formatting.DARK_GRAY).formatted(Formatting.ITALIC);
                }
            }
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;updateButtons()V"))
    public void initCustomButtons(CallbackInfo ci) {
        if (isXYZBook) {
            newEntryButton = addDrawableChild(ButtonWidget.builder(Text.translatable("xyzbook.new_entry"), button -> {
                signing = true;
                updateButtons();
            }).dimensions(width / 2 - 100, signButton.getY(), 98, 20).build());
            newEntryDoneButton = addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
                if (signing) {
                    signing = false;
                    updateButtons();
                    insertEntry(title.trim());
                    title = "";
                }
            }).dimensions(width / 2 - 100, finalizeButton.getY(), 98, 20).build());
        }
    }

    @Inject(method = "updateButtons", at = @At(value = "RETURN"))
    public void updateCustomButtons(CallbackInfo ci) {
        if (isXYZBook) {
            signButton.visible = false;
            finalizeButton.visible = false;
            newEntryButton.visible = !signing;
            newEntryDoneButton.visible = signing;
            newEntryDoneButton.active = !Util.isBlank(title);
        }
    }

    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V", ordinal = 0), index = 0)
    public Identifier setCustomBackground(Identifier originalValue) {
        return isXYZBook ? XYZ_BOOK_TEXTURE : originalValue;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", ordinal = 0))
    public int hideEditTitle(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
        return isXYZBook ? 0 : context.drawText(textRenderer, text, x, y, color, shadow);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I"), index = 3)
    public int moveTitle(int originalValue) {
        return isXYZBook ? originalValue - 16 : originalValue;
    }
}
