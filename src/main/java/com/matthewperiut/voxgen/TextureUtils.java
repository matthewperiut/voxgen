package com.matthewperiut.voxgen;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;

public class TextureUtils {

    public static int getPixelColor(Identifier identifier) {
        BlockState blockState = Registries.BLOCK.get(identifier).getDefaultState();

        // Get the map color for the block state
        MapColor mapColor = blockState.getMapColor(null, null);

        // Get the ARGB color value for the map color
        int color = mapColor.color;
        return color;
    }

    public static int getPixelColor(BlockState blockState)
    {
        // Get the map color for the block state
        MapColor mapColor = blockState.getMapColor(null, null);

        // Get the ARGB color value for the map color
        int color = mapColor.color;
        return color;
    }

    public static int abgrToRgb(int abgrColor) {
        int alpha = (abgrColor >> 24) & 0xFF;
        int blue = (abgrColor >> 16) & 0xFF;
        int green = (abgrColor >> 8) & 0xFF;
        int red = abgrColor & 0xFF;

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static void printRGBValues(FabricClientCommandSource source, int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        source.sendFeedback(Text.literal(new String("Red: " + red + ", Green: " + green + ", Blue: " + blue)));

    }

    public static Color abgrToColor(int abgr)
    {
        int red = (abgr >> 16) & 0xFF;
        int green = (abgr >> 8) & 0xFF;
        int blue = abgr & 0xFF;
        int alpha = (abgr >> 24) & 0xFF;

        Color color = new Color(red, green, blue, alpha);
        return color;
    }

    public static int rgbToRgba(int rgb)
    {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int alpha = (r == 0 && g == 0 && b == 0) ? 0 : 255; // Set alpha to 0 if r, g, and b are all zero

        int rgba = (alpha << 24) | (rgb & 0x00FFFFFF);

        return rgba;
    }

}
