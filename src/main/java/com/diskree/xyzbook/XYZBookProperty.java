package com.diskree.xyzbook;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record XYZBookProperty() implements BooleanProperty {

    public static final MapCodec<XYZBookProperty> CODEC = MapCodec.unit(new XYZBookProperty());

    @Override
    public boolean test(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
        return XYZBook.isXYZBook(stack);
    }

    @Override
    public MapCodec<XYZBookProperty> getCodec() {
        return CODEC;
    }
}
