package com.github.kelemen.brazier;

public interface DamageSource extends PlayerProperty {
    public UndoableResult<Damage> createDamage(int damage);
}
