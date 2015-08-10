package com.github.kelemen.hearthstone.emulator.ui.jtable;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

public final class FormattedTableModel<RowData> extends AbstractTableModel {
    private static final int DEFAULT_CHECKBOX_COLUMN_WIDTH = 25;

    private static final long serialVersionUID = 5663506605552575738L;

    private final ColumnDef<RowData, ?>[] columnDefs;
    private Row<RowData>[] rows;
    private boolean[] checkboxes;
    private final MutableProperty<Integer> checkedRowCount;

    public FormattedTableModel(List<ColumnDef<RowData, ?>> columnDefs) {
        this(false, columnDefs);
    }

    public FormattedTableModel(ColumnDef<RowData, ?>[] columnDefs) {
        this(false, columnDefs);
    }

    @SuppressWarnings("unchecked")
    public FormattedTableModel(boolean needCheckBox, List<ColumnDef<RowData, ?>> columnDefs) {
        this(needCheckBox, columnDefs.toArray((ColumnDef<RowData, ?>[])new ColumnDef<?, ?>[columnDefs.size()]));
    }

    public FormattedTableModel(boolean needCheckBox, ColumnDef<RowData, ?>[] columnDefs) {
        this.columnDefs = columnDefs.clone();
        this.rows = createRowArray(0);
        this.checkboxes = needCheckBox ? new boolean[0] : null;
        this.checkedRowCount = PropertyFactory.lazilySetProperty(PropertyFactory.memProperty(0));

        ExceptionHelper.checkNotNullElements(this.columnDefs, "columnDefs");
    }

    public static void setupCheckboxColumn(JTable table) {
        ExceptionHelper.checkNotNullArgument(table, "table");

        TableModel model = table.getModel();
        if (!(model instanceof FormattedTableModel)) {
            throw new IllegalArgumentException("JTable must have FormattedTableModel table model.");
        }

        if (((FormattedTableModel<?>)model).checkboxes == null) {
            throw new IllegalArgumentException("JTable must have checkbox columns.");
        }

        TableColumn checkBoxColumn = table.getColumnModel().getColumn(0);
        checkBoxColumn.setResizable(false);
        checkBoxColumn.setPreferredWidth(DEFAULT_CHECKBOX_COLUMN_WIDTH);
        checkBoxColumn.setMinWidth(DEFAULT_CHECKBOX_COLUMN_WIDTH);
        checkBoxColumn.setMaxWidth(DEFAULT_CHECKBOX_COLUMN_WIDTH);
    }

    public boolean hasCheckboxes() {
        return checkboxes != null;
    }

    public PropertySource<Integer> checkedRowCount() {
        return PropertyFactory.protectedView(checkedRowCount);
    }

    public PropertySource<Boolean> hasCheckedRow() {
        return PropertyFactory.convert(checkedRowCount, (count) -> count > 0);
    }

    @SuppressWarnings("unchecked")
    private static <RowData> Row<RowData>[] createRowArray(int length) {
        return (Row<RowData>[])new Row<?>[length];
    }

    private boolean isCheckBoxColumn(int displayColumnIndex) {
        return checkboxes != null && displayColumnIndex == 0;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (isCheckBoxColumn(columnIndex)) {
            return Boolean.class;
        }

        ColumnDef<?, ?> columnDef = columnDefs[getRealColumnIndex(columnIndex)];
        return columnDef.getDataComparer() != null
                ? Element.class
                : columnDef.getDisplayClass();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (isCheckBoxColumn(columnIndex)) {
            return true;
        }

        ColumnDef<?, ?> columnDef = columnDefs[getRealColumnIndex(columnIndex)];
        return columnDef.isColumnEditable();
    }

