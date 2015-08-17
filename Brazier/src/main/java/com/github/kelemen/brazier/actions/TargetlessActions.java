package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.BornEntity;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

public final class TargetlessActions {
    public static <Actor, FinalActor> TargetlessAction<Actor> forActors(
            @NamedArg("actors") EntitySelector<? super Actor, ? extends FinalActor> actors,
            @NamedArg("action") TargetlessAction<? super FinalActor> action) {
        ExceptionHelper.checkNotNullArgument(actors, "actors");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor initialActor) -> {
            UndoBuilder result = new UndoBuilder();
            actors.select(world, initialActor).forEach((FinalActor actor) -> {
                result.addUndo(action.alterWorld(world, actor));
            });
            return result;
        };
    }

    public static <Actor, Target extends BornEntity> TargetlessAction<Actor> forBornTargets(
            @NamedArg("targets") EntitySelector<? super Actor, ? extends Target> targets,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        return forBornTargets(targets, action, true);
    }

    public static <Actor, Target extends BornEntity> TargetlessAction<Actor> forBornTargets(
            @NamedArg("targets") EntitySelector<? super Actor, ? extends Target> targets,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action,
            @NamedArg("atomic") boolean atomic) {
        return forTargets(EntitySelectors.sorted(targets, BornEntity.CMP), action, atomic);
    }

    public static <Actor, Target> TargetlessAction<Actor> forTargets(
            @NamedArg("targets") EntitySelector<? super Actor, ? extends Target> targets,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        return forTargets(targets, action, true);
    }

    public static <Actor, Target> TargetlessAction<Actor> forTargets(
            @NamedArg("targets") EntitySelector<? super Actor, ? extends Target> targets,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action,
            @NamedArg("atomic") boolean atomic) {
        ExceptionHelper.checkNotNullArgument(targets, "targets");
        ExceptionHelper.checkNotNullArgument(action, "action");

        TargetlessAction<Actor> resultAction = (World world, Actor actor) -> {
            UndoBuilder result = new UndoBuilder();
            targets.select(world, actor).forEach((Target target) -> {
                result.addUndo(action.alterWorld(world, actor, target));
            });
            return result;
        };

        if (atomic) {
            return (World world, Actor actor) -> {
                return world.getEvents().doAtomic(() -> resultAction.alterWorld(world, actor));
            };
        }
        else {
            return resultAction;
        }
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> destroyOpponentsWeapon() {
        return (World world, Actor actor) -> {
            return actor.getOwner().destroyWeapon();
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> actWithOpponent(
            @NamedArg("action") TargetlessAction<? super Player> action) {
        return (World world, Actor actor) -> {
            return action.alterWorld(world, actor.getOwner().getOpponent());
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> drawForSelf() {
        return (World world, Actor actor) -> {
            return actor.getOwner().drawCardToHand();
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> drawForOpponent() {
        return actWithOpponent(drawForSelf());
    }

    public static TargetlessAction<Minion> swapWithMinionInHand() {
        return (World world, Minion actor) -> {
            Hand hand = actor.getOwner().getHand();
            int cardIndex = hand.chooseRandomCardIndex(Card::isMinionCard);
            if (cardIndex < 0) {
                return UndoAction.DO_NOTHING;
            }

            CardDescr newCard = actor.getBaseDescr().getBaseCard();
            UndoableResult<Card> replaceCardRef = hand.replaceAtIndex(cardIndex, newCard);
            Minion newMinion = replaceCardRef.getResult().getMinion();
            if (newMinion == null) {
                throw new IllegalStateException("Selected a card with no minion.");
            }

            UndoAction replaceMinionUndo = actor.getLocationRef().replace(newMinion);
            return () -> {
                replaceMinionUndo.undo();
                replaceCardRef.undo();
            };
        };
    }

    private TargetlessActions() {
        throw new AssertionError();
    }
}
