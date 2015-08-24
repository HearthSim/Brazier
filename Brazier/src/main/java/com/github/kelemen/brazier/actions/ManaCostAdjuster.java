package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.cards.Card;

public interface ManaCostAdjuster {
    public int adjustCost(Card card, int currentManaCost);
}
