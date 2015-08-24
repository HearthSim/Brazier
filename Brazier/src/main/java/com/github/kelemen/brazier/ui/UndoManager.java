package com.github.kelemen.brazier.ui;

import com.github.kelemen.brazier.actions.UndoAction;
import java.util.LinkedList;
import java.util.List;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

import static org.jtrim.property.PropertyFactory.*;

public final class UndoManager {
    private final MutableProperty<Boolean> hasUndos;
    private final List<UndoAction> undos;

    public UndoManager() {
        this.undos = new LinkedList<>();
        this.hasUndos = lazilySetProperty(memProperty(false));
    }

    public void addUndo(UndoAction undo) {
        ExceptionHelper.checkNotNullArgument(undo, "undo");
        undos.add(undo);
        hasUndos.setValue(true);
    }

    public PropertySource<Boolean> hasUndos() {
        return hasUndos;
    }

    public void undo() {
        if (!undos.isEmpty()) {
            undos.remove(undos.size() - 1).undo();
            hasUndos.setValue(!undos.isEmpty());
        }
    }
}
