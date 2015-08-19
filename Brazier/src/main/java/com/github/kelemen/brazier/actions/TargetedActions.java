package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Deck;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.Priorities;
import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableIntResult;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.abilities.HpProperty;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.CharacterTargetedAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionBody;
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
        HpProperty hp = ActionUtils.tryGetHp(target);
        if (hp == null) {
            return UndoAction.DO_NOTHING;
        }

        int healAmount = hp.getMaxHp();
        return damageTarget(actor, target, -healAmount);
    };

    public static final TargetedAction<DamageSource, TargetableCharacter> RESTORES_HEALTH = (world, actor, target) -> {
        HpProperty hp = ActionUtils.tryGetHp(target);
        if (hp == null) {
            return UndoAction.DO_NOTHING;
        }

        int damage = hp.getCurrentHp() - hp.getMaxHp();
        if (damage >= 0) {
            return UndoAction.DO_NOTHING;
        }
        return ActionUtils.damageCharacter(actor, damage, target);
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

    public static final TargetedAction<Object, Minion> CHARGE = (world, actor, target) -> {
        return target.setCharge(true);
    };

    public static final TargetedAction<Object, Minion> STEALTH = (world, actor, target) -> {
        return target.getBody().setStealth(true);
    };

    public static final TargetedAction<Object, Minion> STEALTH_FOR_A_TURN = (world, actor, target) -> {
        return target.addAndActivateAbility(ActionUtils.toUntilTurnStartsAbility(world, target, (Minion self) -> {
            return self.getBody().getStealthProperty().addBuff((prev) -> true);
        }));
    };

    public static final TargetedAction<Object, Silencable> SILENCE = (world, actor, target) -> {
        return target.silence();
    };

    public static final TargetedAction<Object, TargetableCharacter> FREEZE_TARGET = (world, actor, target) -> {
        return target.getAttackTool().freeze();
    };

    public static final TargetedAction<Object, Minion> RETURN_MINION = returnMinion(0);

    public static final TargetedAction<PlayerProperty, Minion> TAKE_CONTROL = (world, actor, target) -> {
        return actor.getOwner().getBoard().takeOwnership(target);
    };

    public static final TargetedAction<PlayerProperty, Card> COPY_TARGET_CARD = (world, self, target) -> {
        Hand hand = self.getOwner().getHand();
        return hand.addCard(target.getCardDescr());
    };

    public static final TargetedAction<Object, Minion> SHUFFLE_MINION = (world, actor, minion) -> {
        Player owner = minion.getOwner();
        Deck deck = owner.getBoard().getDeck();
        CardDescr baseCard = minion.getBaseDescr().getBaseCard();

        UndoAction removeUndo = minion.getLocationRef().removeFromBoard();
        UndoAction shuffleUndo = deck.putToRandomPosition(world.getRandomProvider(), baseCard);
        return () -> {
            shuffleUndo.undo();
            removeUndo.undo();
        };
    };

    public static final TargetedAction<Object, Minion> ATTACK_HP_SWITCH = (World world, Object actor, Minion target) -> {
        MinionBody body = target.getBody();

        int attack = target.getAttackTool().getAttack();
        int hp = body.getCurrentHp();

        UndoAction attackUndo = target.getBuffableAttack().setValueTo(hp);
        UndoAction hpUndo = body.getHp().setMaxHp(attack);
        UndoAction currentHpUndo = body.getHp().setCurrentHp(body.getMaxHp());
        return () -> {
            currentHpUndo.undo();
            hpUndo.undo();
            attackUndo.undo();
        };
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

    public static <Actor, Target> TargetedAction<Actor, Target> doIf(
            @NamedArg("condition") TargetedActionCondition<? super Actor, ? super Target> condition,
            @NamedArg("if") TargetedAction<? super Actor, ? super Target> ifAction) {
        return doIf(condition, ifAction, TargetedAction.DO_NOTHING);
    }

    public static <Actor, Target> TargetedAction<Actor, Target> doIf(
            @NamedArg("condition") TargetedActionCondition<? super Actor, ? super Target> condition,
            @NamedArg("if") TargetedAction<? super Actor, ? super Target> ifAction,
            @NamedArg("else") TargetedAction<? super Actor, ? super Target> elseAction) {
        ExceptionHelper.checkNotNullArgument(condition, "condition");
        ExceptionHelper.checkNotNullArgument(ifAction, "ifAction");
        ExceptionHelper.checkNotNullArgument(elseAction, "elseAction");

        return (World world, Actor actor, Target target) -> {
            return condition.applies(world, actor, target)
                    ? ifAction.alterWorld(world, actor, target)
                    : elseAction.alterWorld(world, actor, target);
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
