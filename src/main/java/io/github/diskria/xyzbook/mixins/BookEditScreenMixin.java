package io.github.diskria.xyzbook.mixins;

import io.github.diskria.xyzbook.XYZBook;
import io.github.diskria.xyzbook.extensions.BookSignScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {

    @Unique
    private boolean isXYZBook;

    @Shadow
    @Final
    private ItemStack book;

    @Shadow
    @Final
    private Player owner;

    @Shadow
    @Final
    private BookSignScreen signScreen;

    @Inject(
        method = "<init>",
        at = @At(value = "TAIL")
    )
    public void checkXYZBook(CallbackInfo ci) {
        isXYZBook = book != null && XYZBook.isXYZBook(book);
        if (isXYZBook && signScreen instanceof BookSignScreenExtension ext) {
            ResourceKey<Level> level = owner.level().dimension();
            ChatFormatting color;
            if (level == Level.OVERWORLD) {
                color = ChatFormatting.DARK_GREEN;
            } else if (level == Level.NETHER) {
                color = ChatFormatting.DARK_RED;
            } else if (level == Level.END) {
                color = ChatFormatting.DARK_PURPLE;
            } else {
                color = ChatFormatting.DARK_GRAY;
            }
            String xyz = (int) owner.getX() + " " + (int) owner.getY() + " " + (int) owner.getZ();
            ext.xyzbook$setCoordinates(Component.literal(color + xyz + ChatFormatting.RESET));
        }
    }

    @WrapOperation(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
            ordinal = 0
        )
    )
    public Button.Builder overrideSignButtonText(
        Component message, Button.OnPress onPress,
        Operation<Button.Builder> original
    ) {
        return original.call(
            isXYZBook ? Component.translatable("xyzbook.new_entry") : message,
            onPress
        );
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
        original.call(graphics, renderPipeline,
            isXYZBook ? XYZBook.GUI_TEXTURE : texture,
            x, y, u, v, width, height, textureWidth, textureHeight
        );
    }
}
