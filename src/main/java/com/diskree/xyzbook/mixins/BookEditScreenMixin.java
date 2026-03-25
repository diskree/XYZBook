package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.BuildConfig;
import com.diskree.xyzbook.XYZBook;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.http.util.TextUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Unique
    private static final int MAX_ENTRY_NAME_LENGTH = 50;

    @Unique
    private static final String SEPARATOR = "-------------------";

    @Unique
    private static final Identifier XYZ_BOOK_TEXTURE = Identifier.of(BuildConfig.MOD_ID, "textures/gui/xyzbook.png");

    @Unique
    private ButtonWidget newEntryButton;

    @Unique
    private ButtonWidget addEntryButton;

    @Unique
    private boolean isXYZBook;

    @Unique
    private String coordinates;

    @Unique
    private String prepareEntry() {
        String current = getCurrentPageContent();
        String prefix = (current.isEmpty() || current.endsWith("\n")) ? "" : "\n";
        return prefix + title.trim() + "\n" + coordinates;
    }

    @Unique
    private void insertEntry() {
        String baseEntry = prepareEntry();
        String fullEntry = baseEntry + "\n" + SEPARATOR;
        currentPageSelectionManager.putCursorAtEnd();
        currentPageSelectionManager.insert(willFit(fullEntry) ? fullEntry : baseEntry);
        invalidatePageContent();
        finalizeBook(false);
    }

    @Unique
    private boolean willFit(String text) {
        return textRenderer.getWrappedLinesHeight(getCurrentPageContent() + text, MAX_TEXT_WIDTH) <= MAX_TEXT_HEIGHT;
    }

    protected BookEditScreenMixin() {
        super(null);
    }

    @Shadow
    @Final
    private ItemStack stack;

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
    @Final
    private static int MAX_TEXT_WIDTH;

    @Shadow
    @Final
    private static int MAX_TEXT_HEIGHT;

    @Mutable
    @Shadow
    @Final
    private SelectionManager bookTitleSelectionManager;

    @Inject(
        method = "<init>",
        at = @At(value = "TAIL")
    )
    public void checkXYZBook(CallbackInfo ci) {
        if (stack != null) {
            isXYZBook = XYZBook.isXYZBook(stack);
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

    @WrapOperation(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;updateButtons()V"
        )
    )
    public void initXYZButtons(BookEditScreen instance, Operation<Void> original) {
        if (isXYZBook) {
            newEntryButton = addDrawableChild(ButtonWidget.builder(Text.translatable("xyzbook.new_entry"), button -> {
                RegistryKey<World> dimension = player.getWorld().getRegistryKey();
                String dimensionColor;
                if (dimension == World.OVERWORLD) {
                    dimensionColor = "§2";
                } else if (dimension == World.NETHER) {
                    dimensionColor = "§4";
                } else {
                    dimensionColor = "§5";
                }
                coordinates = dimensionColor +
                    (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ() + "§r";
                signedByText = Text.literal(coordinates);
                signing = true;
                updateButtons();
            }).dimensions(width / 2 - 100, signButton.getY(), 98, 20).build());
            addEntryButton = addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
                if (signing) {
                    signing = false;
                    updateButtons();
                    insertEntry();
                    title = "";
                }
            }).dimensions(width / 2 - 100, finalizeButton.getY(), 98, 20).build());
        }
        original.call(instance);
    }

    @Inject(
        method = "updateButtons",
        at = @At(value = "TAIL")
    )
    public void updateXYZButtons(CallbackInfo ci) {
        if (!isXYZBook) return;

        signButton.visible = false;
        finalizeButton.visible = false;
        newEntryButton.visible = !signing;
        addEntryButton.visible = signing;

        if (signing) {
            boolean isTitleEmpty = TextUtils.isBlank(title);
            boolean fits = willFit(prepareEntry());
            addEntryButton.active = !isTitleEmpty && fits;
            if (isTitleEmpty || fits) {
                addEntryButton.setTooltip(null);
            } else {
                addEntryButton.setTooltip(Tooltip.of(
                    Text.translatable("xyzbook.no_more_space").formatted(Formatting.RED)
                ));
            }
        }
    }

    @WrapOperation(
        method = "renderBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V",
            ordinal = 0
        )
    )
    public void setCustomBackground(DrawContext instance, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
        original.call(instance, renderLayers,
            isXYZBook ? XYZ_BOOK_TEXTURE : sprite,
            x, y, u, v, width, height, textureWidth, textureHeight
        );
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I",
            ordinal = 0
        )
    )
    public int hideEditTitle(
        DrawContext context,
        TextRenderer textRenderer,
        Text text,
        int x,
        int y,
        int color,
        boolean shadow,
        Operation<Integer> original
    ) {
        return isXYZBook ? 0 : original.call(context, textRenderer, text, x, y, color, shadow);
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

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I"
        )
    )
    public int moveTitle(DrawContext instance, TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, Operation<Integer> original) {
        return original.call(instance, textRenderer, text, x,
            isXYZBook ? y - 16 : y,
            color, shadow
        );
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/OrderedText;styledForwardsVisitedString(Ljava/lang/String;Lnet/minecraft/text/Style;)Lnet/minecraft/text/OrderedText;"
        )
    )
    public OrderedText ellipsisTitle(String string, Style style, Operation<OrderedText> original) {
        int maxWidth = MAX_TEXT_WIDTH - 10;
        if (!isXYZBook || textRenderer.getWidth(title) < maxWidth) {
            return original.call(string, style);
        }
        return original.call(
            title.substring(title.length() - textRenderer.trimToWidth(title, maxWidth).length()),
            style
        );
    }

    @WrapOperation(
        method = "keyPressedSignMode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;finalizeBook(Z)V"
        )
    )
    public void disallowKeyInput(BookEditScreen instance, boolean signBook, Operation<Void> original) {
        if (!isXYZBook) {
            original.call(instance, signBook);
            return;
        }
        signing = false;
        updateButtons();
        insertEntry();
        title = "";
    }
}
