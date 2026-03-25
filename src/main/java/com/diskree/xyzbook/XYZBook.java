package com.diskree.xyzbook;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Locale;

public class XYZBook implements ClientModInitializer {

    public static final Identifier GUI_TEXTURE = Identifier.of(BuildConfig.MOD_ID, "textures/gui/xyzbook.png");

    public static boolean isXYZBook(ItemStack stack) {
        return stack.contains(DataComponentTypes.CUSTOM_NAME) &&
            stack.getName().getString().toLowerCase(Locale.ROOT).contains("xyz");
    }

    @Override
    public void onInitializeClient() {
        BooleanProperties.ID_MAPPER.put(
            Identifier.of(BuildConfig.MOD_ID, "is_xyz"),
            XYZBookProperty.CODEC
        );
    }
}
