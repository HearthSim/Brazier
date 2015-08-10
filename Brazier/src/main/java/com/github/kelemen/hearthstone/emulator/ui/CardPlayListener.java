package com.github.kelemen.hearthstone.emulator.ui;

import com.github.kelemen.hearthstone.emulator.cards.Card;

public interface CardPlayListener {
    public void playCard(int cardIndex, Card card);
}
