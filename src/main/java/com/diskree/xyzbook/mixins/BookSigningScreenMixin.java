package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.XYZBook;
import com.diskree.xyzbook.extensions.BookSigningScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookSigningScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(BookSigningScreen.class)
public abstract class BookSigningScreenMixin implements BookSigningScreenExtension {

    @Unique
    private static final int MAX_ENTRY_NAME_LENGTH = 50;

    @Unique
    private boolean isXYZBook;

    @Unique
    private static final String SEPARATOR = "-------------------";

    @Unique
    public String getCurrentPageText() {
        return pages.get(editScreen.currentPage);
    }

    @Override
    public boolean xyzbook$isXYZBook() {
        return isXYZBook;
    }

    @Override
    public String xyzbook$prepareModifiedPage(String entryName) {
        String currentPageText = getCurrentPageText();
        String prefix = (currentPageText.isEmpty() || currentPageText.endsWith("\n")) ? "" : "\n";
        return currentPageText + prefix + entryName.trim() + "\n" + bylineText.getString();
    }

    @Override
    public void xyzbook$setCoordinates(Text coordinates) {
        isXYZBook = true;
        bylineText = coordinates;
    }

    @Mutable
    @Shadow
    @Final
    private Text bylineText;

    @Shadow
    @Final
    private BookEditScreen editScreen;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    private TextFieldWidget bookTitleTextField;

    @WrapOperation(
        method = "method_71542",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/widget/ButtonWidget;active:Z",
            ordinal = 0,
            opcode = Opcodes.PUTFIELD
        )
    )
    private static void overrideActiveCondition(ButtonWidget signButton, boolean isTitleNotEmpty, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) String bookTitle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof BookSigningScreenExtension bookSigningScreenExtension && bookSigningScreenExtension.xyzbook$isXYZBook()) {
            boolean canFit = bookSigningScreenExtension.xyzbook$prepareModifiedPage(bookTitle).length()
                <= WritableBookContentComponent.MAX_PAGE_LENGTH;
            original.call(signButton, isTitleNotEmpty && canFit);
            if (isTitleNotEmpty && !canFit) {
                signButton.setTooltip(Tooltip.of(Text.translatable("xyzbook.no_more_space").formatted(Formatting.RED)));
            } else {
                signButton.setTooltip(null);
            }
        } else {
            original.call(signButton, isTitleNotEmpty);
        }
    }

    @ModifyExpressionValue(
        method = "init",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=book.finalizeButton"
        )
    )
    public String overrideSignButtonText(String original) {
        return isXYZBook ? "gui.done" : original;
    }

    @ModifyExpressionValue(
        method = "init",
        at = @At(
            value = "CONSTANT",
            args = "intValue=50"
        )
    )
    public int moveTitle(int original) {
        return isXYZBook ? original - 16 : original;
    }

    @ModifyExpressionValue(
        method = "init",
        at = @At(
            value = "CONSTANT",
            args = "intValue=15"
        )
    )
    public int overrideTitleLengthLimit(int original) {
        return isXYZBook ? MAX_ENTRY_NAME_LENGTH : original;
    }

    @WrapOperation(
        method = "renderBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V",
            ordinal = 0
        )
    )
    public void setCustomBackground(DrawContext instance, RenderPipeline pipeline, Identifier sprite, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
        original.call(instance, pipeline,
            isXYZBook ? XYZBook.GUI_TEXTURE : sprite,
            x, y, u, v, width, height, textureWidth, textureHeight
        );
    }

    @WrapWithCondition(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawWrappedText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/StringVisitable;IIIIZ)V"
        )
    )
    public boolean hideFinalizeText(
        DrawContext instance, TextRenderer textRenderer, StringVisitable text, int x, int y, int width, int color, boolean shadow
    ) {
        return !isXYZBook;
    }

    @WrapWithCondition(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V",
            ordinal = 0
        )
    )
    public boolean hideEditTitle(
        DrawContext instance, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow
    ) {
        return !isXYZBook;
    }

    @WrapOperation(
        method = "onFinalize",
        at = @At(
            value = "NEW",
            target = "(ILjava/util/List;Ljava/util/Optional;)Lnet/minecraft/network/packet/c2s/play/BookUpdateC2SPacket;"
        )
    )
    public BookUpdateC2SPacket overrideSignLogic(int slot, List<String> pages, Optional<String> title, Operation<BookUpdateC2SPacket> original) {
        if (isXYZBook) {
            String modifiedPageWithEntryBase = xyzbook$prepareModifiedPage(bookTitleTextField.getText());
            String modifiedPageWithFullEntry = modifiedPageWithEntryBase + "\n" + SEPARATOR;
            if (modifiedPageWithFullEntry.length() <= WritableBookContentComponent.MAX_PAGE_LENGTH) {
                pages.set(editScreen.currentPage, modifiedPageWithFullEntry);
            } else if (modifiedPageWithEntryBase.length() <= WritableBookContentComponent.MAX_PAGE_LENGTH) {
                pages.set(editScreen.currentPage, modifiedPageWithEntryBase);
            }
        }
        return original.call(slot, pages, isXYZBook ? Optional.<String>empty() : title);
    }

    @WrapOperation(
        method = {"keyPressed", "method_71543"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
            ordinal = 0
        )
    )
    public void backToEditScreen(MinecraftClient instance, Screen screen, Operation<Void> original) {
        original.call(instance, isXYZBook ? editScreen : screen);
    }
}
