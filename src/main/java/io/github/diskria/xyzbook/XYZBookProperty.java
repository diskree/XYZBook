package io.github.diskria.xyzbook;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record XYZBookProperty() implements ConditionalItemModelProperty {

    public static final MapCodec<XYZBookProperty> CODEC = MapCodec.unit(new XYZBookProperty());

    @Override
    public boolean get(
        @NonNull ItemStack itemStack,
        @Nullable ClientLevel level,
        @Nullable LivingEntity owner,
        int seed,
        @NonNull ItemDisplayContext displayContext
    ) {
        return XYZBook.isXYZBook(itemStack);
    }

    @Override
    public @NonNull MapCodec<? extends ConditionalItemModelProperty> type() {
        return CODEC;
    }
}
