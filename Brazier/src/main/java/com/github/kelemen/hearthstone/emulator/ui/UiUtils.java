package com.github.kelemen.hearthstone.emulator.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jtrim.utils.ExceptionHelper;


public final class UiUtils {
    private static final Logger LOGGER = Logger.getLogger(UiUtils.class.getName());

    public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    private static final double TEXT_MARGIN = 5.0;

    public static void renderTextToImage(String text, Graphics2D g2d, int width, int height, TextLocation location) {
        g2d.setBackground(TRANSPARENT_COLOR);

        Rectangle2D textRect = g2d.getFontMetrics().getStringBounds(text, g2d);

        double offsetX;
        double offsetY;

        switch (location) {
            case TOP_LEFT: {
                offsetX = TEXT_MARGIN - textRect.getMinX();
                offsetY = TEXT_MARGIN - textRect.getMinY();
                break;
            }
            case TOP_RIGHT: {
                offsetX = width - textRect.getMaxX() - TEXT_MARGIN;
                offsetY = TEXT_MARGIN - textRect.getMinY();
                break;
            }
            case BOTTOM_LEFT: {
                offsetX = TEXT_MARGIN - textRect.getMinX();
                offsetY = height - textRect.getMaxY() - TEXT_MARGIN;
                break;
            }
            case BOTTOM_RIGHT: {
                offsetX = width - textRect.getMaxX() - TEXT_MARGIN;
                offsetY = height - textRect.getMaxY() - TEXT_MARGIN;
                break;
            }
            case CENTER: {
                double centerDestX = width / 2.0;
                double centerDestY = height / 2.0;
                offsetX = centerDestX - textRect.getCenterX();
                offsetY = centerDestY - textRect.getCenterY();
                break;
            }
            default:
                throw new AssertionError(location.name());

        }

        g2d.drawString(text, (float)offsetX, (float)offsetY);
    }

    public static void useLookAndFeel(String name) {
        Objects.requireNonNull(name, "name");

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (name.equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.SEVERE, "Failed to set " + name + " look and feel.", ex);
        }
    }

    public static void forwardMouseEvents(Component component) {
        component.addMouseListener(MouseEventForwarder.getEventForwarder());
        component.addMouseMotionListener(MouseEventForwarder.getEventForwarder());
        component.addMouseWheelListener(MouseEventForwarder.getEventForwarder());
    }

    public static <T> T getOnEdt(Supplier<T> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.get();
        }
        else {
            try {
                AtomicReference<T> resultRef = new AtomicReference<>();
                SwingUtilities.invokeAndWait(() -> {
                    resultRef.set(task.get());
                });
                return resultRef.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unexpected InterruptedException", ex);
            } catch (InvocationTargetException ex) {
                throw ExceptionHelper.throwUnchecked(ex.getCause());
            }
        }
    }

    public static void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unexpected InterruptedException", ex);
            } catch (InvocationTargetException ex) {
                throw ExceptionHelper.throwUnchecked(ex.getCause());
            }
        }
    }

    private UiUtils() {
        throw new AssertionError();
    }
}
