package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.XYZBook;
import com.diskree.xyzbook.extensions.BookSigningScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookSigningScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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
    private ItemStack stack;

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    @Final
    private BookSigningScreen signingScreen;

    @Inject(
        method = "<init>",
        at = @At(value = "TAIL")
    )
    public void checkXYZBook(CallbackInfo ci) {
        isXYZBook = stack != null && XYZBook.isXYZBook(stack);
        if (isXYZBook && signingScreen instanceof BookSigningScreenExtension bookSigningScreenExtension) {
            RegistryKey<World> dimension = player.getWorld().getRegistryKey();
            String dimensionColor;
            if (dimension == World.OVERWORLD) {
                dimensionColor = "§2";
            } else if (dimension == World.NETHER) {
                dimensionColor = "§4";
            } else {
                dimensionColor = "§5";
            }
            bookSigningScreenExtension.xyzbook$setCoordinates(
                Text.of(
                    dimensionColor + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ() + "§r"
                )
            );
        }
    }

    @ModifyExpressionValue(
        method = "init",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=book.signButton"
        )
    )
    public String overrideSignButtonText(String original) {
        return isXYZBook ? "xyzbook.new_entry" : original;
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
}
