package com.github.kelemen.brazier.events;

import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import org.jtrim.utils.ExceptionHelper;

public final class UndoableUnregisterRefBuilder implements UndoableUnregisterRef {
    private UndoableUnregisterRef[] refs;
    private int count;

    public UndoableUnregisterRefBuilder() {
        this(10);
    }

    public UndoableUnregisterRefBuilder(int expectedSize) {
        this.refs = new UndoableUnregisterRef[expectedSize];
        this.count = 0;
    }

    public void addRef(UndoableUnregisterRef ref) {
        ExceptionHelper.checkNotNullArgument(ref, "ref");

        if (ref == UndoableUnregisterRef.UNREGISTERED_REF) {
            return;
        }

        if (refs.length >= count) {
            int newLength = Math.max(count + 1, 2 * refs.length);
            UndoableUnregisterRef[] newRefs = new UndoableUnregisterRef[newLength];
            System.arraycopy(refs, 0, newRefs, 0, count);
            refs = newRefs;
        }

        refs[count] = ref;
        count++;
    }

    @Override
    public UndoAction unregister() {
        UndoBuilder result = new UndoBuilder(count);
        for (int i = 0; i < count; i++) {
            result.addUndo(refs[i].unregister());
        }
        return result;
    }

    @Override
    public void undo() {
        for (int i = count - 1; i >= 0; i--) {
            refs[i].undo();
        }
    }
}
