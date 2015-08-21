package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.CardPlayEvent;
import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageEvent;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetRef;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.AttackRequest;
import com.github.kelemen.hearthstone.emulator.actions.CardRef;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public final class WorldEventActions {
    public static final WorldEventAction<PlayerProperty, CardPlayEvent> PREVENT_CARD_PLAY = (world, self, eventSource) -> {
        return eventSource.vetoPlay();
    };

    public static final WorldEventAction<PlayerProperty, AttackRequest> MISS_TARGET_SOMETIMES
            = missTargetSometimes(1, 2);

    public static WorldEventAction<PlayerProperty, AttackRequest> missTargetSometimes(
            @NamedArg("missCount") int missCount,
            @NamedArg("attackCount") int attackCount) {

        return (World world, PlayerProperty self, AttackRequest eventSource) -> {
            TargetableCharacter defender = eventSource.getDefender();
            if (defender == null) {
                return UndoAction.DO_NOTHING;
            }

            int roll = world.getRandomProvider().roll(attackCount);
            if (roll >=  missCount) {
                return UndoAction.DO_NOTHING;
            }

            List<TargetableCharacter> targets = new ArrayList<>(Player.MAX_BOARD_SIZE);
            ActionUtils.collectAliveTargets(defender.getOwner(), targets, (target) -> target != defender);
            TargetableCharacter newTarget = ActionUtils.pickRandom(world, targets);
            if (newTarget == null) {
                return UndoAction.DO_NOTHING;
            }

            return eventSource.replaceDefender(newTarget);
        };
    }

    public static <Actor extends PlayerProperty> WorldEventAction<Actor, TargetRef> forDamageTarget(
            @NamedArg("action") TargetedAction<? super Actor, ? super TargetableCharacter> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor self, TargetRef eventSource) -> {
            return action.alterWorld(world, self, eventSource.getTarget());
        };
    }

    public static <Actor extends PlayerProperty> WorldEventAction<Actor, AttackRequest> forAttacker(
            @NamedArg("action") TargetedAction<? super Actor, ? super TargetableCharacter> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor self, AttackRequest eventSource) -> {
            return action.alterWorld(world, self, eventSource.getAttacker());
        };
    }

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

    public static <Actor extends DamageSource> WorldEventAction<Actor, DamageEvent> reflectDamage(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends TargetableCharacter> selector) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        return (world, self, eventSource) -> {
            int damage = eventSource.getDamageDealt();
            UndoableResult<Damage> damageRef = self.createDamage(damage);
            UndoAction damageUndo = selector.forEach(world, self, (target) -> target.damage(damageRef.getResult()));
            return () -> {
                damageUndo.undo();
                damageRef.undo();
            };
        };
    }
}
