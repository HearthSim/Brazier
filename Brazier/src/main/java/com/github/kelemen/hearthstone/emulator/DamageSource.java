package com.github.kelemen.hearthstone.emulator;

public interface DamageSource extends PlayerProperty {
    public UndoableResult<Damage> createDamage(int damage);
}
