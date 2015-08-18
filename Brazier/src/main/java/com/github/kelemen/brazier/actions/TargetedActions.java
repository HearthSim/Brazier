package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.Priorities;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableIntResult;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;


public final class TargetedActions {
    public static final TargetedAction<DamageSource, TargetableCharacter> FULL_HEAL = (world, actor, target) -> {
        int healAmount;
        if (target instanceof Minion) {
            healAmount = ((Minion)target).getBody().getMaxHp();
        }
        else if (target instanceof Hero) {
            healAmount = ((Hero)target).getMaxHp();
        }
        else {
            healAmount = 0;
        }

        return damageTarget(actor, target, -healAmount);
    };

    public static final TargetedAction<Object, TargetableCharacter> KILL_TARGET = (world, actor, target) -> {
        return target.poison();
    };

    public static final TargetedAction<Object, Minion> TAUNT = (World world, Object actor, Minion target) -> {
        return target.getBody().setTaunt(true);
    };

    public static final TargetedAction<Object, Minion> GIVE_DIVINE_SHIELD = (world, actor, target) -> {
        return target.getProperties().getBody().setDivineShield(true);
    };

    public static final TargetedAction<Object, TargetableCharacter> FREEZE_TARGET = (world, actor, target) -> {
        return target.getAttackTool().freeze();
    };

    public static final TargetedAction<Object, Minion> RETURN_MINION = returnMinion(0);

    public static final TargetedAction<PlayerProperty, Minion> TAKE_CONTROL = (world, actor, target) -> {
        return actor.getOwner().getBoard().takeOwnership(target);
    };

