package com.diskree.xyzbook;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class XYZBook implements ClientModInitializer {

    public static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(BuildConfig.MOD_ID, "textures/gui/xyzbook.png");

    public static boolean isXYZBook(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        return customName != null && customName.getString().toLowerCase(Locale.ROOT).contains("xyz");
    }

    @Override
    public void onInitializeClient() {
        ConditionalItemModelProperties.ID_MAPPER.put(
            Identifier.fromNamespaceAndPath(BuildConfig.MOD_ID, "is_xyz"),
            XYZBookProperty.CODEC
        );
    }
}