    private boolean nullSafeCast(Boolean value) {
        return value == null ? false : value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (isCheckBoxColumn(columnIndex)) {
            if (aValue instanceof Boolean) {
                setChecked(rowIndex, nullSafeCast((Boolean)aValue));
            }
            return;
        }

        Element<?> element = getRawValueAt(rowIndex, getRealColumnIndex(columnIndex));
        element.setValue(aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    private int getRealColumnIndex(int displayColumn) {
        return checkboxes == null ? displayColumn : displayColumn - 1;
    }

    @Override
    public String getColumnName(int column) {
        if (isCheckBoxColumn(column)) {
            return "";
        }

        return columnDefs[getRealColumnIndex(column)].getCaption();
    }

    @Override
    public int getRowCount() {
        return rows.length;
    }

    @Override
    public int getColumnCount() {
        return checkboxes == null
                ? columnDefs.length
                : columnDefs.length + 1;
    }

    private void verifyNonNullCheckboxes() {
        if (checkboxes == null) {
            throw new IllegalStateException("There are no checkboxes defined for this model.");
        }
    }

    public List<RowData> getRowsWithCheckState(boolean checkState) {
        verifyNonNullCheckboxes();

        List<RowData> result = new LinkedList<>();
        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i] == checkState) {
                result.add(rows[i].getRow());
            }
        }
        return result;
    }

    public List<RowData> getCheckedRows() {
        return getRowsWithCheckState(true);
    }

    public boolean isRowChecked(int rowIndex) {
        verifyNonNullCheckboxes();
        return checkboxes[rowIndex];
    }

    public void setChecked(int rowIndex, boolean checked) {
        verifyNonNullCheckboxes();

        if (checkboxes[rowIndex] == checked) {
            return;
        }

        checkboxes[rowIndex] = checked;
        checkedRowCount.setValue(checkedRowCount.getValue() + (checked ? 1 : -1));

        fireTableCellUpdated(rowIndex, 0);
    }

    public void setCheckedFor(boolean checked, Predicate<? super RowData> applyCondition) {
        ExceptionHelper.checkNotNullArgument(applyCondition, "applyCondition");

        setCheckedForAllImpl((prevChecked, row) -> {
            return applyCondition.test(row) ? checked : prevChecked;
        });
    }

    public void setCheckedForAll(boolean checked) {
        setCheckedForAllImpl((prevChecked, row) -> checked);
    }

    public void setCheckedForAll(Predicate<? super RowData> checkedValue) {
        ExceptionHelper.checkNotNullArgument(checkedValue, "checkedValue");

        setCheckedForAllImpl((prevChecked, row) -> checkedValue.test(row));
    }

    private void setCheckedForAllImpl(CheckedValueProvider<RowData> checkedValue) {
        verifyNonNullCheckboxes();
        ExceptionHelper.checkNotNullArgument(checkedValue, "checkedValue");

        int minChangeIndex = checkboxes.length;
        int maxChangeIndex = -1;
        int newCheckedRowCount = 0;
        for (int i = 0; i < checkboxes.length; i++) {
            boolean checked = checkedValue.getChecked(checkboxes[i], rows[i].row);
            if (checkboxes[i] != checked) {
                checkboxes[i] = checked;
                maxChangeIndex = i;
                if (minChangeIndex > i) minChangeIndex = i;
            }

            if (checked) {
                newCheckedRowCount++;
            }
        }

        checkedRowCount.setValue(newCheckedRowCount);

        if (maxChangeIndex >= minChangeIndex) {
            fireTableChanged(new TableModelEvent(this, minChangeIndex, maxChangeIndex, 0));
        }
    }

    public Element<?> getRawValueAt(int rowIndex, int columnIndex) {
        return rows[rowIndex].getElement(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (isCheckBoxColumn(columnIndex)) {
            return checkboxes[rowIndex];
        }

        Element<?> rawValue = getRawValueAt(rowIndex, getRealColumnIndex(columnIndex));
        if (getColumnClass(columnIndex) == Element.class) {
            return rawValue;
        }
        else {
            return rawValue.getValue();
        }
    }

    public int findRowIndex(Predicate<? super RowData> findCondition) {
        ExceptionHelper.checkNotNullArgument(findCondition, "findCondition");

        Row<RowData>[] currentRows = rows;
        for (int i = 0; i < currentRows.length; i++) {
            if (findCondition.test(currentRows[i].row)) {
                return i;
            }
        }
        return -1;
    }

    public void setRow(int rowIndex, RowData row) {
        rows[rowIndex].setRow(row);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public RowData getRow(int rowIndex) {
        return rows[rowIndex].getRow();
    }

    private void setRows(Row<RowData>[] newRows) {
        this.rows = newRows;

        if (checkboxes != null && checkboxes.length != newRows.length) {
            boolean[] newCheckBoxes = new boolean[newRows.length];
            System.arraycopy(checkboxes, 0, newCheckBoxes, 0, Math.min(newCheckBoxes.length, checkboxes.length));
            // TODO: Allow to have other default than false
            // Warning: If the default value is other than false, checkedRowCount must be updated.
            checkboxes = newCheckBoxes;
        }
    }

    public RowData removeRow(int rowIndex) {
        RowData result = rows[rowIndex].getRow();
        Row<RowData>[] newRows = createRowArray(rows.length - 1);

        int newRowsIndex = 0;
        for (int i = 0; i < rows.length; i++) {
            if (i != rowIndex) {
                newRows[newRowsIndex] = rows[i];
                newRowsIndex++;
            }
        }

        setRows(newRows);
        fireTableRowsDeleted(rowIndex, rowIndex);
        return result;
    }

    @SuppressWarnings("unchecked")
    public void setRows(List<? extends RowData> rows) {
        setRows((RowData[])rows.toArray());
    }

    public void setRows(RowData[] rows) {
        Row<RowData>[] newRows = createRowArray(rows.length);
        for (int i = 0; i < newRows.length; i++) {
            newRows[i] = new Row<>(rows[i], columnDefs);
        }

        setRows(newRows);
        fireTableDataChanged();
    }

    public void clear() {
        setRows(FormattedTableModel.<RowData>createRowArray(0));
        fireTableDataChanged();
    }

    private static <RowData, ColumnData> Element<?> getElementOfRow(
            RowData row,
            ColumnDef<RowData, ColumnData> def) {
        return new Element<>(row, def);
    }

    private static <RowData> Element<?>[] getElementsOfRow(
            RowData row,
            ColumnDef<RowData, ?>[] columnDefs) {

        Element<?>[] result = new Element<?>[columnDefs.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = getElementOfRow(row, columnDefs[i]);
        }
        return result;
    }

    private interface CheckedValueProvider<RowData> {
        public boolean getChecked(boolean prevValue, RowData row);
    }

    private static final class Row<RowData> {
        private final ColumnDef<RowData, ?>[] columnDefs;

        private RowData row;
        private Element<?>[] elements;

        public Row(RowData row, ColumnDef<RowData, ?>[] columnDefs) {
            assert columnDefs != null;

            this.columnDefs = columnDefs;
            this.row = row;
            this.elements = getElementsOfRow(row, columnDefs);
        }

        public RowData getRow() {
            return row;
        }

        public void setRow(RowData row) {
            this.row = row;
            this.elements = getElementsOfRow(row, columnDefs);
        }

        public Element<?> getElement(int index) {
            return elements[index];
        }
    }

    public static final class Element<ColumnData> implements Comparable<Element<ColumnData>> {
        private final ColumnData columnData;
        private Object value;
        private final Comparator<ColumnData> cmp;

        private <RowData> Element(RowData row, ColumnDef<RowData, ColumnData> columnDef) {
            this.columnData = columnDef.getColumnData(row);
            this.value = columnDef.formatColumn(columnData);
            this.cmp = columnDef.getDataComparer();
        }

        public ColumnData getData() {
            return columnData;
        }

        @Override
        public int compareTo(Element<ColumnData> o) {
            return cmp.compare(this.columnData, o.columnData);
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }


        @Override
        public String toString() {
            return value != null ? value.toString() : null;
        }
    }
}
