package com.github.kelemen.brazier.ui.jtable;

public interface ColumnDataGetter<RowData, ColumnData> {
    public ColumnData getColumnData(RowData data);
}
