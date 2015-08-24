package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;

public final class HpProperty implements Silencable {
    private final int baseMaxValue;

    private int buffedMaxHp;
    private int auraBuff;

    private int currentMaxHp;
    private int currentHp;

    public HpProperty(int baseMaxValue) {
        this.baseMaxValue = baseMaxValue;
        this.currentMaxHp = baseMaxValue;
        this.buffedMaxHp = baseMaxValue;
        this.auraBuff = 0;
        this.currentHp = baseMaxValue;
    }

    public UndoAction applyAura() {
        int newMaxHp = buffedMaxHp + auraBuff;
        if (newMaxHp == currentMaxHp) {
            return UndoAction.DO_NOTHING;
        }

        int prevMaxHp = currentMaxHp;
        int prevCurrentHp = currentHp;

        if (newMaxHp > currentMaxHp) {
            currentHp = currentHp + (newMaxHp - currentMaxHp);
        }
        currentHp = Math.min(newMaxHp, currentHp);
        currentMaxHp = newMaxHp;

        return () -> {
            currentHp = prevCurrentHp;
            currentMaxHp = prevMaxHp;
        };
    }

    public HpProperty copy() {
        HpProperty result = new HpProperty(baseMaxValue);
        int auraOffset = currentMaxHp - buffedMaxHp;

        result.currentMaxHp = buffedMaxHp;
        result.buffedMaxHp = buffedMaxHp;
        result.auraBuff = 0;
        result.currentHp = Math.min(result.getMaxHp(), currentHp - auraOffset);
        return result;
    }

    public UndoAction setCurrentHp(int newHp) {
        if (newHp == currentHp) {
            return UndoAction.DO_NOTHING;
        }

        int prevCurrentHp = currentHp;
        currentHp = Math.min(getMaxHp(), newHp);
        return () -> currentHp = prevCurrentHp;
    }

    public UndoAction buffHp(int amount) {
        if (amount == 0) {
            return UndoAction.DO_NOTHING;
        }

        currentHp += amount;
        UndoAction maxHpUndo = setMaxHp(buffedMaxHp + amount);

        return () -> {
            maxHpUndo.undo();
            currentHp -= amount;
        };
    }

    public UndoableUnregisterRef addAuraBuff(int amount) {
        auraBuff += amount;

        return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
            @Override
            public UndoAction unregister() {
                auraBuff -= amount;
                return () -> auraBuff += amount;
            }

            @Override
            public void undo() {
                auraBuff -= amount;
            }
        });
    }

    public int getMaxHp() {
        return currentMaxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public boolean isDead() {
        return getCurrentHp() <= 0;
    }

    @Override
    public UndoAction silence() {
        return setMaxHp(baseMaxValue);
    }

    public UndoAction setMaxAndCurrentHp(int newValue) {
        int prevBuffedMaxHp = buffedMaxHp;
        int prevCurrentHp = currentHp;
        int prevCurrentMaxHp = currentMaxHp;

        buffedMaxHp = newValue;
        currentMaxHp = newValue;
        currentHp = newValue;

        return () -> {
            currentMaxHp = prevCurrentMaxHp;
            currentHp = prevCurrentHp;
            buffedMaxHp = prevBuffedMaxHp;
        };
    }

    public UndoAction setMaxHp(int newValue) {
        int prevBuffedMaxHp = buffedMaxHp;
        int prevCurrentHp = currentHp;
        int prevCurrentMaxHp = currentMaxHp;

        buffedMaxHp = newValue;
        currentMaxHp = newValue;
        currentHp = Math.min(currentHp, newValue);

        return () -> {
            currentMaxHp = prevCurrentMaxHp;
            currentHp = prevCurrentHp;
            buffedMaxHp = prevBuffedMaxHp;
        };
    }

    public boolean isDamaged() {
        return currentHp < getMaxHp();
    }
}
