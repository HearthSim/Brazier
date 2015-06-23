package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.CardPlayEvent;
import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

public final class CardEventActions {
    public static final WorldEventAction<PlayerProperty, CardRef> ADD_COPY_TO_HAND = (world, self, eventSource) -> {
        Hand hand = self.getOwner().getHand();
        return hand.addCard(eventSource.getCard().getCardDescr());
    };

    public static final WorldEventAction<PlayerProperty, CardPlayEvent> PREVENT_CARD_PLAY = (world, self, eventSource) -> {
        return eventSource.vetoPlay();
    };

    public static WorldEventAction<PlayerProperty, CardRef> applyToCardMinion(@NamedArg("action") MinionAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, PlayerProperty self, CardRef eventSource) -> {
            Minion minion = eventSource.getCard().getMinion();
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }
            return action.alterWorld(world, minion);
        };
    }

    public static WorldEventAction<Card, Object> applyToSelfCardMinion(@NamedArg("action") MinionAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Card self, Object eventSource) -> {
            Minion minion = self.getMinion();
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }
            return action.alterWorld(world, minion);
        };
    }

    public static WorldEventAction<PlayerProperty, CardRef> damageCardMinionWithSpell(@NamedArg("damage") int damage) {
        return (World world, PlayerProperty self, CardRef eventSource) -> {
            Minion minion = eventSource.getCard().getMinion();
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }

            Player player = self.getOwner();
            Damage appliedDamage = player.getSpellDamage(damage);
            return minion.damage(appliedDamage);
        };
    }

    public static WorldEventAction<PlayerProperty, CardPlayEvent> summonNewTargetForCard(
            @NamedArg("minion") MinionProvider minion) {
        return (world, self, eventSource) -> {
            Player targetPlayer = self.getOwner();
            if (targetPlayer.getBoard().isFull()) {
                return UndoAction.DO_NOTHING;
            }

            Minion summonedMinion = new Minion(targetPlayer, minion.getMinion());
            UndoAction summonUndo = targetPlayer.summonMinion(summonedMinion);
            UndoAction retargetUndo = eventSource.replaceTarget(summonedMinion);
            return () -> {
                retargetUndo.undo();
                summonUndo.undo();
            };
        };
    }

    private CardEventActions() {
        throw new AssertionError();
    }
}
