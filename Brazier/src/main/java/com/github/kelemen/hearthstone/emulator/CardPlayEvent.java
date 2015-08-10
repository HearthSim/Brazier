package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.CardPlayArg;
import com.github.kelemen.hearthstone.emulator.actions.CardPlayRef;
import com.github.kelemen.hearthstone.emulator.actions.PlayTarget;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class CardPlayEvent implements PlayerProperty, LabeledEntity, CardPlayRef, TargetRef {
    private CardPlayArg cardPlayArg;
    private final int manaCost;
    private boolean vetodPlay;

    public CardPlayEvent(CardPlayArg cardPlayArg, int manaCost) {
        ExceptionHelper.checkNotNullArgument(cardPlayArg, "cardPlayArg");

        this.cardPlayArg = cardPlayArg;
        this.manaCost = manaCost;
        this.vetodPlay = false;
    }

    public UndoAction replaceTarget(TargetableCharacter newTarget) {
        if (newTarget == getTarget()) {
            return UndoAction.DO_NOTHING;
        }

        return replaceTarget(new PlayTarget(getCastingPlayer(), newTarget));
    }

    public UndoAction replaceTarget(PlayTarget newTarget) {
        ExceptionHelper.checkNotNullArgument(newTarget, "newTarget");

        CardPlayArg prevArg = cardPlayArg;
        cardPlayArg = new CardPlayArg(getCard(), newTarget);
        return () -> cardPlayArg = prevArg;
    }

    public Player getCastingPlayer() {
        return cardPlayArg.getTarget().getCastingPlayer();
    }

    public CardPlayArg getCardPlayArg() {
        return cardPlayArg;
    }

    @Override
    public TargetableCharacter getTarget() {
        return cardPlayArg.getTarget().getTarget();
    }

    @Override
    public Set<Keyword> getKeywords() {
        return getCard().getKeywords();
    }

    public UndoAction vetoPlay() {
        if (vetodPlay) {
            return UndoAction.DO_NOTHING;
        }

        vetodPlay = true;
        return () -> vetodPlay = false;
    }

    public boolean isVetodPlay() {
        return vetodPlay;
    }

    @Override
    public Card getCard() {
        return cardPlayArg.getCard();
    }

    @Override
    public Player getOwner() {
        return getCard().getOwner();
    }

    @Override
    public int getManaCost() {
        return manaCost;
    }
}
