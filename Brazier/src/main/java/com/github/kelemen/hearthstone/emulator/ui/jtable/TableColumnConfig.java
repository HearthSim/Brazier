package com.github.kelemen.hearthstone.emulator.ui.jtable;

import java.util.function.Consumer;
import javax.swing.table.TableColumn;
import org.jtrim.utils.ExceptionHelper;

public final class TableColumnConfig {
    private int minWidth;
    private int maxWidth;
    private int preferredWidth;
    private Consumer<? super TableColumn> customConfig;

    public TableColumnConfig() {
        this.minWidth = -1;
        this.maxWidth = -1;
        this.preferredWidth = -1;
        this.customConfig = (column) -> { };
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
    }

    public void setCustomConfig(Consumer<? super TableColumn> customConfig) {
        ExceptionHelper.checkNotNullArgument(customConfig, "customConfig");
        this.customConfig = customConfig;
    }

    public void setupColumn(TableColumn column) {
        ExceptionHelper.checkNotNullArgument(column, "column");

        if (minWidth >= 0) {
            column.setMinWidth(minWidth);
        }
        if (preferredWidth >= 0) {
            column.setPreferredWidth(preferredWidth);
        }
        if (maxWidth >= 0) {
            column.setMaxWidth(maxWidth);
        }

        customConfig.accept(column);
    }
}
