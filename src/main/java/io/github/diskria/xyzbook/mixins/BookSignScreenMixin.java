package io.github.diskria.xyzbook.mixins;

import io.github.diskria.xyzbook.XYZBook;
import io.github.diskria.xyzbook.extensions.BookSignScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.WritableBookContent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(BookSignScreen.class)
public abstract class BookSignScreenMixin implements BookSignScreenExtension {

    @Unique
    private static final int MAX_ENTRY_NAME_LENGTH = 50;

    @Unique
    private static final String SEPARATOR = "-------------------";

    @Unique
    private boolean isXYZBook;

    @Unique
    private static boolean canFitToPage(String content) {
        return content.length() <= WritableBookContent.PAGE_EDIT_LENGTH;
    }

    @Override
    public boolean xyzbook$isXYZBook() {
        return isXYZBook;
    }

    @Override
    public String xyzbook$buildModifiedPageWithEntry(String entryName) {
        String currentPageText = pages.get(bookEditScreen.currentPage);
        String newLine = (!currentPageText.isEmpty() && !currentPageText.endsWith("\n")) ? "\n" : "";
        return currentPageText + newLine + entryName.trim() + "\n" + ownerText.getString();
    }

    @Override
    public void xyzbook$setCoordinates(Component coordinates) {
        isXYZBook = true;
        ownerText = coordinates;
    }

    @Mutable
    @Shadow
    @Final
    private Component ownerText;

    @Shadow
    @Final
    private BookEditScreen bookEditScreen;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    private EditBox titleBox;

    @Shadow private String titleValue;

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
        method = "extractBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
            ordinal = 0
        )
    )
    public void setCustomBackground(
        GuiGraphicsExtractor graphics, RenderPipeline renderPipeline, Identifier texture,
        int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight,
        Operation<Void> original
    ) {
        original.call(
            graphics, renderPipeline,
            isXYZBook ? XYZBook.GUI_TEXTURE : texture,
            x, y, u, v, width, height, textureWidth, textureHeight
        );
    }

    @WrapWithCondition(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V"
        )
    )
    public boolean hideFinalizeText(
        GuiGraphicsExtractor graphics, Font font, FormattedText string,
        int x, int y, int width, int col, boolean dropShadow
    ) {
        return !isXYZBook;
    }

    @WrapWithCondition(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 0
        )
    )
    public boolean hideEditTitle(
        GuiGraphicsExtractor graphics, Font font, Component str,
        int x, int y, int color, boolean dropShadow
    ) {
        return !isXYZBook;
    }

    @WrapOperation(
        method = {"keyPressed", "lambda$init$0"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            ordinal = 0
        )
    )
    public void backToEditScreen(Gui instance, Screen screen, Operation<Void> original) {
        if (isXYZBook) {
            titleValue = "";
        }
        original.call(instance, isXYZBook ? bookEditScreen : screen);
    }

    @WrapOperation(
        method = "saveChanges",
        at = @At(
            value = "NEW",
            target = "(ILjava/util/List;Ljava/util/Optional;)Lnet/minecraft/network/protocol/game/ServerboundEditBookPacket;"
        )
    )
    public ServerboundEditBookPacket overrideSignLogic(
        int slot,
        List<String> pages,
        Optional<String> title,
        Operation<ServerboundEditBookPacket> original
    ) {
        if (isXYZBook) {
            String modifiedPageWithEntryBase = xyzbook$buildModifiedPageWithEntry(titleBox.getValue());
            String modifiedPageWithEntrySeparator = modifiedPageWithEntryBase + "\n" + SEPARATOR;
            if (canFitToPage(modifiedPageWithEntrySeparator)) {
                pages.set(bookEditScreen.currentPage, modifiedPageWithEntrySeparator);
            } else if (canFitToPage(modifiedPageWithEntryBase)) {
                pages.set(bookEditScreen.currentPage, modifiedPageWithEntryBase);
            }
        }
        return original.call(slot, pages, isXYZBook ? Optional.<String>empty() : title);
    }

    @WrapOperation(
        method = "lambda$init$1",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/Button;active:Z",
            ordinal = 0,
            opcode = Opcodes.PUTFIELD
        )
    )
    private static void overrideDoneButtonActiveCondition(
        Button signButton,
        boolean isTitleNotEmpty,
        Operation<Void> original,
        @Local(argsOnly = true, name = "value") String value
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof BookSignScreenExtension ext && ext.xyzbook$isXYZBook()) {
            boolean canFit = canFitToPage(ext.xyzbook$buildModifiedPageWithEntry(value));
            original.call(signButton, isTitleNotEmpty && canFit);
            if (isTitleNotEmpty && !canFit) {
                signButton.setTooltip(Tooltip.create(
                    Component.translatable("xyzbook.no_more_space").withStyle(ChatFormatting.RED)
                ));
            } else {
                signButton.setTooltip(null);
            }
        } else {
            original.call(signButton, isTitleNotEmpty);
        }
    }
}
