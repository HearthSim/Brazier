package com.github.kelemen.hearthstone.emulator.ui.jtable;

public final class TableFormatters {
    private static final String NULL_STR = "";

    public static String formatString(String columnData) {
        return columnData != null ? columnData : NULL_STR;
    }

    public static String formatInt(Integer columnData) {
        return columnData != null ? columnData.toString() : NULL_STR;
    }

    private TableFormatters() {
        throw new AssertionError();
    }
}
