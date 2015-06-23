package com.github.kelemen.hearthstone.emulator.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.function.IntSupplier;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import org.jtrim.utils.ExceptionHelper;

public final class JHorizontallyScrollablePanel extends JComponent implements Scrollable {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SCROLL_UNIT = 10;

    private final IntSupplier scrollUnit;

    public JHorizontallyScrollablePanel(LayoutManager layout) {
        this(layout, () -> DEFAULT_SCROLL_UNIT);
    }

    public JHorizontallyScrollablePanel() {
        this(() -> DEFAULT_SCROLL_UNIT);
    }

    public JHorizontallyScrollablePanel(LayoutManager layout, IntSupplier scrollUnit) {
        ExceptionHelper.checkNotNullArgument(scrollUnit, "scrollUnit");
        this.scrollUnit = scrollUnit;

        setLayout(layout);
    }

    public JHorizontallyScrollablePanel(IntSupplier scrollUnit) {
        ExceptionHelper.checkNotNullArgument(scrollUnit, "scrollUnit");
        this.scrollUnit = scrollUnit;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return scrollUnit.getAsInt();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return scrollUnit.getAsInt();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void paintComponent(Graphics g) {
        Color prevColor = g.getColor();

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(prevColor);
    }
}
