package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;

public final class HpProperty implements Silencable {
    private final int baseMaxValue;

    private int currentMaxHp;
    private int extraAuraMaxHp;

    private int currentHp;

    public HpProperty(int baseMaxValue) {
        this.baseMaxValue = baseMaxValue;
        this.currentMaxHp = baseMaxValue;
        this.extraAuraMaxHp = 0;
        this.currentHp = baseMaxValue;
    }

    public HpProperty copy() {
        HpProperty result = new HpProperty(baseMaxValue);
        result.currentMaxHp = currentMaxHp;
        result.extraAuraMaxHp = 0;
        result.currentHp = Math.min(result.getMaxHp(), currentHp - extraAuraMaxHp);
        return result;
    }

    public UndoAction setCurrentHp(int newHp) {
        if (newHp == currentHp) {
            return UndoAction.DO_NOTHING;
        }

        // Omitting the range check is intentional (i.e., we allow setting the
        // current hp to a higher value than the maximum hp by this method).
        int prevCurrentHp = currentHp;
        currentHp = newHp;
        return () -> currentHp = prevCurrentHp;
    }

    public UndoAction adjustCurrentHp(int amount) {
        int prevCurrentHp = currentHp;
        currentHp = Math.min(getMaxHp(), currentHp + amount);
        return () -> currentHp = prevCurrentHp;
    }

    public UndoAction buffHp(int amount) {
        if (amount == 0) {
            return UndoAction.DO_NOTHING;
        }

        currentHp += amount;
        currentMaxHp += amount;

        return () -> {
            currentMaxHp -= amount;
            currentHp -= amount;
        };
    }

    public UndoableUnregisterRef addAuraBuff(int amount) {
        currentHp += amount;
        extraAuraMaxHp += amount;

        return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
            @Override
            public UndoAction unregister() {
                int prevCurrentHp = currentHp;
                int prevExtraMaxHp = extraAuraMaxHp;

                extraAuraMaxHp -= amount;
                currentHp = Math.min(getMaxHp(), currentHp);

                return () -> {
                    currentHp = prevCurrentHp;
                    extraAuraMaxHp = prevExtraMaxHp;
                };
            }

            @Override
            public void undo() {
                currentHp -= amount;
                extraAuraMaxHp -= amount;
            }
        });
    }

    public int getMaxHp() {
        return currentMaxHp + extraAuraMaxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public boolean isDead() {
        return getCurrentHp() <= 0;
    }

    @Override
    public UndoAction silence() {
        int prevCurrentHp = currentHp;
        int prevCurrentMaxHp = currentMaxHp;

        currentMaxHp = baseMaxValue;
        currentHp = Math.min(getMaxHp(), prevCurrentMaxHp < currentMaxHp
                ? currentHp + currentMaxHp - prevCurrentMaxHp
                : currentHp);

        return () -> {
            currentHp = prevCurrentHp;
            currentMaxHp = prevCurrentMaxHp;
        };
    }

    public UndoAction setMaxHp(int newValue) {
        int prevMaxHp = currentMaxHp;
        currentMaxHp = newValue;

        int prevCurentHp = currentHp;
        currentHp = Math.min(currentHp, getMaxHp());

        return () -> {
            currentHp = prevCurentHp;
            currentMaxHp = prevMaxHp;
        };
    }

    public boolean isDamaged() {
        return currentHp < getMaxHp();
    }
}
