package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableIntResult;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedActions {
    public static <Target> TargetedAction<Card, Target> withCardsMinion(
            @NamedArg("action") TargetedAction<? super Minion, ? super Target> action) {
        return forActors(TargetedEntitySelectors.actorCardsMinion(), action);
    }

    public static <Actor, Target, FinalTarget> TargetedAction<Actor, Target> forTargets(
            @NamedArg("targets") TargetedEntitySelector<? super Actor, ? super Target, ? extends FinalTarget> targets,
            @NamedArg("action") TargetedAction<? super Actor, ? super FinalTarget> action) {
        ExceptionHelper.checkNotNullArgument(targets, "targets");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor actor, Target initialTarget) -> {
            UndoBuilder result = new UndoBuilder();
            targets.select(world, actor, initialTarget).forEach((FinalTarget target) -> {
                result.addUndo(action.alterWorld(world, actor, target));
            });
            return result;
        };
    }

    public static <Actor, Target, FinalActor> TargetedAction<Actor, Target> forActors(
            @NamedArg("actors") TargetedEntitySelector<? super Actor, ? super Target, ? extends FinalActor> actors,
            @NamedArg("action") TargetedAction<? super FinalActor, ? super Target> action) {
        ExceptionHelper.checkNotNullArgument(actors, "actors");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor initialActor, Target target) -> {
            UndoBuilder result = new UndoBuilder();
            actors.select(world, initialActor, target).forEach((FinalActor actor) -> {
                result.addUndo(action.alterWorld(world, actor, target));
            });
            return result;
        };
    }

    public static TargetedAction<DamageSource, TargetableCharacter> damageTarget(@NamedArg("damage") int damage) {
        return damageTarget(damage, damage);
    }

    public static TargetedAction<DamageSource, TargetableCharacter> damageTarget(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return (World world, DamageSource actor, TargetableCharacter target) -> {
            if (target == null) {
                return UndoAction.DO_NOTHING;
            }

            int damage = world.getRandomProvider().roll(minDamage, maxDamage);
            UndoableResult<Damage> damageRef = actor.createDamage(damage);
            UndoableIntResult damageUndo = target.damage(damageRef.getResult());
            return () -> {
                damageUndo.undo();
                damageRef.getUndoAction();
            };
        };
    }

    private TargetedActions() {
        throw new AssertionError();
    }
}
