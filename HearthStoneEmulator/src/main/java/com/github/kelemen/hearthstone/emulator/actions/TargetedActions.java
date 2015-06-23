package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.BoardLocationRef;
import com.github.kelemen.hearthstone.emulator.BornEntity;
import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.MultiTargeter;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.SummonLocationRef;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableIntResult;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionBody;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionProperties;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedActions {
    public static final TargetedAction FULL_HEAL = (World world, PlayTarget target) -> {
        Player player = target.getCastingPlayer();
        TargetableCharacter character = target.getTarget();
        if (character == null) {
            return UndoAction.DO_NOTHING;
        }

        int healAmount;
        if (character instanceof Minion) {
            healAmount = ((Minion)character).getBody().getMaxHp();
        }
        else if (character instanceof Hero) {
            healAmount = ((Hero)character).getMaxHp();
        }
        else {
            healAmount = 0;
        }

        return dealSpellDamage(player, character, -healAmount);
    };

    public static final CharacterTargetedAction SILENCE = (World world, TargetableCharacter target) -> {
        if (target instanceof Silencable) {
            return ((Silencable)target).silence();
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final CharacterTargetedAction TAUNT_MINION = (World world, TargetableCharacter target) -> {
        if (target instanceof Minion) {
            Minion minion = (Minion)target;
            return minion.getBody().setTaunt(true);
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final TargetedAction SHADOW_FLAME_DAMAGE = shadowFlameDamage();

    public static final CharacterTargetedAction KILL_TARGET = (World world, TargetableCharacter target) -> {
        return target.poison();
    };

    public static final CharacterTargetedAction FREEZE_TARGET = (World world, TargetableCharacter target) -> {
        return target.getAttackTool().freeze();
    };

    public static final CharacterTargetedAction STEALTH_FOR_A_TURN = (World world, TargetableCharacter target) -> {
        if (target instanceof Minion) {
            Minion minion = (Minion)target;
            return minion.addAndActivateAbility(ActionUtils.toUntilTurnStartsAbility(world, minion, (Minion self) -> {
                return self.getBody().getStealthProperty().addBuff((prev) -> true);
            }));
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final CharacterTargetedAction ATTACK_HP_SWITCH = (World world, TargetableCharacter target) -> {
        if (target instanceof Minion) {
            Minion minion = (Minion)target;
            MinionBody body = minion.getBody();

            int attack = minion.getAttackTool().getAttack();
            int hp = body.getCurrentHp();

            UndoAction attackUndo = minion.getBuffableAttack().setValueTo(hp);
            UndoAction hpUndo = body.getHp().setMaxHp(attack);
            return () -> {
                hpUndo.undo();
                attackUndo.undo();
            };
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final TargetedAction SHADOW_MADNESS = (World world, PlayTarget arg) -> {
        TargetableCharacter target = arg.getTarget();
        if (target instanceof Minion) {
            Player player = arg.getCastingPlayer();
            Minion minion = (Minion)target;
            return takeControlForThisTurn(player, minion);
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final TargetedAction HOLY_WRATH = (world, arg) -> {
        Player player = arg.getCastingPlayer();

        UndoableResult<CardDescr> cardRef = player.drawCardToHand();
        CardDescr card = cardRef.getResult();

        int damage = card != null ? card.getManaCost() : 0;
        UndoAction damageUndo = dealSpellDamage(arg, damage);

        return () -> {
            damageUndo.undo();
            cardRef.undo();
        };
    };

    private static TargetedAction damageAndDrawCard(
            boolean drawWhenDead,
            int damage) {
        return (world, target) -> {
            TargetableCharacter slamTarget = target.getTarget();
            if (slamTarget instanceof Minion) {
                UndoAction undoDamage = dealSpellDamage(target, damage);
                UndoAction undoDraw;
                if (((Minion)slamTarget).isDead() == drawWhenDead) {
                    undoDraw = target.getCastingPlayer().drawCardToHand();
                }
                else {
                    undoDraw = UndoAction.DO_NOTHING;
                }
                return () -> {
                    undoDraw.undo();
                    undoDamage.undo();
                };
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static TargetedAction mortalCoil(@NamedArg("damage") int damage) {
        return damageAndDrawCard(true, damage);
    }

    public static TargetedAction slam(@NamedArg("damage") int damage) {
        return damageAndDrawCard(false, damage);
    }

    public static TargetedAction dealBasicDamage(@NamedArg("damage") int damage) {
        return (world, target) -> {
            Player player = target.getCastingPlayer();
            TargetableCharacter character = target.getTarget();
            return character != null
                    ? character.damage(player.getBasicDamage(damage))
                    : UndoAction.DO_NOTHING;
        };
    }

    public static TargetedAction dealSpellDamage(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return (world, target) -> {
            int damage = world.getRandomProvider().roll(minDamage, maxDamage);
            return dealSpellDamage(target, damage);
        };
    }

    public static TargetedAction dealSpellDamage(@NamedArg("damage") int damage) {
        return (world, target) -> {
            return dealSpellDamage(target, damage);
        };
    }

    private static UndoableIntResult dealSpellDamage(PlayTarget target, int damage) {
        Player player = target.getCastingPlayer();
        TargetableCharacter character = target.getTarget();
        return character != null
                ? dealSpellDamage(player, character, damage)
                : UndoableIntResult.ZERO;
    }

    private static UndoableIntResult dealSpellDamage(Player player, TargetableCharacter target, int damage) {
        return target.damage(player.getSpellDamage(damage));
    }

    private static UndoableIntResult dealSpellDamage(PlayTarget target, int minDamage, int maxDamage) {
        Player player = target.getCastingPlayer();
        TargetableCharacter character = target.getTarget();
        return character != null
                ? dealSpellDamage(player, character, minDamage, maxDamage)
                : UndoableIntResult.ZERO;
    }

    private static UndoableIntResult dealSpellDamage(
            Player player,
            TargetableCharacter target,
            int minDamage,
            int maxDamage) {
        int damage = player.getWorld().getRandomProvider().roll(minDamage, maxDamage);
        return dealSpellDamage(player, target, damage);
    }

    private static TargetedAction shadowFlameDamage() {
        MultiTargeter.Builder targeterBuilder = new MultiTargeter.Builder();
        targeterBuilder.setEnemy(true);
        targeterBuilder.setMinions(true);
        MultiTargeter targeter = targeterBuilder.create();

        return (world, target) -> {
            Player player = target.getCastingPlayer();
            TargetableCharacter character = target.getTarget();
            if (character == null) {
                return UndoAction.DO_NOTHING;
            }

            int damage = character.getAttackTool().getAttack();

            Damage appliedDamage = player.getSpellDamage(damage);
            return targeter.forTargets(player, (damageTarget) -> damageTarget.damage(appliedDamage));
        };
    }

    public static TargetedAction implosion(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage,
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkArgumentInRange(minDamage, 0, maxDamage, "minDamage");
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        return (world, target) -> {
            TargetableCharacter character = target.getTarget();
            if (character == null) {
                return UndoAction.DO_NOTHING;
            }

            UndoBuilder result = new UndoBuilder();

            UndoableIntResult damageUndo = dealSpellDamage(target, minDamage, maxDamage);
            result.addUndo(damageUndo.getUndoAction());

            int damageDelt = damageUndo.getResult();
            Player player = target.getCastingPlayer();
            MinionDescr summonedMinion = minion.getMinion();
            for (int i = 0; i < damageDelt; i++) {
                result.addUndo(player.summonMinion(summonedMinion));
            }

            return result;
        };
    }

    private static ActivatableAbility<Minion> drawCardOnAttackAbility(World world, Player player, int priority) {
        return (Minion self) -> {
            WorldEvents events = world.getEvents();
            return events.attackListeners().addAction(priority, (World attackWorld, AttackRequest object) -> {
                return object.getAttacker() == self
                        ? player.drawCardToHand()
                        : UndoAction.DO_NOTHING;
            });
        };
    }

    public static TargetedAction blessingOfWisdom() {
        return (World world, PlayTarget target) -> {
            TargetableCharacter targetCharacter = target.getTarget();
            if (targetCharacter instanceof Minion) {
                Player player = target.getCastingPlayer();
                Minion minion = (Minion)targetCharacter;

                return minion.addAndActivateAbility(drawCardOnAttackAbility(world, player, WorldEvents.LOW_PRIORITY));
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static CharacterTargetedAction addTemporaryAttack(@NamedArg("attack") int attack) {
        return (World world, TargetableCharacter target) -> {
            if (target instanceof Minion) {
                Minion minion = (Minion)target;
                MinionProperties properties = minion.getProperties();
                return ActionUtils.doTemporary(world, () -> properties.addRemovableAttackBuff(attack));
            }
            else if (target instanceof Hero) {
                Hero hero = (Hero)target;
                return hero.addExtraAttackForThisTurn(attack);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static CharacterTargetedAction setMinionAttackTo(@NamedArg("attack") int attack) {
        return (World world, TargetableCharacter target) -> {
            if (target instanceof Minion) {
                Minion minion = (Minion)target;
                return minion.getBuffableAttack().setValueTo(attack);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static CharacterTargetedAction applyToMinionTarget(
            @NamedArg("action") MinionAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, TargetableCharacter target) -> {
            if (target instanceof Minion) {
                return action.alterWorld(world, (Minion)target);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private static Minion tryGetLeft(SummonLocationRef locationRef) {
        BoardLocationRef leftRef = locationRef.tryGetLeft();
        return leftRef != null ? leftRef.getMinion() : null;
    }

    private static Minion tryGetRight(SummonLocationRef locationRef) {
        BoardLocationRef leftRef = locationRef.tryGetRight();
        return leftRef != null ? leftRef.getMinion() : null;
    }

    public static TargetedAction swipeAction(
            @NamedArg("mainAction") TargetedAction mainAction,
            @NamedArg("othersAction") TargetedAction othersAction) {
        ExceptionHelper.checkNotNullArgument(mainAction, "mainAction");
        ExceptionHelper.checkNotNullArgument(othersAction, "othersAction");

        return (World world, PlayTarget target) -> {
            TargetableCharacter targetCharacter = target.getTarget();
            if (targetCharacter == null) {
                return UndoAction.DO_NOTHING;
            }

            List<TargetableCharacter> targets = new ArrayList<>(Player.MAX_BOARD_SIZE + 1);
            ActionUtils.collectTargets(targetCharacter.getOwner(), targets);

            UndoBuilder result = new UndoBuilder(targets.size());
            result.addUndo(mainAction.alterWorld(world, target));

            BornEntity.sortEntities(targets);
            for (TargetableCharacter otherTarget: targets) {
                if (otherTarget != targetCharacter) {
                    PlayTarget otherPlayTarget = new PlayTarget(target.getCastingPlayer(), otherTarget);
                    result.addUndo(othersAction.alterWorld(world, otherPlayTarget));
                }
            }

            return result;
        };
    }

    public static TargetedAction applyToAdjacentTargets(
            @NamedArg("mainAction") TargetedAction mainAction,
            @NamedArg("sideAction") TargetedAction sideAction) {
        ExceptionHelper.checkNotNullArgument(mainAction, "mainAction");
        ExceptionHelper.checkNotNullArgument(sideAction, "sideAction");

        return (World world, PlayTarget target) -> {
            TargetableCharacter targetCharacter = target.getTarget();
            if (!(targetCharacter instanceof Minion)) {
                return mainAction.alterWorld(world, target);
            }

            Minion minion = (Minion)targetCharacter;
            SummonLocationRef locationRef = minion.getLocationRef();
            Minion left = tryGetLeft(locationRef);
            Minion right = tryGetRight(locationRef);

            UndoBuilder result = new UndoBuilder(3);
            result.addUndo(mainAction.alterWorld(world, target));

            if (right != null) {
                PlayTarget rightTarget = new PlayTarget(target.getCastingPlayer(), right);
                result.addUndo(sideAction.alterWorld(world, rightTarget));
            }
            if (left != null) {
                PlayTarget leftTarget = new PlayTarget(target.getCastingPlayer(), left);
                result.addUndo(sideAction.alterWorld(world, leftTarget));
            }
            return result;
        };
    }

    public static TargetedAction applyToAdjacentTargets(
            @NamedArg("action") TargetedAction action) {
        return applyToAdjacentTargets(action, action);
    }

    public static TargetedAction applyIfFrozen(
            @NamedArg("action") TargetedAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, PlayTarget target) -> {
            TargetableCharacter targetCharacter = target.getTarget();
            if (targetCharacter == null || !targetCharacter.getAttackTool().isFrozen()) {
                return UndoAction.DO_NOTHING;
            }
            return action.alterWorld(world, target);
        };
    }

    public static CharacterTargetedAction applyToTargetOwner(
            @NamedArg("action") PlayerAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, TargetableCharacter target) -> {
            return action.alterWorld(world, target.getOwner());
        };
    }

    public static CharacterTargetedAction buffMinion(
            @NamedArg("ability") ActivatableAbility<? super Minion> ability) {
        ExceptionHelper.checkNotNullArgument(ability, "ability");

        return (World world, TargetableCharacter target) -> {
            if (target instanceof Minion) {
                Minion minion = (Minion)target;
                return minion.addAndActivateAbility(ability);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private static boolean isDemon(TargetableCharacter target) {
        return target.getKeywords().contains(Keywords.RACE_DEMON);
    }

    public static TargetedAction demonBuff(@NamedArg("buff") int buff) {
        CharacterTargetedAction buffAction = applyToMinionTarget(MinionActions.buff(buff, buff));
        TargetedAction damageAction = dealSpellDamage(buff);

        return (World world, PlayTarget arg) -> {
            TargetableCharacter target = arg.getTarget();
            if (target.getOwner() == arg.getCastingPlayer() && isDemon(target)) {
                return buffAction.alterWorld(world, target);
            }
            else {
                return damageAction.alterWorld(world, arg);
            }
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

    public static TargetedAction damageAndSummonOnDeath(
            @NamedArg("damage") int damage,
            @NamedArg("keywords") Keyword[] keywords) {

        Function<World, MinionDescr> minionProvider = ActionUtils.randomMinionProvider(keywords);
        TargetedAction damageAction = dealSpellDamage(damage);

        return (World world, PlayTarget arg) -> {
            UndoAction damageUndo = damageAction.alterWorld(world, arg);
            if (arg.getTarget().isDead()) {
                MinionDescr minion = minionProvider.apply(world);
                if (minion == null) {
                    return damageUndo;
                }

                UndoAction summonUndo = arg.getCastingPlayer().summonMinion(minion);
                return () -> {
                    summonUndo.undo();
                    damageUndo.undo();
                };
            }
            else {
                return damageUndo;
            }
        };
    }

    private TargetedActions() {
        throw new AssertionError();
    }
}
