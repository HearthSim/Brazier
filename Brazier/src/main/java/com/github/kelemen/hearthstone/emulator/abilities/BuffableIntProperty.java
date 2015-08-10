package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import org.jtrim.utils.ExceptionHelper;

public final class BuffableIntProperty implements Silencable {
    private final IntSupplier baseValue;
    private List<IntPropertyBuff> buffs;
    private int extraBuff;

    public BuffableIntProperty(IntSupplier baseValue) {
        ExceptionHelper.checkNotNullArgument(baseValue, "baseValue");

        this.baseValue = baseValue;
        // We do not use RefList because its element references
        // are not serializable.
        this.buffs = new ArrayList<>();
        this.extraBuff = 0;
    }

    public BuffableIntProperty copy(IntSupplier newBaseValue) {
        BuffableIntProperty result = new BuffableIntProperty(newBaseValue);
        result.buffs.addAll(buffs);
        result.extraBuff = extraBuff;
        return result;
    }

    public UndoAction setValueTo(int newValue) {
        UndoAction silenceUndo = silence();
        UndoAction buffUndo = addBuff((prev) -> newValue);
        return () -> {
            buffUndo.undo();
            silenceUndo.undo();
        };
    }

    public UndoAction addNonRemovableBuff(int toAdd) {
        if (toAdd == 0) {
            return UndoAction.DO_NOTHING;
        }

        // This is a very common case, so optimize for it.
        extraBuff += toAdd;
        return () -> extraBuff -= toAdd;
    }

    public UndoableUnregisterRef addBuff(int toAdd) {
        return addBuff((prevValue) -> prevValue + toAdd);
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

    public UndoableUnregisterRef addBuff(IntPropertyBuff buff) {
        ExceptionHelper.checkNotNullArgument(buff, "buff");

        if (extraBuff != 0) {
            // Note that remove will not restore the extraBuff property
            // but the visible effect is the same.
            int currentExtraBuff = extraBuff;
            extraBuff = 0;
            addBuffAlways((prevValue) -> prevValue + currentExtraBuff);
        }

        return addBuffAlways(buff);
    }

    private UndoableUnregisterRef addBuffAlways(IntPropertyBuff buff) {
        // We wrap the buff to ensure that we remove the
        // approriate buff when requested so.
        IntPropertyBuffWrapper wrapper = new IntPropertyBuffWrapper(buff);
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
            if (extraBuff == 0) {
                return UndoAction.DO_NOTHING;
            }
            else {
                int prevExtraBuff = extraBuff;
                extraBuff = 0;
                return () -> extraBuff = prevExtraBuff;
            }
        }

        int prevExtraBuff = extraBuff;
        extraBuff = 0;

        List<IntPropertyBuff> prevBuffs = buffs;
        buffs = new ArrayList<>();

        return () -> {
            extraBuff = prevExtraBuff;
            buffs = prevBuffs;
        };
    }

    public int getValue() {
        int result = baseValue.getAsInt();
        for (IntPropertyBuff buff: buffs) {
            result = buff.buffProperty(result);
        }
        return result + extraBuff;
    }

    private static final class IntPropertyBuffWrapper implements IntPropertyBuff {
        private final IntPropertyBuff wrapped;

        public IntPropertyBuffWrapper(IntPropertyBuff wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int buffProperty(int prevValue) {
            return wrapped.buffProperty(prevValue);
        }
    }
}
