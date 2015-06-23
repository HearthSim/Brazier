package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;


public final class Deck {
    private final List<CardDescr> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public List<CardDescr> getCards(Predicate<? super CardDescr> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        List<CardDescr> result = new ArrayList<>();
        for (CardDescr card: cards) {
            if (filter.test(card)) {
                result.add(card);
            }
        }
        return result;
    }

    public List<CardDescr> getCards() {
        return new ArrayList<>(cards);
    }

    public UndoAction setCards(Collection<? extends CardDescr> newCards) {
        ExceptionHelper.checkNotNullElements(newCards, "newCards");

        List<CardDescr> prevDeck;
        if (!cards.isEmpty()) {
            prevDeck = new ArrayList<>(cards);
        }
        else {
            prevDeck = Collections.emptyList();
        }

        cards.clear();
        cards.addAll(newCards);

        return () -> {
            cards.clear();
            cards.addAll(prevDeck);
        };
    }

    public void shuffle(RandomProvider randomProvider) {
        for (int i = cards.size() - 1; i > 0; i--) {
            int otherIndex = randomProvider.roll(i + 1);
            cards.set(i, cards.set(otherIndex, cards.get(i)));
        }
    }

    public int getNumberOfCards() {
        return cards.size();
    }

    public UndoableResult<CardDescr> tryDrawOneCard() {
        if (cards.isEmpty()) {
            return null;
        }
        else {
            CardDescr result = cards.remove(cards.size() - 1);
            return new UndoableResult<>(result, () -> cards.add(result));
        }
    }

    public UndoAction putOnTop(CardDescr card) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        cards.add(card);
        return () -> cards.remove(cards.size() - 1);
    }

    public UndoAction putToRandomPosition(RandomProvider randomProvider, CardDescr card) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        int pos = randomProvider.roll(cards.size() + 1);

        cards.add(pos, card);
        return () -> cards.remove(pos);
    }

    public UndoableResult<CardDescr> tryDrawRandom(RandomProvider randomProvider, Predicate<? super CardDescr> filter) {
        ExceptionHelper.checkNotNullArgument(randomProvider, "randomProvider");
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        int[] indexes = new int[cards.size()];
        int cardCount = 0;

        int index = 0;
        for (CardDescr card: cards) {
            if (filter.test(card)) {
                indexes[cardCount] = index;
                cardCount++;
            }
            index++;
        }

        if (cardCount == 0) {
            return null;
        }

        int selectedIndex = indexes[randomProvider.roll(cardCount)];
        CardDescr removedCard = cards.remove(selectedIndex);
        return new UndoableResult<>(removedCard, () -> cards.add(selectedIndex, removedCard));
    }
}
