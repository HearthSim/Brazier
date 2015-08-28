package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Priorities;
import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;

public final class AuraAwareIntProperty implements Silencable {
    private final int baseValue;
    private final int minValue;

    private final AuraAwarePropertyBase<IntPropertyBuff> impl;

    public AuraAwareIntProperty(int baseValue) {
        this(baseValue, Integer.MIN_VALUE);
    }

    public AuraAwareIntProperty(int baseValue, int minValue) {
        this.baseValue = baseValue;
        this.minValue = minValue;

        this.impl = new AuraAwarePropertyBase<>((buffs) -> {
            return (prev) -> {
                int result = prev;
                for (AuraAwarePropertyBase.BuffRef<IntPropertyBuff> buffRef: buffs) {
                    result = buffRef.getBuff().buffProperty(result);
                }
                return result;
            };
        });
    }

    private AuraAwareIntProperty(AuraAwareIntProperty other) {
        this.baseValue = other.baseValue;
        this.minValue = other.minValue;
        this.impl = other.impl.copy();
    }

    public AuraAwareIntProperty copy() {
        return new AuraAwareIntProperty(this);
    }

    public UndoableUnregisterRef setValueTo(int newValue) {
        return addRemovableBuff((prev) -> newValue);
    }

    public UndoAction addBuff(int toAdd) {
        return addRemovableBuff(toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int toAdd) {
        return addRemovableBuff((prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(IntPropertyBuff toAdd) {
        return addRemovableBuff(Priorities.NORMAL_PRIORITY, false, toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(int toAdd) {
        return addExternalBuff((prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(IntPropertyBuff toAdd) {
        return addRemovableBuff(Priorities.HIGH_PRIORITY, true, toAdd);
    }

    public UndoableUnregisterRef setValueTo(int priority, boolean external, int newValue) {
        return addRemovableBuff(priority, external, (prev) -> newValue);
    }

    public UndoAction addBuff(int priority, boolean external, int toAdd) {
        return addRemovableBuff(priority, external, toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int priority, boolean external, int toAdd) {
        return addRemovableBuff(priority, external, (prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int priority, boolean external, IntPropertyBuff toAdd) {
        return impl.addRemovableBuff(priority, external, toAdd);
    }

    @Override
    public UndoAction silence() {
        return impl.silence();
    }

    public int getValue() {
        int result = impl.getCombinedView().buffProperty(baseValue);
        return result >= minValue ? result : minValue;
    }
}
