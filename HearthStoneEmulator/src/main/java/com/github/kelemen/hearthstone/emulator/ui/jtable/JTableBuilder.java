package com.github.kelemen.hearthstone.emulator.ui.jtable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.jtrim.utils.ExceptionHelper;

public final class JTableBuilder<RowData> {
    private final JTable table;

    private boolean needCheckboxes;
    private boolean sortableColumns;
    private boolean sortsOnUpdates;
    private int initialSortColumnIndex;
    private final List<ColumnInfo<RowData>> columnInfos;

    public JTableBuilder(JTable table) {
        ExceptionHelper.checkNotNullArgument(table, "table");

        this.table = table;
        this.needCheckboxes = false;
        this.sortableColumns = true;
        this.sortsOnUpdates = false;
        this.initialSortColumnIndex = -1;
        this.columnInfos = new LinkedList<>();
    }

    public List<ColumnDef<RowData, ?>> getColumnDefs() {
        List<ColumnDef<RowData, ?>> result = new ArrayList<>(columnInfos.size());
        columnInfos.forEach((element) -> {
            result.add(element.columnDef);
        });
        return result;
    }

    public JTable getTable() {
        return table;
    }

    public int getInitialSortColumnIndex() {
        return initialSortColumnIndex;
    }

    public void setInitialSortColumnIndex(int initialSortColumnIndex) {
        this.initialSortColumnIndex = initialSortColumnIndex;
    }

    public boolean isSortsOnUpdates() {
        return sortsOnUpdates;
    }

    public void setSortsOnUpdates(boolean sortsOnUpdates) {
        this.sortsOnUpdates = sortsOnUpdates;
    }

    public boolean isNeedCheckboxes() {
        return needCheckboxes;
    }

    public void setNeedCheckboxes(boolean needCheckboxes) {
        this.needCheckboxes = needCheckboxes;
    }

    public boolean isSortableColumns() {
        return sortableColumns;
    }

    public void setSortableColumns(boolean sortableColumns) {
        this.sortableColumns = sortableColumns;
    }

    public TableColumnConfig addColumnDef(ColumnDef<RowData, ?> columnDef) {
        ColumnInfo<RowData> columnInfo = new ColumnInfo<>(columnDef);
        columnInfos.add(columnInfo);
        return columnInfo.config;
    }

    public <ColumnData> TableColumnConfig addColumnDef(
            String caption,
            Comparator<ColumnData> dataComparer,
            ColumnFormatter<ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter) {

        return addColumnDef(new ColumnDef<>(caption, dataComparer, formatter, dataGetter));
    }

    public <ColumnData> TableColumnConfig addColumnDef(
            String caption,
            ColumnDisplayFormat<?, ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter) {

        return addColumnDef(new ColumnDef<>(caption, formatter, dataGetter));
    }

    public <ColumnData> TableColumnConfig addColumnDef(
            String caption,
            ColumnDisplayFormat<?, ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter,
            boolean editable) {

        return addColumnDef(new ColumnDef<>(caption, formatter, dataGetter, editable));
    }

    public TableColumnConfig addStringColumn(String caption, ColumnDataGetter<RowData, String> dataGetter) {
        return addColumnDef(ColumnDef.stringColumn(caption, dataGetter));
    }

    public TableColumnConfig addIntegerColumn(String caption, ColumnDataGetter<RowData, Integer> dataGetter) {
        return addColumnDef(caption, JTableUtils.nullFirstNaturalOrder(), JTableBuilder::intToStr, dataGetter);
    }

    public TableColumnConfig addLongColumn(String caption, ColumnDataGetter<RowData, Long> dataGetter) {
        return addColumnDef(caption, JTableUtils.nullFirstNaturalOrder(), JTableBuilder::longToStr, dataGetter);
    }

    private static String longToStr(Long value) {
        return value != null ? Long.toString(value) : JTableUtils.DEFAULT_NULL_VALUE;
    }

    private static String intToStr(Integer value) {
        return value != null ? Integer.toString(value) : JTableUtils.DEFAULT_NULL_VALUE;
    }

    public FormattedTableModel<RowData> setupTable() {
        FormattedTableModel<RowData> tableModel = new FormattedTableModel<>(needCheckboxes, getColumnDefs());

        table.setModel(tableModel);

        if (sortableColumns) {
            TableRowSorter<FormattedTableModel<RowData>> rowSorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(rowSorter);

            rowSorter.setSortsOnUpdates(sortsOnUpdates);
            if (initialSortColumnIndex >= 0 && initialSortColumnIndex < columnInfos.size()) {
                rowSorter.toggleSortOrder(initialSortColumnIndex);
            }
        }
        else {
            table.setRowSorter(null);
        }

        TableColumnModel columnModel = table.getColumnModel();

        int index = 0;
        for (ColumnInfo<RowData> columnInfo: columnInfos) {
            columnInfo.config.setupColumn(columnModel.getColumn(index));
            index++;
        }

        return tableModel;
    }

    private static final class ColumnInfo<RowData> {
        public final ColumnDef<RowData, ?> columnDef;
        public final TableColumnConfig config;

        public ColumnInfo(ColumnDef<RowData, ?> columnDef) {
            ExceptionHelper.checkNotNullArgument(columnDef, "columnDef");

            this.columnDef = columnDef;
            this.config = new TableColumnConfig();
        }
    }
}
