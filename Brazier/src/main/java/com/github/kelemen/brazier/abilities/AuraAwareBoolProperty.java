package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AuraAwareBoolProperty implements Silencable {
    private final AtomicBoolean baseValueRef;
    private final BuffableBoolProperty ownValue;
    private final BuffableBoolProperty auraBuffs;

    public AuraAwareBoolProperty(boolean baseValue) {
        this.baseValueRef = new AtomicBoolean(baseValue);
        this.ownValue = new BuffableBoolProperty(baseValueRef::get);
        this.auraBuffs = new BuffableBoolProperty(this.ownValue::getValue);
    }

    private AuraAwareBoolProperty(AuraAwareBoolProperty base) {
        this.baseValueRef = new AtomicBoolean(base.baseValueRef.get());
        this.ownValue = base.ownValue.copy(baseValueRef::get);
        this.auraBuffs = new BuffableBoolProperty(this.ownValue::getValue);
    }

    public AuraAwareBoolProperty copy() {
        return new AuraAwareBoolProperty(this);
    }

    @Override
    public UndoAction silence() {
        boolean prevBaseValue = baseValueRef.getAndSet(false);
        UndoAction silenceUndo = ownValue.silence();
        return () -> {
            silenceUndo.undo();
            baseValueRef.set(prevBaseValue);
        };
    }

    public UndoAction setValue(boolean newValue) {
        UndoAction buffSilenceUndo = ownValue.silence();
        boolean prevValue = baseValueRef.getAndSet(newValue);
        return () -> {
            baseValueRef.set(prevValue);
            buffSilenceUndo.undo();
        };
    }

    public UndoAction addBuff(boolean value) {
        return ownValue.addNonRemovableBuff(value);
    }

    public UndoableUnregisterRef addBuff(BoolPropertyBuff value) {
        return ownValue.addBuff(value);
    }

    public UndoableUnregisterRef addRemovableBuff(boolean value) {
        return ownValue.addBuff(value);
    }

    public boolean getValue() {
        return auraBuffs.getValue();
    }

    public UndoableUnregisterRef addAuraBuff(boolean value) {
        return auraBuffs.addBuff(value);
    }

    public UndoableUnregisterRef addAuraBuff(BoolPropertyBuff buff) {
        return auraBuffs.addBuff(buff);
    }
}
