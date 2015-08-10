package com.github.kelemen.hearthstone.emulator.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

/**
 * A {@code LayoutManager} ordering the children in rows of a fixed maximum
 * width. The children components size is set to their preferred size and in all
 * row they are aligned by their vertical center. If the parent component has
 * enough height (width doesn't matter) to display all the rows then the
 * components are moved to the center of the parent component (vertically).
 * However, rows always start at the zero x coordinate.
 * <P>
 * The preferred width reported by this layout manager includes the specified
 * gap both at the start and at the end of the component list.
 * <P>
 * This layout manager is particularly useful for displaying thumbnail icons.
 *
 */
public final class SerialLayoutManager implements LayoutManager {
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    private final int componentGap;
    private final int maxWidth;
    private final boolean sameSizeComponents;
    private final Alignment align;

    /**
     * Creates a new {@code SerialLayoutManager} with the given gap between the
     * components. The maximum width of a row is 30000.
     *
     * @param componentGap the gap in pixels between components. This defines
     *   both vertical and horizontal gap. This argument must be greater than or
     *   equal to zero.
     */
    public SerialLayoutManager(int componentGap) {
        this(componentGap, 30000);
    }

    /**
     * Creates a new {@code SerialLayoutManager} with the given gap between the
     * components. The maximum width of a row is 30000.
     *
     * @param componentGap the gap in pixels between components. This defines
     *   both vertical and horizontal gap. This argument must be greater than or
     *   equal to zero.
     * @param sameSizeComponents {@code true} if the components are set to the
     *   same size as the largest component. This is sometimes useful for buttons.
     *   If {@code false}, then components are simply set to their preferred size.
     */
    public SerialLayoutManager(int componentGap, boolean sameSizeComponents) {
        this(componentGap, 30000, sameSizeComponents);
    }

    /**
     * Creates a new {@code SerialLayoutManager} with the given gap between the
     * components. The maximum width of a row is 30000.
     *
     * @param componentGap the gap in pixels between components. This defines
     *   both vertical and horizontal gap. This argument must be greater than or
     *   equal to zero.
     * @param sameSizeComponents {@code true} if the components are set to the
     *   same size as the largest component. This is sometimes useful for buttons.
     *   If {@code false}, then components are simply set to their preferred size.
     * @param align an enum defining if the components are to be aligned to the left
     *   or right. This argument cannot be {@code null}.
     */
    public SerialLayoutManager(int componentGap, boolean sameSizeComponents, Alignment align) {
        this(componentGap, 30000, sameSizeComponents, align);
    }

    /**
     * Creates a new {@code SerialLayoutManager} with the given gap between the
     * components and the specified maximum width of a row.
     *
     * @param componentGap the gap in pixels between components. This defines
     *   both vertical and horizontal gap. This argument must be greater than or
     *   equal to zero.
     * @param maxWidth the maximum width of a row in pixels after a new row
     *   of components must be started. This argument must be greater than or
     *   equal to zero and cannot be larger than {@code Short.MAX_VALUE}.
     */
    public SerialLayoutManager(int componentGap, int maxWidth) {
        this(componentGap, maxWidth, false);
    }

    /**
     * Creates a new {@code SerialLayoutManager} with the given gap between the
     * components and the specified maximum width of a row.
     *
     * @param componentGap the gap in pixels between components. This defines
     *   both vertical and horizontal gap. This argument must be greater than or
     *   equal to zero.
     * @param maxWidth the maximum width of a row in pixels after a new row
     *   of components must be started. This argument must be greater than or
     *   equal to zero and cannot be larger than {@code Short.MAX_VALUE}.
     * @param sameSizeComponents {@code true} if the components are set to the
     *   same size as the largest component. This is sometimes useful for buttons.
     *   If {@code false}, then components are simply set to their preferred size.
     */
    public SerialLayoutManager(int componentGap, int maxWidth, boolean sameSizeComponents) {
        this(componentGap, maxWidth, sameSizeComponents, Alignment.LEFT);
    }

