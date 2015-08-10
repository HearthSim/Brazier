package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.cards.Card;

public interface ManaCostAdjuster {
    public int adjustCost(Card card, int currentManaCost);
}
