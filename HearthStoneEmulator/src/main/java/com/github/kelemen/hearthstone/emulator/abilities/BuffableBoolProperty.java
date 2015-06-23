package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.jtrim.utils.ExceptionHelper;

public final class BuffableBoolProperty implements Silencable {
    private final BooleanSupplier baseValue;
    private List<BoolPropertyBuff> buffs;
    private Boolean topBuff;

    public BuffableBoolProperty(BooleanSupplier baseValue) {
        ExceptionHelper.checkNotNullArgument(baseValue, "baseValue");

        this.baseValue = baseValue;
        // We do not use RefList because its element references
        // are not serializable.
        this.buffs = new ArrayList<>();
        this.topBuff = null;
    }

    public BuffableBoolProperty copy(BooleanSupplier newBaseValue) {
        BuffableBoolProperty result = new BuffableBoolProperty(newBaseValue);
        result.buffs.addAll(buffs);
        return result;
    }

    public UndoAction setValueTo(boolean newValue) {
        UndoAction silenceUndo = silence();
        UndoAction buffUndo = addBuff((prev) -> newValue);
        return () -> {
            buffUndo.undo();
            silenceUndo.undo();
        };
    }

    public UndoAction addNonRemovableBuff(boolean value) {
        if (Objects.equals(topBuff, value)) {
            return UndoAction.DO_NOTHING;
        }

        // This is a very common case, so optimize for it.
        Boolean prevTopBuff = topBuff;
        topBuff = value;
        return () -> topBuff = prevTopBuff;
    }

    public UndoableUnregisterRef addBuff(boolean value) {
        return addBuff((prevValue) -> value);
    }

    private <T> int removeAndGetIndex(List<T> list, T value) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (value == list.get(i)) {
                list.remove(i);
                return i;
            }
        }
        return -1;
    }

    public UndoableUnregisterRef addBuff(BoolPropertyBuff buff) {
        ExceptionHelper.checkNotNullArgument(buff, "buff");

        if (topBuff != null) {
            // Note that remove will not restore the extraBuff property
            // but the visible effect is the same.
            boolean currentTopBuff = topBuff;
            topBuff = null;
            addBuffAlways((prevValue) -> currentTopBuff);
        }

        return addBuffAlways(buff);
    }

    private UndoableUnregisterRef addBuffAlways(BoolPropertyBuff buff) {
        // We wrap the buff to ensure that we remove the
        // approriate buff when requested so.
        BoolPropertyBuffWrapper wrapper = new BoolPropertyBuffWrapper(buff);
        buffs.add(wrapper);
        return () -> {
            int prevIndex = removeAndGetIndex(buffs, wrapper);
            return prevIndex >= 0
                    ? () -> buffs.add(prevIndex, wrapper)
                    : UndoAction.DO_NOTHING;
        };
    }

    @Override
    public UndoAction silence() {
        if (buffs.isEmpty()) {
            if (topBuff == null) {
                return UndoAction.DO_NOTHING;
            }
            else {
                Boolean prevTopBuff = topBuff;
                topBuff = null;
                return () -> topBuff = prevTopBuff;
            }
        }

        Boolean prevTopBuff = topBuff;
        topBuff = null;

        List<BoolPropertyBuff> prevBuffs = buffs;
        buffs = new ArrayList<>();

        return () -> {
            topBuff = prevTopBuff;
            buffs = prevBuffs;
        };
    }

    public boolean getValue() {
        if (topBuff != null) {
            return topBuff;
        }

        boolean result = baseValue.getAsBoolean();
        for (BoolPropertyBuff buff: buffs) {
            result = buff.buffProperty(result);
        }
        return result;
    }

    private static final class BoolPropertyBuffWrapper implements BoolPropertyBuff {
        private final BoolPropertyBuff wrapped;

        public BoolPropertyBuffWrapper(BoolPropertyBuff wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean buffProperty(boolean prevValue) {
            return wrapped.buffProperty(prevValue);
        }
    }
}
