package com.github.kelemen.hearthstone.emulator.weapons;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;

public interface AttackTool {
    public int getAttack();

    public boolean canAttackWith();
    public boolean canRetaliateWith();
    public boolean canTargetRetaliate();

    public boolean attacksLeft();
    public boolean attacksRight();

    public UndoAction incUseCount();
    public UndoAction freeze();
    public boolean isFrozen();
}
