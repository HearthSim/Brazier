package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import org.jtrim.utils.ExceptionHelper;

public final class DamageRequest implements TargetRef, PlayerProperty {
    private Damage damage;
    private final TargetableCharacter target;

    public DamageRequest(Damage damage, TargetableCharacter target) {
        ExceptionHelper.checkNotNullArgument(damage, "damage");
        ExceptionHelper.checkNotNullArgument(target, "target");

        this.damage = damage;
        this.target = target;
    }

    @Override
    public Player getOwner() {
        return damage.getSource().getOwner();
    }

    @Override
    public TargetableCharacter getTarget() {
        return target;
    }

    public Damage getDamage() {
        return damage;
    }

    public UndoAction adjustDamage(int newAttack) {
        if (damage.getAttack() == newAttack) {
            return UndoAction.DO_NOTHING;
        }

        return adjustDamage(new Damage(damage.getSource(), newAttack));
    }

    public UndoAction adjustDamage(Damage newDamage) {
        ExceptionHelper.checkNotNullArgument(newDamage, "newDamage");

        Damage prevDamage = damage;
        damage = newDamage;
        return () -> damage = prevDamage;
    }
}
