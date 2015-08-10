package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.CardPlayRef;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class CardPlayedEvent implements PlayerProperty, LabeledEntity, CardPlayRef {
    private final Card card;
    private final int manaCost;

    public CardPlayedEvent(Card card, int manaCost) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        this.card = card;
        this.manaCost = manaCost;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return card.getKeywords();
    }

    @Override
    public Player getOwner() {
        return card.getOwner();
    }

    @Override
    public Card getCard() {
        return card;
    }

    @Override
    public int getManaCost() {
        return manaCost;
    }
}
