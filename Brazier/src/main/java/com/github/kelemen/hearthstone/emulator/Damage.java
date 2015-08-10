package com.github.kelemen.hearthstone.emulator;

import org.jtrim.utils.ExceptionHelper;

public final class Damage {
    private final DamageSource source;
    private final int attack;

    public Damage(DamageSource source, int damage) {
        ExceptionHelper.checkNotNullArgument(source, "source");

        this.source = source;
        this.attack = damage;
    }

    public DamageSource getSource() {
        return source;
    }

    public int getAttack() {
        return attack;
    }

    @Override
    public String toString() {
        return "Damage{" + attack + ", source=" + source + '}';
    }
}
