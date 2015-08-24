package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Deck;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
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
import com.github.kelemen.hearthstone.emulator.abilities.AuraAwareIntProperty;
import com.github.kelemen.hearthstone.emulator.abilities.HpProperty;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
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

    public static final TargetedAction<Object, Minion> TRIGGER_DEATHRATTLE = (world, actor, target) -> {
        return target.triggetDeathRattles();
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

    public static TargetedAction<Minion, Minion> COPY_OTHER_MINION = (world, actor, target) -> {
        return actor.copyOther(target);
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

    public static final TargetedAction<DamageSource, TargetableCharacter> SAVAGERY = (world, actor, target) -> {
        int damage = actor.getOwner().getHero().getAttackTool().getAttack();
        return damageTarget(actor, target, damage);
    };

    public static final TargetedAction<DamageSource, TargetableCharacter> SHIELD_SLAM = (world, actor, target) -> {
        int damage = actor.getOwner().getHero().getCurrentArmor();
        return damageTarget(actor, target, damage);
    };

    public static final TargetedAction<PlayerProperty, Minion> SHADOW_MADNESS = (world, actor, target) -> {
        return takeControlForThisTurn(actor.getOwner(), target);
    };

    public static final TargetedAction<DamageSource, TargetableCharacter> HOLY_WRATH = (world, actor, target) -> {
        Player player = actor.getOwner();

        UndoableResult<Card> cardRef = player.drawCardToHand();
        Card card = cardRef.getResult();

        int damage = card != null ? card.getCardDescr().getManaCost() : 0;
        UndoAction damageUndo = damageTarget(actor, target, damage);

        return () -> {
            damageUndo.undo();
            cardRef.undo();
        };
    };

    public static final TargetedAction<Object, Minion> WIND_FURY = windFury(2);

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

    public static final TargetedAction<Object, Minion> RECOMBOBULATE = transformMinion((Minion target) -> {
        World world = target.getWorld();

        int manaCost = target.getBaseDescr().getBaseCard().getManaCost();
        Keyword manaCostKeyword = Keywords.manaCost(manaCost);
        List<CardDescr> possibleMinions = world.getDb().getCardDb().getByKeywords(Keywords.MINION, manaCostKeyword);
        CardDescr selected = ActionUtils.pickRandom(world, possibleMinions);
        if (selected == null) {
            return null;
        }

        MinionDescr result = selected.getMinion();
        if (result == null) {
            throw new IllegalStateException("Minion keyword was appied to a non-minion card: " + selected.getId());
        }
        return result;
    });

    public static final TargetedAction<Minion, Minion> SWAP_HP_WITH_TARGET = (World world, Minion actor, Minion target) -> {
        HpProperty targetHpProperty = target.getBody().getHp();
        HpProperty ourHpProperty = actor.getBody().getHp();

        int targetHp = targetHpProperty.getCurrentHp();
        int ourHp = ourHpProperty.getCurrentHp();

        UndoAction targetHpUndo = targetHpProperty.setMaxAndCurrentHp(ourHp);
        UndoAction ourHpUndo = ourHpProperty.setMaxAndCurrentHp(targetHp);
        return () -> {
            ourHpUndo.undo();
            targetHpUndo.undo();
        };
    };

    public static final TargetedAction<Object, Minion> DESTROY_STEALTH = (world, actor, target) -> {
        return target.getBody().setStealth(false);
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

    public static <Actor, Target> TargetedAction<Actor, Target> doAtomic(
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor actor, Target target) -> {
            return world.getEvents().doAtomic(() -> action.alterWorld(world, actor, target));
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

    public static TargetedAction<DamageSource, TargetableCharacter> implosion(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage,
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkArgumentInRange(minDamage, 0, maxDamage, "minDamage");
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        return (world, actor, target) -> {
            UndoBuilder result = new UndoBuilder();

            int damage = world.getRandomProvider().roll(minDamage, maxDamage);
            UndoableResult<Damage> damageRef = actor.createDamage(damage);
            result.addUndo(damageRef.getUndoAction());

            UndoableIntResult damageUndo = target.damage(damageRef.getResult());
            result.addUndo(damageUndo.getUndoAction());

            int damageDelt = damageUndo.getResult();
            Player player = actor.getOwner();
            MinionDescr summonedMinion = minion.getMinion();
            for (int i = 0; i < damageDelt; i++) {
                result.addUndo(player.summonMinion(summonedMinion));
            }

            return result;
        };
    }

    public static TargetedAction<Object, TargetableCharacter> multiplyHp(@NamedArg("mul") int mul) {
        Function<HpProperty, UndoAction> buffAction = (hp) -> hp.buffHp((mul - 1) * hp.getCurrentHp());
        return (World world, Object actor, TargetableCharacter target) -> {
            return ActionUtils.adjustHp(target, buffAction);
        };
    }

    public static TargetedAction<Object, Card> copyTargetToHand(@NamedArg("copyCount") int copyCount) {
        return (World world, Object actor, Card target) -> {
            CardDescr baseCard = target.getCardDescr();
            Hand hand = target.getOwner().getHand();

            UndoBuilder result = new UndoBuilder(copyCount);
            for (int i = 0; i < copyCount; i++) {
                result.addUndo(hand.addCard(baseCard));
            }
            return result;
        };
    }

    public static TargetedAction<Object, Card> decreaseCostOfTarget(@NamedArg("amount") int amount) {
        return (World world, Object actor, Card target) -> {
            return target.decreaseManaCost(amount);
        };
    }

    public static <Actor, Target> TargetedAction<Actor, Target> randomAction(
            @NamedArg("actions") TargetedAction<? super Actor, ? super Target>[] actions) {
        TargetedAction<? super Actor, ? super Target>[] actionsCopy = actions.clone();
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");
        ExceptionHelper.checkArgumentInRange(actionsCopy.length, 1, Integer.MAX_VALUE, "actions.length");

        return (World world, Actor actor, Target target) -> {
            TargetedAction<? super Actor, ? super Target> selected = ActionUtils.pickRandom(world, actionsCopy);
            return selected.alterWorld(world, actor, target);
        };
    }

    public static <Actor, Target> TargetedAction<Actor, Target> combine(
            @NamedArg("actions") TargetedAction<Actor, Target>[] actions) {
        return TargetedAction.merge(Arrays.asList(actions));
    }

    public static TargetedAction<Object, Minion> windFury(@NamedArg("attackCount") int attackCount) {
        return (World world, Object actor, Minion target) -> {
            AuraAwareIntProperty maxAttackCount = target.getProperties().getMaxAttackCountProperty();
            return maxAttackCount.addAuraBuff((prev) -> Math.max(prev, attackCount));
        };
    }


    private static UndoAction takeControlForThisTurn(Player newOwner, Minion minion) {
        World world = newOwner.getWorld();
        return minion.addAndActivateAbility(ActionUtils.toSingleTurnAbility(world, (Minion self) -> {
            Player originalOwner = self.getOwner();
            UndoAction takeOwnUndo = newOwner.getBoard().takeOwnership(self);
            UndoAction refreshUndo = self.refresh();

            return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
                @Override
                public UndoAction unregister() {
                    // We must not return this minion to its owner,
                    // if we are disabling this ability before destroying the
                    // minion.
                    if (self.isScheduledToDestroy()) {
                        return UndoAction.DO_NOTHING;
                    }
                    return originalOwner.getBoard().takeOwnership(self);
                }

                @Override
                public void undo() {
                    refreshUndo.undo();
                    takeOwnUndo.undo();
                }
            });
        }));
    }

    public static <Actor extends DamageSource> TargetedAction<Actor, TargetableCharacter> shadowFlameDamage(
            @NamedArg("selector") EntitySelector<Actor, ? extends TargetableCharacter> selector) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");
        return (World world, Actor actor, TargetableCharacter target) -> {
            int damage = target.getAttackTool().getAttack();
            TargetlessAction<Actor> damageAction = TargetlessActions.damageTarget(selector, damage);
            return damageAction.alterWorld(world, actor);
        };
    }

    public static TargetedAction<Object, Minion> resummonMinionWithHp(@NamedArg("hp") int hp) {
        return (World world, Object actor, Minion target) -> {
            Minion newMinion = new Minion(target.getOwner(), target.getBaseDescr());

            UndoAction summonUndo = target.getLocationRef().summonRight(newMinion);
            UndoAction updateHpUndo = newMinion.getProperties().getBody().getHp().setCurrentHp(1);
            return () -> {
                updateHpUndo.undo();
                summonUndo.undo();
            };
        };
    }

    public static <Actor> TargetedAction<Actor, Minion> reincarnate(
            @NamedArg("newMinionAction") TargetedAction<? super Actor, ? super Minion> newMinionAction) {
        ExceptionHelper.checkNotNullArgument(newMinionAction, "newMinionAction");

        return (World world, Actor actor, Minion target) -> {
            Player owner = target.getOwner();

            UndoBuilder result = new UndoBuilder();
            result.addUndo(target.poison());
            result.addUndo(world.endPhase());

            Minion newMinion = new Minion(owner, target.getBaseDescr());
            result.addUndo(owner.summonMinion(newMinion));
            result.addUndo(newMinionAction.alterWorld(world, actor, newMinion));

            return result;
        };
    }

    private TargetedActions() {
        throw new AssertionError();
    }
}
