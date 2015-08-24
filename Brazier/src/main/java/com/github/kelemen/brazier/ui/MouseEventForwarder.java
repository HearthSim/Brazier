package com.github.kelemen.brazier.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.SwingUtilities;

final class MouseEventForwarder implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final MouseEventForwarder INSTANCE = new MouseEventForwarder();

    private MouseEventForwarder() {
    }

    public static MouseEventForwarder getEventForwarder() {
        return INSTANCE;
    }

    private void forwardEvent(MouseEvent e) {
        Component component = e.getComponent();

        Container parent = component.getParent();
        while (parent != null) {
            if (parent.getMouseListeners().length != 0) {
                parent.dispatchEvent(SwingUtilities.convertMouseEvent(component, e, parent));
                break;
            }
            parent = parent.getParent();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        forwardEvent(e);
    }
}
