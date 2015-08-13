package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.PlayTargetRequest;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import org.jtrim.utils.ExceptionHelper;

public final class HeroPower implements PlayerProperty {
    private final Hero hero;
    private final CardDescr powerDef;
    private int useCount;

    public HeroPower(Hero hero, CardDescr powerDef) {
        ExceptionHelper.checkNotNullArgument(hero, "hero");
        ExceptionHelper.checkNotNullArgument(powerDef, "powerDef");

        this.hero = hero;
        this.powerDef = powerDef;
        this.useCount = 0;
    }

    @Override
    public Player getOwner() {
        return hero.getOwner();
    }

    public Card createCard() {
        return new Card(getOwner(), powerDef);
    }

    public CardDescr getPowerDef() {
        return powerDef;
    }

    public int getManaCost() {
        return powerDef.getManaCost();
    }

    public boolean canPlay() {
        return useCount < 1 && getOwner().canPlayCard(createCard());
    }

    public UndoAction play(PlayTargetRequest targetRequest) {
        Card card = createCard();

        useCount++;
        UndoAction playUndo = getOwner().playCardEffect(card, card.getActiveManaCost(), targetRequest);
        return () -> {
            playUndo.undo();
            useCount--;
        };
    }

    public UndoAction refresh() {
        int prevUseCount = useCount;
        useCount = 0;
        return () -> useCount = prevUseCount;
    }
}
