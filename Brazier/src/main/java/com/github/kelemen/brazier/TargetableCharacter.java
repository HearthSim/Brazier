package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.weapons.AttackTool;

public interface TargetableCharacter extends PlayerProperty, LabeledEntity, DamageSource, BornEntity {
    public TargetId getTargetId();

    public AttackTool getAttackTool();
    public UndoAction poison();
    public UndoableIntResult damage(Damage damage);
    public boolean isLethalDamage(int damage);
    public boolean isTargetable(TargeterDef targeterDef);

    public boolean isDead();
    public boolean isDamaged();
}
