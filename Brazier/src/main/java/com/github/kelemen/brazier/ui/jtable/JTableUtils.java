package com.github.kelemen.brazier.ui.jtable;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Comparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

public final class JTableUtils {
    public static final String DEFAULT_NULL_VALUE = "";
    public static final String DEFAULT_UNKNOWN_DATE = "?";

    private static ListenerRef toListenerRef(Runnable unregisterTask) {
        Runnable safeUnregisterTask = Tasks.runOnceTask(unregisterTask, false);
        return new ListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                safeUnregisterTask.run();
                registered = false;
            }
        };
    }

    public static ListenerRef addSelectionChangeListener(JTable table, Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        ListSelectionModel model = table.getSelectionModel();

        ListSelectionListener selectionListener = (ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            listener.run();
        };

        model.addListSelectionListener(selectionListener);

        return toListenerRef(() -> model.removeListSelectionListener(selectionListener));
    }

    public static int getModelRowAtPoint(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        return rowToModelIndex(table, row);
    }

    public static int getSelectedModelIndex(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return -1;
        }

        return rowToModelIndex(table, selectedRow);
    }

    public static void scrollToLine(JTable table, int lineIndex) {
        Rectangle rect = table.getCellRect(lineIndex, lineIndex, true);
        table.scrollRectToVisible(rect);
    }

    public static int modelIndexToDisplayIndex(JTable table, int modelIndex) {
        if (modelIndex >= 0) {
            RowSorter<?> rowSorter = table.getRowSorter();
            return rowSorter != null
                    ? rowSorter.convertRowIndexToView(modelIndex)
                    : modelIndex;
        }
        return -1;
    }

    public static int rowToModelIndex(JTable table, int row) {
        if (row >= 0) {
            RowSorter<?> rowSorter = table.getRowSorter();
            return rowSorter != null
                    ? rowSorter.convertRowIndexToModel(row)
                    : row;
        }
        return -1;
    }

    public static ListenerRef addClickListener(JTable table, RowClickListener listener, int minClick, int maxClick) {
        ExceptionHelper.checkNotNullArgument(table, "table");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final MouseListener addedListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount >= minClick && clickCount <= maxClick) {
                    int row = getModelRowAtPoint(table, e.getPoint());
                    if (row >= 0) {
                        listener.onClickRow(row);
                    }
                }
            }
        };

        table.addMouseListener(addedListener);
        return toListenerRef(() -> {
            table.removeMouseListener(addedListener);
        });
    }

    public static ListenerRef addSingleClickListener(JTable table, RowClickListener listener) {
        return addClickListener(table, listener, 1, 1);
    }

    public static ListenerRef addDoubleClickListener(JTable table, RowClickListener listener) {
        return addClickListener(table, listener, 2, Integer.MAX_VALUE);
    }

    public static <T> Comparator<T> nullSafeNaturalOrder(boolean nullFirst) {
        return nullFirst ? nullFirstNaturalOrder() : nullLastNaturalOrder();
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullFirstNaturalOrder() {
        return (Comparator<T>)NullSafeNaturalComparator.NULL_FIRST;
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullLastNaturalOrder() {
        return (Comparator<T>)NullSafeNaturalComparator.NULL_LAST;
    }

    private enum NullSafeNaturalComparator implements Comparator<Comparable<Object>> {
        NULL_FIRST(-1, 1),
        NULL_LAST(1, -1);

        private final int firstNullResult;
        private final int secondNullResult;

        private NullSafeNaturalComparator(int firstNullResult, int secondNullResult) {
            this.firstNullResult = firstNullResult;
            this.secondNullResult = secondNullResult;
        }

        @Override
        public int compare(Comparable<Object> o1, Comparable<Object> o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return firstNullResult;
            }
            if (o2 == null) {
                return secondNullResult;
            }

            return o1.compareTo(o2);
        }
    }

    private JTableUtils() {
        throw new AssertionError();
    }
}
