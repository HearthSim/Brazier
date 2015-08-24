package com.github.kelemen.brazier;

import org.jtrim.utils.ExceptionHelper;

public final class ArmorGainedEvent implements PlayerProperty {
    private final Hero hero;
    private final int armorGained;

    public ArmorGainedEvent(Hero hero, int armorGained) {
        ExceptionHelper.checkNotNullArgument(hero, "hero");
        this.hero = hero;
        this.armorGained = armorGained;
    }

    public Hero getHero() {
        return hero;
    }

    public int getArmorGained() {
        return armorGained;
    }

    @Override
    public Player getOwner() {
        return hero.getOwner();
    }
}
