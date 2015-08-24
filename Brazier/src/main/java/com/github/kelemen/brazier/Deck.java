package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.cards.CardDescr;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class Deck {
    private final Player owner;
    private final List<Card> cards;

    public Deck(Player owner) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");

        this.owner = owner;
        this.cards = new ArrayList<>();
    }

    public List<Card> getCards(Predicate<? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        List<Card> result = new ArrayList<>();
        for (Card card: cards) {
            if (filter.test(card)) {
                result.add(card);
            }
        }
        return result;
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public UndoAction setCards(Collection<? extends CardDescr> newCards) {
        ExceptionHelper.checkNotNullElements(newCards, "newCards");

        List<Card> prevDeck;
        if (!cards.isEmpty()) {
            prevDeck = new ArrayList<>(cards);
        }
        else {
            prevDeck = Collections.emptyList();
        }

        cards.clear();
        for (CardDescr card: newCards) {
            Objects.requireNonNull(card, "newCards[?]");
            cards.add(new Card(owner, card));
        }

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

    public UndoableResult<Card> tryDrawOneCard() {
        if (cards.isEmpty()) {
            return null;
        }
        else {
            Card result = cards.remove(cards.size() - 1);
            return new UndoableResult<>(result, () -> cards.add(result));
        }
    }

    private void checkOwner(Card card) {
        if (card.getOwner() != owner) {
            throw new IllegalArgumentException("Card has the wrong owner player.");
        }
    }

    public UndoAction putOnTop(CardDescr card) {
        return putOnTop(new Card(owner, card));
    }

    public UndoAction putOnTop(Card card) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        checkOwner(card);

        cards.add(card);
        return () -> cards.remove(cards.size() - 1);
    }

    public UndoAction putToRandomPosition(RandomProvider randomProvider, CardDescr card) {
        return putToRandomPosition(randomProvider, new Card(owner, card));
    }

    public UndoAction putToRandomPosition(RandomProvider randomProvider, Card card) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        checkOwner(card);

        int pos = randomProvider.roll(cards.size() + 1);

        cards.add(pos, card);
        return () -> cards.remove(pos);
    }

    public UndoableResult<Card> tryDrawRandom(RandomProvider randomProvider, Predicate<? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(randomProvider, "randomProvider");
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        int[] indexes = new int[cards.size()];
        int cardCount = 0;

        int index = 0;
        for (Card card: cards) {
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
        Card removedCard = cards.remove(selectedIndex);
        return new UndoableResult<>(removedCard, () -> cards.add(selectedIndex, removedCard));
    }
}
