package com.github.kelemen.brazier.abilities;

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
        return addRemovableBuff(BuffArg.NORMAL_BUFF, toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(int toAdd) {
        return addExternalBuff((prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(IntPropertyBuff toAdd) {
        return addRemovableBuff(BuffArg.NORMAL_AURA_BUFF, toAdd);
    }

    public UndoableUnregisterRef setValueTo(BuffArg arg, int newValue) {
        return addRemovableBuff(arg, (prev) -> newValue);
    }

    public UndoAction addBuff(BuffArg arg, int toAdd) {
        return addRemovableBuff(arg, toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(BuffArg arg, int toAdd) {
        return addRemovableBuff(arg, (prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(BuffArg arg, IntPropertyBuff toAdd) {
        return impl.addRemovableBuff(arg, toAdd);
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
