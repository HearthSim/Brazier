package com.github.kelemen.hearthstone.emulator.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.function.Function;

public final class ConstAspectRatioLayout implements LayoutManager {
    private static final int V_GAP = 5;
    private static final int H_GAP = 5;

    private final double aspectRatio;

    public ConstAspectRatioLayout() {
        this(70.0, 99.0); // "A" paper
    }

    public ConstAspectRatioLayout(double widthRatio, double heightRatio) {
        this.aspectRatio = widthRatio / heightRatio;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private Dimension getLayoutSize(Container parent, Function<Component, Dimension> sizeGetter) {
        Component[] components = parent.getComponents();

        int maxHeight = 0;
        for (Component component: components) {
            int height = sizeGetter.apply(component).height;
            if (height > maxHeight) {
                maxHeight = height;
            }
        }

        int componentWidth = (int)Math.round(aspectRatio * maxHeight);
        int width = H_GAP * (components.length + 1) + components.length * componentWidth;
        return new Dimension(width, maxHeight + 2 * V_GAP);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int childCount = parent.getComponentCount();
        int parentHeight = parent.getHeight();
        int height = Math.max(0, parentHeight - 2 * V_GAP);
        int width = (int)Math.round(aspectRatio * height);
        int parentWidth = V_GAP * (childCount + 1) + childCount * width;
        return new Dimension(parentWidth, parentHeight);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return getLayoutSize(parent, Component::getMinimumSize);
    }

    @Override
    public void layoutContainer(Container parent) {
        int parentHeight = parent.getHeight();

        int height = Math.max(0, parentHeight - 2 * V_GAP);
        int offsetY = (parentHeight - height) / 2;
        int offsetX = H_GAP;

        int width = (int)Math.round(aspectRatio * height);

        for (Component component: parent.getComponents()) {
            component.setLocation(offsetX, offsetY);
            component.setSize(width, height);
            offsetX = offsetX + H_GAP + width;
        }
    }
}
