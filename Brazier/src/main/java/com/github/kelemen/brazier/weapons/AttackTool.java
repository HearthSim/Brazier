package com.github.kelemen.brazier.weapons;

import com.github.kelemen.brazier.actions.UndoAction;

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