    public static <Actor extends PlayerProperty> TargetedAction<Actor, Minion> doOnAttack(
            @NamedArg("action") TargetlessAction<? super Actor> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor actor, Minion target) -> {
            return target.addAndActivateAbility((Minion self) -> {
                WorldEvents events = world.getEvents();
                return events.attackListeners().addAction(Priorities.LOW_PRIORITY, (attackWorld, attackRequest) -> {
                    return attackRequest.getAttacker() == self
                            ? action.alterWorld(world, actor)
                            : UndoAction.DO_NOTHING;
                });
            });
        };
    }

    public static <Target> TargetedAction<Object, Target> withMinion(
            @NamedArg("action") TargetedAction<? super Minion, ? super Target> action) {
        return applyToMinionAction(action);
    }

    public static <Target> TargetedAction<Target, Target> withTarget(
            @NamedArg("action") TargetedAction<? super Target, ? super Target> action) {
        return (World world, Target actor, Target target) -> {
            return action.alterWorld(world, target, target);
        };
    }

    public static <Actor, Target, FinalTarget> TargetedAction<Actor, Target> forTargets(
            @NamedArg("selector") TargetedEntitySelector<? super Actor, ? super Target, ? extends FinalTarget> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super FinalTarget> action) {
        ExceptionHelper.checkNotNullArgument(selector, "targets");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor actor, Target initialTarget) -> {
            UndoBuilder result = new UndoBuilder();
            selector.select(world, actor, initialTarget).forEach((FinalTarget target) -> {
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

    public static TargetedAction<TargetableCharacter, TargetableCharacter> DAMAGE_TARGET = (world, actor, target) -> {
        int attack = actor.getAttackTool().getAttack();
        return damageTarget(actor, target, attack);
    };

    public static TargetedAction<DamageSource, TargetableCharacter> damageTarget(@NamedArg("damage") int damage) {
        return damageTarget(damage, damage);
    }

    public static TargetedAction<DamageSource, TargetableCharacter> damageTarget(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return (World world, DamageSource actor, TargetableCharacter target) -> {
            int damage = world.getRandomProvider().roll(minDamage, maxDamage);
            return damageTarget(actor, target, damage);
        };
    }

    private static UndoAction damageTarget(DamageSource actor, TargetableCharacter target, int damage) {
        UndoableResult<Damage> damageRef = actor.createDamage(damage);
        UndoableIntResult damageUndo = target.damage(damageRef.getResult());
        return () -> {
            damageUndo.undo();
            damageRef.getUndoAction();
        };
    }

    public static <Actor, Target> TargetedAction<Actor, Target> ifTarget(
            @NamedArg("condition") Predicate<? super Target> condition,
            @NamedArg("action") TargetlessAction<? super Actor> action) {
        ExceptionHelper.checkNotNullArgument(condition, "condition");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor actor, Target target) -> {
            if (condition.test(target)) {
                return action.alterWorld(world, actor);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static TargetedAction<Object, Minion> addAbility(
            @NamedArg("ability") ActivatableAbility<? super Minion> ability) {
        ExceptionHelper.checkNotNullArgument(ability, "ability");
        return (World world, Object actor, Minion target) -> {
            return target.addAndActivateAbility(ability);
        };
    }

    public static <Target> TargetedAction<Object, Target> buffTarget(
            @NamedArg("buff") PermanentBuff<? super Target> buff) {
        ExceptionHelper.checkNotNullArgument(buff, "buff");
        return (World world, Object actor, Target target) -> {
            return buff.buff(world, target);
        };
    }

    public static <Target> TargetedAction<Object, Target> buffTargetThisTurn(
            @NamedArg("buff") Buff<? super Target> buff) {
        ExceptionHelper.checkNotNullArgument(buff, "buff");
        return (World world, Object actor, Target target) -> {
            return ActionUtils.doTemporary(world, () -> buff.buff(world, target));
        };
    }

    public static TargetedAction<Object, Minion> addDeathRattle(
            @NamedArg("action") WorldEventAction<? super Minion, ? super Minion> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Object actor, Minion target) -> {
            return target.addDeathRattle(action);
        };
    }

    public static TargetedAction<Object, Minion> returnMinion(@NamedArg("costReduction") int costReduction) {
        return (World world, Object actor, Minion target) -> {
            Player owner = target.getOwner();
            CardDescr baseCard = target.getBaseDescr().getBaseCard();

            UndoBuilder result = new UndoBuilder();
            result.addUndo(target.getLocationRef().removeFromBoard());

            Card card = new Card(owner, baseCard);
            if (costReduction != 0) {
                result.addUndo(card.decreaseManaCost(costReduction));
            }

            result.addUndo(owner.getHand().addCard(card));

            return result;
        };
    }

    private static <Target> TargetedAction<Object, Target> applyToMinionAction(
            TargetedAction<? super Minion, ? super Target> buffAction) {
        return (World world, Object actor, Target target) -> {
            return applyToMinion(world, actor, target, buffAction);
        };
    }

    private static <Target> UndoAction applyToMinion(
            World world,
            Object actor,
            Target target,
            TargetedAction<? super Minion, ? super Target> buffAction) {
        Minion minion = ActionUtils.tryGetMinion(actor);
        return minion != null ? buffAction.alterWorld(world, minion, target) : UndoAction.DO_NOTHING;
    }

    public static TargetedAction<Object, Minion> transformMinion(@NamedArg("minion") MinionProvider[] minion) {
        List<MinionProvider> minionCopy = new ArrayList<>(Arrays.asList(minion));
        ExceptionHelper.checkNotNullElements(minionCopy, "minion");

        return transformMinion((originalMinion) -> {
            MinionProvider selected = ActionUtils.pickRandom(originalMinion.getWorld(), minionCopy);
            return selected != null ? selected.getMinion() : null;
        });
    }

    public static TargetedAction<Object, Minion> transformMinion(Function<? super Minion, MinionDescr> newMinionGetter) {
        ExceptionHelper.checkNotNullArgument(newMinionGetter, "newMinionGetter");

        return (World world, Object actor, Minion target) -> {
            MinionDescr newMinion = newMinionGetter.apply(target);
            return target.transformTo(newMinion);
        };
    }

    private TargetedActions() {
        throw new AssertionError();
    }
}
