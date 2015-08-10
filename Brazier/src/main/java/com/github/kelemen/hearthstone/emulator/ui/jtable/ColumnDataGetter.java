package com.github.kelemen.hearthstone.emulator.ui.jtable;

public interface ColumnDataGetter<RowData, ColumnData> {
    public ColumnData getColumnData(RowData data);
}
