package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.cards.Card;
import org.jtrim.utils.ExceptionHelper;

public final class CardPlayArg implements CardRef {
    private final Card card;
    private final PlayTarget target;

    public CardPlayArg(Card card, PlayTarget target) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        ExceptionHelper.checkNotNullArgument(target, "target");

        this.card = card;
        this.target = target;
    }

    @Override
    public Card getCard() {
        return card;
    }

    public PlayTarget getTarget() {
        return target;
    }
}
