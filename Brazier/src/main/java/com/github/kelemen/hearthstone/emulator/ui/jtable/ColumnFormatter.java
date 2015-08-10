package com.github.kelemen.hearthstone.emulator.ui.jtable;

public interface ColumnFormatter<ColumnData> {
    public String format(ColumnData columnData);
}
