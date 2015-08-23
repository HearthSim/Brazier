package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.DamageEvent;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;


public final class MinionEventActions {
    public static WorldEventAction<PlayerProperty, CardRef> COPY_PLAYER_CARD = (world, self, eventSource) -> {
        Card card = eventSource.getCard();
        Player opponent = card.getOwner().getOpponent();
        return opponent.getHand().addCard(card.getCardDescr());
    };

    public static WorldEventAction<DamageSource, DamageEvent> LIFE_STEAL_FOR_HERO = (world, self, event) -> {
        int damageDealt = event.getDamageDealt();
        if (damageDealt <= 0) {
            return UndoAction.DO_NOTHING;
        }

        return ActionUtils.damageCharacter(self, -damageDealt, self.getOwner().getHero());
    };

    public static WorldEventAction<Minion, Object> summonMinionFromHandRight(
            @NamedArg("keywords") Keyword[] keywords) {
        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);

        return (world, self, eventSource) -> {
            Hand hand = self.getOwner().getHand();
            int cardIndex = hand.chooseRandomCardIndex(cardFilter);
            if (cardIndex < 0) {
                return UndoAction.DO_NOTHING;
            }

            UndoableResult<Card> removedCardRef = hand.removeAtIndex(cardIndex);
            Minion minion = removedCardRef.getResult().getMinion();
            assert minion != null;

            UndoAction summonUndo = self.getLocationRef().summonRight(minion);
            return () -> {
                summonUndo.undo();
                removedCardRef.undo();
            };
        };
    }

    public static WorldEventAction<Minion, Object> summonRandomMinionRight(
            @NamedArg("keywords") Keyword[] keywords) {

        Function<World, MinionDescr> minionProvider = ActionUtils.randomMinionProvider(keywords);

        return (world, self, eventSource) -> {
            MinionDescr minion = minionProvider.apply(world);
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }
            return self.getLocationRef().summonRight(minion);
        };
    }

    public static WorldEventAction<Minion, Object> summonMinionRight(@NamedArg("minion") MinionProvider minion) {
        return (world, self, eventSource) -> {
            return self.getLocationRef().summonRight(minion.getMinion());
        };
    }

    private static <Self, Target> Predicate<Target> toPredicate(
            World world,
            Self self,
            WorldEventFilter<? super Self, ? super Target> filter) {
        return (target) -> filter.applies(world, self, target);
    }

    private static <Self, EventSource extends TargetableCharacter> WorldEventFilter<? super Self, ? super EventSource> adjustFilter(
            boolean collectDying,
            WorldEventFilter<? super Self, ? super EventSource> baseFilter) {
        if (collectDying) {
            return baseFilter;
        }

        return (world, owner, eventSource) -> {
            return !eventSource.isDead() && baseFilter.applies(world, owner, eventSource);
        };
    }

    public static WorldEventAction<PlayerProperty, Minion> doForEventSourceMinion(
            @NamedArg("action") MinionAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, PlayerProperty self, Minion eventSource) -> {
            return action.alterWorld(world, eventSource);
        };
    }

    public static WorldEventAction<PlayerProperty, PlayerProperty> doForEventSourcePlayer(
            @NamedArg("action") PlayerAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, PlayerProperty self, PlayerProperty eventSource) -> {
            return action.alterWorld(world, eventSource.getOwner());
        };
    }

    private MinionEventActions() {
        throw new AssertionError();
    }
}