    /**
     * Creates a new {@code SerialLayoutManager} with the given gap between the
     * components and the specified maximum width of a row.
     *
     * @param componentGap the gap in pixels between components. This defines
     *   both vertical and horizontal gap. This argument must be greater than or
     *   equal to zero.
     * @param maxWidth the maximum width of a row in pixels after a new row
     *   of components must be started. This argument must be greater than or
     *   equal to zero and cannot be larger than {@code Short.MAX_VALUE}.
     * @param sameSizeComponents {@code true} if the components are set to the
     *   same size as the largest component. This is sometimes useful for buttons.
     *   If {@code false}, then components are simply set to their preferred size.
     * @param align an enum defining if the components are to be aligned to the left
     *   or right. This argument cannot be {@code null}.
     */
    public SerialLayoutManager(int componentGap, int maxWidth, boolean sameSizeComponents, Alignment align) {
        ExceptionHelper.checkArgumentInRange(componentGap, 0, Integer.MAX_VALUE, "componentGap");
        ExceptionHelper.checkArgumentInRange(maxWidth, 0, Short.MAX_VALUE, "maxWidth");
        ExceptionHelper.checkNotNullArgument(align, "align");

        this.componentGap = componentGap;
        this.maxWidth = maxWidth;
        this.sameSizeComponents = sameSizeComponents;
        this.align = align;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private Dimension maxDimension(Dimension dim1, Dimension dim2) {
        if (dim1.height < dim2.height && dim1.width < dim2.width) {
            return dim2;
        }

        if (dim2.height < dim1.height && dim2.width < dim1.width) {
            return dim1;
        }

        return new Dimension(
                Math.max(dim1.width, dim2.width),
                Math.max(dim1.height, dim2.height));
    }

    private Dimension tryGetCommonSize(Component[] components, Function<Component, Dimension> componentSizeGetter) {
        if (!sameSizeComponents) {
            return null;
        }

        Dimension result = null;
        for (Component component: components) {
            Dimension preferredSize = componentSizeGetter.apply(component);

            if (result == null) {
                result = preferredSize;
            }
            else {
                result = maxDimension(preferredSize, result);
            }
        }
        return result;
    }

    private Dimension layoutSize(Container parent, Function<Component, Dimension> componentSizeGetter) {
        Component[] components = parent.getComponents();
        int heightOffset = componentGap;
        int currentLineHeight = 0;

        int currentLineWidth = componentGap;
        int maxLineWidth = currentLineWidth;

        // Because we will add componentGap to the right side as well.
        int widthLimit = maxWidth - componentGap;

        Dimension commonSize = tryGetCommonSize(components, componentSizeGetter);

        for (Component component: components) {
            Dimension preferredSize = commonSize != null ? commonSize : componentSizeGetter.apply(component);
            int preferredWidth = (int)preferredSize.getWidth();
            int preferredHeight = (int)preferredSize.getHeight();

            int nextWidth = currentLineWidth + preferredWidth + componentGap;
            if (nextWidth > widthLimit) {
                nextWidth = componentGap + preferredWidth;
                heightOffset = heightOffset + currentLineHeight + componentGap;
                currentLineHeight = 0;
            }

            currentLineHeight = Math.max(currentLineHeight, preferredHeight);
            currentLineWidth = nextWidth;

            maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
        }

        int height = heightOffset + currentLineHeight;
        int width = maxLineWidth + componentGap;

        return new Dimension(width, height);
    }

    /**
     * {@inheritDoc }
     *
     * @return the preferred size to display the children components. This
     *   method never returns {@code null}.
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return layoutSize(parent, Component::getPreferredSize);
    }

    /**
     * {@inheritDoc }
     *
     * @return the minimum size to display the children components. This
     *   method never returns {@code null}.
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void layoutContainer(Container parent) {
        int parentHeight = parent.getHeight();

        List<ComponentLine> lines = new LinkedList<>();

        ComponentLine currentLine = new ComponentLine(componentGap);
        lines.add(currentLine);

        int widthLimit = maxWidth - componentGap;

        Component[] components = parent.getComponents();
        Dimension commonSize = tryGetCommonSize(components, Component::getPreferredSize);

        for (Component component: parent.getComponents()) {
            Dimension preferredSize = commonSize != null ? commonSize : component.getPreferredSize();

            if (!currentLine.tryAddComponent(component, preferredSize, widthLimit)) {
                currentLine = new ComponentLine(componentGap);
                lines.add(currentLine);

                currentLine.tryAddComponent(component, preferredSize, widthLimit);
            }
        }

        int height = 0;

        for (ComponentLine line: lines) {
            height += line.getHeight();
        }

        height += (lines.size() - 1) * componentGap;

        int offsetY = (parentHeight - height) / 2;
        offsetY = Math.max(0, offsetY);

        for (ComponentLine line: lines) {
            int offsetX = line.getStartOffsetX(parent, align);

            int lineHeight = line.getHeight();

            for (FixedSizeComponent component: line.getComponents()) {
                int y = (lineHeight - component.height) / 2;

                component.component.setLocation(offsetX, offsetY + y);
                component.setSize();

                offsetX += component.width + componentGap;
            }

            offsetY += lineHeight + componentGap;
        }
    }

    private static final class ComponentLine {
        private final List<FixedSizeComponent> components;
        private final int gap;

        private int width;
        private int height;

        public ComponentLine(int gap) {
            this.gap = gap;
            this.components = new LinkedList<>();
            this.width = gap;
            this.height = 0;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public List<FixedSizeComponent> getComponents() {
            return components;
        }

        public int getStartOffsetX(Component parent, Alignment align) {
            switch (align) {
                case LEFT:
                    return gap;
                case CENTER:
                    return (parent.getWidth() - width) / 2 + gap;
                case RIGHT:
                    return parent.getWidth() - width + gap;
                default:
                    throw new AssertionError(align.name());
            }
        }

        public boolean tryAddComponent(Component component, Dimension preferredSize, int maxWidth) {
            int preferredWidth = (int)preferredSize.getWidth();
            int preferredHeight = (int)preferredSize.getHeight();

            int nextWidth = width + preferredWidth + gap;
            if (!components.isEmpty() && nextWidth > maxWidth) {
                return false;
            }

            components.add(new FixedSizeComponent(preferredWidth, preferredHeight, component));
            height = Math.max(height, preferredHeight);
            width = nextWidth;
            return true;
        }
    }

    private static final class FixedSizeComponent {
        public final int width;
        public final int height;
        public final Component component;

        public FixedSizeComponent(int width, int height, Component component) {
            this.width = width;
            this.height = height;
            this.component = component;
        }

        public void setSize() {
            component.setSize(width, height);
        }
    }
}
