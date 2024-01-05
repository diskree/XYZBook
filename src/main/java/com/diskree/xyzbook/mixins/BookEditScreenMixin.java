package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.XYZBook;
import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Unique
    private static final int MAX_ENTRY_NAME_LENGTH = 50;

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
    private RegistryKey<World> dimension;

    @Unique
    private void insertEntry(String entryName) {
        int lastNotEmptyPage = countPages() - 1;
        if (currentPage != lastNotEmptyPage) {
            currentPage = lastNotEmptyPage;
            updateButtons();
            changePage();
        }
        String textToAppend = entryName + ScreenTexts.LINE_BREAK.getString() + xyz + ScreenTexts.LINE_BREAK.getString() + getDimensionName() + ScreenTexts.LINE_BREAK.getString() + "-------------------" + ScreenTexts.LINE_BREAK.getString();
        if (textRenderer.getWrappedLinesHeight(getCurrentPageContent() + textToAppend, MAX_TEXT_WIDTH) > MAX_TEXT_HEIGHT) {
            openNextPage();
            if (currentPage == lastNotEmptyPage) {
                if (client != null) {
                    client.setScreen(null);
                }
                player.sendMessage(Text.translatable("xyzbook.no_more_space").formatted(Formatting.RED), true);
                return;
            }
        }
        currentPageSelectionManager.putCursorAtEnd();
        currentPageSelectionManager.insert(textToAppend);
        invalidatePageContent();
        finalizeBook(false);
    }

    @Unique
    @NotNull
    private String getDimensionName() {
        if (dimension == World.OVERWORLD) {
            return Text.translatable("flat_world_preset.minecraft.overworld").getString();
        } else if (dimension == World.NETHER) {
            return Text.translatable("advancements.nether.root.title").getString();
        } else if (dimension == World.END) {
            return Text.translatable("advancements.end.root.title").getString();
        }
        return dimension.getValue().toString();
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
    private static Text FINALIZE_WARNING_TEXT;

    @Mutable
    @Shadow
    @Final
    private Text signedByText;

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    @Final
    private SelectionManager currentPageSelectionManager;

    @Shadow
    protected abstract void finalizeBook(boolean signBook);

    @Shadow
    protected abstract void invalidatePageContent();

    @Shadow
    protected abstract String getCurrentPageContent();

    @Shadow
    private int currentPage;

    @Shadow
    protected abstract int countPages();

    @Shadow
    protected abstract void changePage();

    @Shadow
    @Final
    private static int MAX_TEXT_WIDTH;

    @Shadow
    @Final
    private static int MAX_TEXT_HEIGHT;

    @Shadow
    protected abstract void openNextPage();

    @Shadow
    private ButtonWidget doneButton;

    @Mutable
    @Shadow
    @Final
    private SelectionManager bookTitleSelectionManager;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    public void identifyCustomBook(CallbackInfo ci) {
        if (itemStack != null) {
            String name = itemStack.getName().getString();
            isXYZBook = name != null && name.toLowerCase().contains("xyz");
        }
        if (isXYZBook) {
            bookTitleSelectionManager = new SelectionManager(
                    bookTitleSelectionManager.stringGetter,
                    bookTitleSelectionManager.stringSetter,
                    bookTitleSelectionManager.clipboardGetter,
                    bookTitleSelectionManager.clipboardSetter,
                    (string) -> string.length() < MAX_ENTRY_NAME_LENGTH
            );
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;updateButtons()V"))
    public void initCustomButtons(CallbackInfo ci) {
        if (isXYZBook) {
            newEntryButton = addDrawableChild(ButtonWidget.builder(Text.translatable("xyzbook.new_entry"), button -> {
                xyz = (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ();
                dimension = player.getWorld().getRegistryKey();
                signedByText = Text.literal(xyz).formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
                FINALIZE_WARNING_TEXT = Text.translatable("xyzbook.new_entry.note");
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
            doneButton.setMessage(Text.translatable("mco.selectServer.close"));
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

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/OrderedText;styledForwardsVisitedString(Ljava/lang/String;Lnet/minecraft/text/Style;)Lnet/minecraft/text/OrderedText;"), index = 0)
    public String ellipsisTitle(String title) {
        if (isXYZBook) {
            int maxWidth = MAX_TEXT_WIDTH - 10;
            if (textRenderer.getWidth(title) >= maxWidth) {
                return title.substring(title.length() - textRenderer.trimToWidth(title, maxWidth).length());
            }
        }
        return title;
    }

    @Inject(method = "drawCursor", at = @At(value = "HEAD"), cancellable = true)
    public void hideCursor(DrawContext context, BookEditScreen.Position position, boolean atEnd, CallbackInfo ci) {
        if (isXYZBook) {
            ci.cancel();
        }
    }

    @Inject(method = "drawSelection", at = @At(value = "HEAD"), cancellable = true)
    public void hideSelection(DrawContext context, Rect2i[] selectionRectangles, CallbackInfo ci) {
        if (isXYZBook) {
            ci.cancel();
        }
    }

    @Inject(method = "selectCurrentWord", at = @At(value = "HEAD"), cancellable = true)
    public void disallowCurrentWordSelection(int cursor, CallbackInfo ci) {
        if (isXYZBook) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressedEditMode", at = @At(value = "HEAD"), cancellable = true)
    public void disallowKeyInput(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (isXYZBook) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/SharedConstants;isValidChar(C)Z"))
    public boolean disallowTyping(char chr) {
        return !isXYZBook && SharedConstants.isValidChar(chr);
    }
}
