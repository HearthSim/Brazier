package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;

public final class AuraAwareBoolProperty implements Silencable {
    private final boolean baseValue;
    private final AuraAwarePropertyBase<BoolPropertyBuff> impl;

    public AuraAwareBoolProperty(boolean baseValue) {
        this.baseValue = baseValue;

        this.impl = new AuraAwarePropertyBase<>((buffs) -> {
            return (prev) -> {
                boolean result = prev;
                for (AuraAwarePropertyBase.BuffRef<BoolPropertyBuff> buffRef: buffs) {
                    result = buffRef.getBuff().buffProperty(result);
                }
                return result;
            };
        });
    }

    private AuraAwareBoolProperty(AuraAwareBoolProperty other) {
        this.baseValue = other.baseValue;
        this.impl = other.impl.copy();
    }

    public AuraAwareBoolProperty copy() {
        return new AuraAwareBoolProperty(this);
    }

    public UndoableUnregisterRef setValueTo(boolean newValue) {
        return addRemovableBuff((prev) -> newValue);
    }

    public UndoableUnregisterRef setValueToExternal(boolean newValue) {
        return addRemovableBuff(BuffArg.NORMAL_AURA_BUFF, (prev) -> newValue);
    }

    public UndoableUnregisterRef addRemovableBuff(BoolPropertyBuff toAdd) {
        return addRemovableBuff(BuffArg.NORMAL_BUFF, toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(BoolPropertyBuff toAdd) {
        return addRemovableBuff(BuffArg.NORMAL_AURA_BUFF, toAdd);
    }

    public UndoableUnregisterRef setValueTo(BuffArg buffArg, boolean newValue) {
        return addRemovableBuff(buffArg, (prev) -> newValue);
    }

    public UndoableUnregisterRef addRemovableBuff(BuffArg buffArg, BoolPropertyBuff toAdd) {
        return impl.addRemovableBuff(buffArg, toAdd);
    }

    @Override
    public UndoAction silence() {
        return impl.silence();
    }

    public boolean getValue() {
        return impl.getCombinedView().buffProperty(baseValue);
    }
}
