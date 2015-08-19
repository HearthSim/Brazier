package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.CardPlayEvent;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.CardRef;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

public final class WorldEventActions {
    public static final WorldEventAction<PlayerProperty, CardPlayEvent> PREVENT_CARD_PLAY = (world, self, eventSource) -> {
        return eventSource.vetoPlay();
    };

    public static <Actor extends PlayerProperty, Target> WorldEventAction<Actor, Target> forEventArgTarget(
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        return action::alterWorld;
    }

    public static <Actor extends PlayerProperty> WorldEventAction<Actor, CardRef> forEventArgCardTarget(
            @NamedArg("action") TargetedAction<? super Actor, ? super Card> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor self, CardRef eventSource) -> {
            return action.alterWorld(world, self, eventSource.getCard());
        };
    }

    public static <Actor extends PlayerProperty> WorldEventAction<Actor, Object> forEventArgMinionTarget(
            @NamedArg("action") TargetedAction<? super Actor, ? super Minion> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor self, Object eventSource) -> {
            Minion minion = ActionUtils.tryGetMinion(eventSource);
            if (minion != null) {
                return action.alterWorld(world, self, minion);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static <Actor extends PlayerProperty> WorldEventAction<Actor, Object> withSelf(
            @NamedArg("action") TargetlessAction<? super Actor> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor self, Object eventSource) -> {
            return action.alterWorld(world, self);
        };
    }

    public static <Actor extends PlayerProperty> WorldEventAction<Actor, Object> withEventArgMinion(
            @NamedArg("action") TargetlessAction<? super Minion> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor self, Object eventSource) -> {
            Minion minion = ActionUtils.tryGetMinion(eventSource);
            if (minion != null) {
                return action.alterWorld(world, minion);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static WorldEventAction<PlayerProperty, CardPlayEvent> summonNewTargetForCardPlay(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
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

}
