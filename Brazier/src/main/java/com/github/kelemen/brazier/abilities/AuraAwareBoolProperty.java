package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Priorities;
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
        return addRemovableBuff(Priorities.HIGH_PRIORITY, true, (prev) -> newValue);
    }

    public UndoableUnregisterRef addRemovableBuff(BoolPropertyBuff toAdd) {
        return addRemovableBuff(Priorities.NORMAL_PRIORITY, false, toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(BoolPropertyBuff toAdd) {
        return addRemovableBuff(Priorities.HIGH_PRIORITY, true, toAdd);
    }

    public UndoableUnregisterRef setValueTo(int priority, boolean external, boolean newValue) {
        return addRemovableBuff(priority, external, (prev) -> newValue);
    }

    public UndoableUnregisterRef addRemovableBuff(int priority, boolean external, BoolPropertyBuff toAdd) {
        return impl.addRemovableBuff(priority, external, toAdd);
    }

    @Override
    public UndoAction silence() {
        return impl.silence();
    }

    public boolean getValue() {
        return impl.getCombinedView().buffProperty(baseValue);
    }
}
