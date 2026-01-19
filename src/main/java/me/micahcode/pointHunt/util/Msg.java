package me.micahcode.pointHunt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Msg {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    public static Component c(String text) {
        return LEGACY.deserialize(text);
    }
}
