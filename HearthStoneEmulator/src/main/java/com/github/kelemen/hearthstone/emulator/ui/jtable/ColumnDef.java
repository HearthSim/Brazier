package com.github.kelemen.hearthstone.emulator.ui.jtable;

import java.util.Comparator;
import org.jtrim.utils.ExceptionHelper;

public final class ColumnDef<RowData, ColumnData> {
    private final String caption;
    private final Comparator<ColumnData> dataComparer;
    private final ColumnDisplayFormat<?, ColumnData> formatter;
    private final ColumnDataGetter<RowData, ColumnData> dataGetter;
    private final Class<?> displayDataClass;
    private final boolean editable;

    public ColumnDef(
            String caption,
            Comparator<ColumnData> dataComparer,
            ColumnFormatter<ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter) {
        this(caption, dataComparer, new StringFormat<>(formatter), dataGetter, false);

        ExceptionHelper.checkNotNullArgument(dataComparer, "dataComparer");
    }

    public ColumnDef(
            String caption,
            ColumnDisplayFormat<?, ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter) {
        this(caption, null, formatter, dataGetter, false);
    }

     public ColumnDef(
            String caption,
            ColumnDisplayFormat<?, ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter,
            boolean editable) {
        this(caption, null, formatter, dataGetter, editable);
    }

    private ColumnDef(
            String caption,
            Comparator<ColumnData> dataComparer,
            ColumnDisplayFormat<?, ColumnData> formatter,
            ColumnDataGetter<RowData, ColumnData> dataGetter,
            boolean editable) {
        ExceptionHelper.checkNotNullArgument(caption, "caption");
        ExceptionHelper.checkNotNullArgument(formatter, "formatter");
        ExceptionHelper.checkNotNullArgument(dataGetter, "dataGetter");

        this.displayDataClass = formatter.getDisplayDataClass();
        ExceptionHelper.checkNotNullArgument(displayDataClass, "formatter.getDisplayDataClass()");

        this.caption = caption;
        this.dataComparer = dataComparer;
        this.formatter = formatter;
        this.dataGetter = dataGetter;
        this.editable = editable;
    }

    public static <RowData> ColumnDef<RowData, String> stringColumn(
            String caption,
            ColumnDataGetter<RowData, String> dataGetter) {
        return new ColumnDef<>(caption, StringComparator.DEFAULT_INSTANCE, TableFormatters::formatString, dataGetter);
    }

    public static <RowData> ColumnDef<RowData, Integer> integerColumn(
            String caption,
            ColumnDataGetter<RowData, Integer> dataGetter) {
        return new ColumnDef<>(caption, JTableUtils.nullFirstNaturalOrder(), ColumnDef::intToStr, dataGetter);
    }

    public static <RowData> ColumnDef<RowData, Long> longColumn(
            String caption,
            ColumnDataGetter<RowData, Long> dataGetter) {
        return new ColumnDef<>(caption, JTableUtils.nullFirstNaturalOrder(), ColumnDef::longToStr, dataGetter);
    }

    private static String longToStr(Long value) {
        return value != null ? Long.toString(value) : JTableUtils.DEFAULT_NULL_VALUE;
    }

    private static String intToStr(Integer value) {
        return value != null ? Integer.toString(value) : JTableUtils.DEFAULT_NULL_VALUE;
    }

    public String getCaption() {
        return caption;
    }

    // !!! Can be null if ColumnDisplayFormat is specified instead of ColumnFormatter.
    public Comparator<ColumnData> getDataComparer() {
        return dataComparer;
    }

    public ColumnDisplayFormat<?, ColumnData> getFormatter() {
        return formatter;
    }

    public ColumnDataGetter<RowData, ColumnData> getDataGetter() {
        return dataGetter;
    }

    public ColumnData getColumnData(RowData data) {
        return dataGetter.getColumnData(data);
    }

    public Class<?> getDisplayClass() {
        return displayDataClass;
    }

    public Object formatColumn(ColumnData columnData) {
        return formatter.getDisplayData(columnData);
    }

    public boolean isColumnEditable() {
        return editable;
    }

    private static final class StringFormat<ColumnData> implements ColumnDisplayFormat<String, ColumnData> {
        private final ColumnFormatter<ColumnData> formatter;

        public StringFormat(ColumnFormatter<ColumnData> formatter) {
            ExceptionHelper.checkNotNullArgument(formatter, "formatter");
            this.formatter = formatter;
        }

        @Override
        public Class<String> getDisplayDataClass() {
            return String.class;
        }

        @Override
        public String getDisplayData(ColumnData data) {
            return formatter.format(data);
        }
    }


}
