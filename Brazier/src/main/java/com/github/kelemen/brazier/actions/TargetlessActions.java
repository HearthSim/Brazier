package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.BornEntity;
import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.ManaResource;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.PlayerAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventFilter;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardProvider;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionBody;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponDescr;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jtrim.utils.ExceptionHelper;

import static com.github.kelemen.hearthstone.emulator.actions.PlayerActions.dealDamageToEnemyTargets;

public final class TargetlessActions {
    public static final TargetlessAction<PlayerProperty> DRAW_FOR_SELF = (World world, PlayerProperty actor) -> {
        return actor.getOwner().drawCardToHand();
    };

    public static final TargetlessAction<PlayerProperty> DRAW_FOR_OPPONENT = actWithOpponent(DRAW_FOR_SELF);

    public static final TargetlessAction<Minion> RESUMMON_RIGHT = (World world, Minion minion) -> {
        return minion.getLocationRef().summonRight(minion.getBaseDescr());
    };

    public static final TargetlessAction<PlayerProperty> DESTROY_OPPONENTS_WEAPON = (world, actor) -> {
        return actor.getOwner().destroyWeapon();
    };

    public static final TargetlessAction<PlayerProperty> REDUCE_OPPONENTS_WEAPON_DURABILITY = reduceOpponentsWeaponDurability(1);

    public static final TargetlessAction<Minion> SWAP_WITH_MINION_IN_HAND = (World world, Minion actor) -> {
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

    public static TargetlessAction<PlayerProperty> SUMMON_RANDOM_MINION_FROM_HAND = (world, actor) -> {
        Player player = actor.getOwner();
        Hand hand = player.getHand();
        int cardIndex = hand.chooseRandomCardIndex((card) -> card.isMinionCard());
        if (cardIndex < 0) {
            return UndoAction.DO_NOTHING;
        }

        UndoableResult<Card> removedCardRef = hand.removeAtIndex(cardIndex);
        Minion minion = removedCardRef.getResult().getMinion();
        assert minion != null;

        UndoAction summonUndo = player.summonMinion(minion);
        return () -> {
            summonUndo.undo();
            removedCardRef.undo();
        };
    };

    public static final TargetlessAction<TargetableCharacter> SELF_DESTRUCT = (world, actor) -> {
        return actor.poison();
    };

    public static final TargetlessAction<PlayerProperty> BATTLE_RAGE = (world, actor) -> {
        Player player = actor.getOwner();
        int cardsToDraw = player.getHero().isDamaged() ? 1 : 0;
        cardsToDraw += player.getBoard().countMinions(Minion::isDamaged);

        UndoBuilder result = new UndoBuilder(cardsToDraw);
        for (int i = 0; i < cardsToDraw; i++) {
            result.addUndo(player.drawCardToHand());
        }
        return result;
    };

    public static final TargetlessAction<PlayerProperty> BLADE_FLURRY = (World world, PlayerProperty actor) -> {
        Player player = actor.getOwner();
        if (player.tryGetWeapon() == null) {
            return UndoAction.DO_NOTHING;
        }

        int damage = player.getHero().getAttackTool().getAttack();

        UndoAction destroyUndo = player.destroyWeapon();
        UndoAction damageUndo = dealDamageToEnemyTargets(damage).alterWorld(world, player);
        return () -> {
            damageUndo.undo();
            destroyUndo.undo();
        };
    };

    public static final TargetlessAction<Minion> EAT_DIVINE_SHIELDS = eatDivineShields(3, 3);

    public static final TargetlessAction<Object> BRAWL = (world, actor) -> {
        List<Minion> minions = new ArrayList<>(2 * Player.MAX_BOARD_SIZE);
        world.getPlayer1().getBoard().collectMinions(minions);
        world.getPlayer2().getBoard().collectMinions(minions);

        // TODO: Brawler shouldn't be a keyword because keywords cannot be silenced.
        List<Minion> brawlers = minions.stream()
                .filter((minion) -> minion.getKeywords().contains(Keywords.BRAWLER))
                .collect(Collectors.toList());

        Minion winner = brawlers.isEmpty()
                ? ActionUtils.pickRandom(world, minions)
                : ActionUtils.pickRandom(world, brawlers);

        UndoBuilder result = new UndoBuilder();
        for (Minion minion: minions) {
            if (minion != winner) {
                result.addUndo(minion.poison());
            }
        }
        return result;
    };

    public static TargetlessAction<Object> withMinion(
            @NamedArg("action") TargetlessAction<? super Minion> action) {
        return applyToMinionAction(action);
    }

    public static <Actor> TargetlessAction<Actor> forSelf(
            @NamedArg("action") TargetedAction<? super Actor, ? super Actor> action) {
        return forTargets(EntitySelectors.self(), action);
    }

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
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Target> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        return forBornTargets(selector, action, false);
    }

    public static <Actor, Target extends BornEntity> TargetlessAction<Actor> forBornTargets(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Target> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action,
            @NamedArg("atomic") boolean atomic) {
        return forTargets(EntitySelectors.sorted(selector, BornEntity.CMP), action, atomic);
    }

    public static <Actor, Target> TargetlessAction<Actor> forTargets(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Target> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        return forTargets(selector, action, false);
    }

    public static <Actor, Target> TargetlessAction<Actor> forTargets(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Target> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action,
            @NamedArg("atomic") boolean atomic) {
        ExceptionHelper.checkNotNullArgument(selector, "targets");
        ExceptionHelper.checkNotNullArgument(action, "action");

        TargetlessAction<Actor> resultAction = (World world, Actor actor) -> {
            UndoBuilder result = new UndoBuilder();
            selector.select(world, actor).forEach((Target target) -> {
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

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> actWithOpponent(
            @NamedArg("action") TargetlessAction<? super Player> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor actor) -> {
            return action.alterWorld(world, actor.getOwner().getOpponent());
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> equipWeapon(
            @NamedArg("weapon") WeaponProvider weapon) {
        ExceptionHelper.checkNotNullArgument(weapon, "weapon");
        return (World world, Actor actor) -> {
            Player player = actor.getOwner();
            return player.equipWeapon(weapon.getWeapon());
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> equipSelectedWeapon(
            @NamedArg("weapon") EntitySelector<? super Actor, ? extends WeaponDescr> weapon) {
        ExceptionHelper.checkNotNullArgument(weapon, "weapon");
        return (World world, Actor actor) -> {
            Player player = actor.getOwner();
            // Equip the first weapon, since equiping multiple weapons make no sense.
            WeaponDescr toEquip = weapon.select(world, actor).findFirst().orElse(null);
            return toEquip != null
                    ? player.equipWeapon(toEquip)
                    : UndoAction.DO_NOTHING;
        };
    }

    public static TargetlessAction<Minion> summonMinionRight(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return (World world, Minion actor) -> {
            return actor.getLocationRef().summonRight(minion.getMinion());
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> summonMinion(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return (World world, Actor actor) -> {
            Player player = actor.getOwner();
            return player.summonMinion(minion.getMinion());
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> summonSelectedMinion(
            @NamedArg("minion") EntitySelector<? super Actor, ? extends MinionDescr> minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return (World world, Actor actor) -> {
            Player player = actor.getOwner();
            return minion.forEach(world, actor, (toSummon) -> player.summonMinion(toSummon));
        };
    }

    public static TargetlessAction<DamageSource> damageTarget(
            @NamedArg("selector") EntitySelector<DamageSource, TargetableCharacter> selector,
            @NamedArg("damage") int damage) {
        return damageTarget(selector, damage, damage);
    }

    public static <Actor extends DamageSource> TargetlessAction<Actor> damageTarget(
            @NamedArg("selector") EntitySelector<Actor, ? extends TargetableCharacter> selector,
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return forTargets(selector, TargetedActions.damageTarget(minDamage, maxDamage));
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addManaCrystal(@NamedArg("amount") int amount) {
        return addManaCrystal(true, amount);
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addManaCrystal(
            @NamedArg("empty") boolean empty,
            @NamedArg("amount") int amount) {
        return (World world, Actor actor) -> {
            ManaResource manaResource = actor.getOwner().getManaResource();
            UndoAction crystalUndo = manaResource.setManaCrystals(Math.max(0, manaResource.getManaCrystals() + amount));
            if (empty) {
                return crystalUndo;
            }
            UndoAction manaUndo = manaResource.setMana(manaResource.getMana() + amount);
            return () -> {
                manaUndo.undo();
                crystalUndo.undo();
            };
        };
    }

    public static <Actor extends DamageSource> TargetlessAction<Actor> dealMissleDamage(
            @NamedArg("missleCount") int missleCount) {
        return dealMissleDamage(
                EntitySelectors.filtered(EntityFilters.random(), EntitySelectors.enemyTargetsAlive()),
                missleCount);
    }

    public static <Actor extends DamageSource> TargetlessAction<Actor> dealMissleDamage(
            @NamedArg("selector") EntitySelector<Actor, ? extends TargetableCharacter> selector,
            @NamedArg("missleCount") int missleCount) {
        ExceptionHelper.checkNotNullArgument(selector, "selector");

        return (World world, Actor actor) -> {
            UndoableResult<Damage> missleCountRef = actor.createDamage(missleCount);
            int appliedMissleCount = missleCountRef.getResult().getAttack();

            UndoBuilder result = new UndoBuilder(appliedMissleCount + 1);
            result.addUndo(missleCountRef.getUndoAction());

            Damage damage = new Damage(actor, 1);
            Consumer<TargetableCharacter> damageAction = (target) -> {
                result.addUndo(target.damage(damage));
            };
            for (int i = 0; i < appliedMissleCount; i++) {
                selector.select(world, actor).forEach(damageAction);
            }
            return result;
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addCard(
            @NamedArg("card") CardProvider card) {
        return addCard(false, card);
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addCard(
            @NamedArg("delay") boolean delay,
            @NamedArg("card") CardProvider card) {
        return addSelectedCard(delay, (World world, Actor actor) -> Stream.of(card.getCard()));
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addSelectedCard(
            @NamedArg("card") EntitySelector<? super Actor, ? extends CardDescr> card) {
        return addSelectedCard(false, card);
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addSelectedCard(
            @NamedArg("delay") boolean delay,
            @NamedArg("card") EntitySelector<? super Actor, ? extends CardDescr> card) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        return (World world, Actor actor) -> {
            if (!delay) {
                return addCards(world, actor, card);
            }

            return ActionUtils.doOnEndOfTurn(world, () -> {
                return addCards(world, actor, card);
            });
        };
    }

    private static <Actor extends PlayerProperty> UndoAction addCards(
            World world,
            Actor actor,
            EntitySelector<? super Actor, ? extends CardDescr> card) {
        Hand hand = actor.getOwner().getHand();
        return card.forEach(world, actor, hand::addCard);
    }

    public static TargetlessAction<Object> buffSelfMinion(
            @NamedArg("buff") PermanentBuff<? super Minion> buff) {
        TargetlessAction<Minion> buffAction = forSelf(TargetedActions.buffTarget(buff));
        return applyToMinionAction(buffAction);
    }

    public static TargetlessAction<Object> buffSelfMinionThisTurn(
            @NamedArg("buff") Buff<? super Minion> buff) {
        TargetlessAction<Minion> buffAction = forSelf(TargetedActions.buffTargetThisTurn(buff));
        return applyToMinionAction(buffAction);
    }

    private static TargetlessAction<Object> applyToMinionAction(TargetlessAction<? super Minion> buffAction) {
        return (World world, Object actor) -> {
            return applyToMinion(world, actor, buffAction);
        };
    }

    private static UndoAction applyToMinion(World world, Object actor, TargetlessAction<? super Minion> buffAction) {
        Minion minion = ActionUtils.tryGetMinion(actor);
        return minion != null ? buffAction.alterWorld(world, minion) : UndoAction.DO_NOTHING;
    }

    public static TargetlessAction<PlayerProperty> armorUp(@NamedArg("armor") int armor) {
        return (world, actor) -> {
            return actor.getOwner().getHero().armorUp(armor);
        };
    }

    private static UndoAction rollRandomTotem(World world, Player player, MinionProvider[] totems) {
        Map<MinionId, MinionDescr> allowedMinions = new HashMap<>();
        for (MinionProvider minionProvider: totems) {
            MinionDescr minion = minionProvider.getMinion();
            allowedMinions.put(minion.getId(), minion);
        }
        for (Minion minion: player.getBoard().getAliveMinions()) {
            allowedMinions.remove(minion.getBaseDescr().getId());
        }

        int allowedCount = allowedMinions.size();
        if (allowedCount == 0) {
            return UndoAction.DO_NOTHING;
        }

        int totemIndex = world.getRandomProvider().roll(allowedCount);

        Iterator<MinionDescr> minionItr = allowedMinions.values().iterator();
        MinionDescr selected = minionItr.next();
        for (int i = 0; i < totemIndex; i++) {
            selected = minionItr.next();
        }

        return player.summonMinion(selected);
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> summonRandomTotem(
            @NamedArg("totems") MinionProvider... totems) {
        MinionProvider[] totemsCopy = totems.clone();
        ExceptionHelper.checkNotNullElements(totemsCopy, "totems");

        return (world, actor) -> {
            return rollRandomTotem(world, actor.getOwner(), totemsCopy);
        };
    }

    public static TargetlessAction<Minion> eatDivineShields(
            @NamedArg("attackPerShield") int attackPerShield,
            @NamedArg("hpPerShield") int hpPerShield) {
        return (World world, Minion actor) -> {
            AtomicInteger shieldCountRef = new AtomicInteger(0);
            Function<Minion, UndoAction> collector = (Minion minion) -> {
                MinionBody body = minion.getBody();
                if (body.isDivineShield()) {
                    shieldCountRef.incrementAndGet();
                    return body.setDivineShield(false);
                }
                else {
                    return UndoAction.DO_NOTHING;
                }
            };

            UndoAction collect1Undo = world.getPlayer1().getBoard().forAllMinions(collector);
            UndoAction collect2Undo = world.getPlayer2().getBoard().forAllMinions(collector);
            int shieldCount = shieldCountRef.get();
            if (shieldCount <= 0) {
                return UndoAction.DO_NOTHING;
            }

            UndoBuilder result = new UndoBuilder();
            result.addUndo(collect1Undo);
            result.addUndo(collect2Undo);

            result.addUndo(actor.getBuffableAttack().addBuff(attackPerShield * shieldCount));
            result.addUndo(actor.getBody().getHp().buffHp(hpPerShield * shieldCount));

            return result;
        };
    }

    public static TargetlessAction<PlayerProperty> reduceOpponentsWeaponDurability(@NamedArg("amount") int amount) {
        ExceptionHelper.checkArgumentInRange(amount, 1, Integer.MAX_VALUE, "amount");

        return (World world, PlayerProperty actor) -> {
            Weapon weapon = actor.getOwner().getOpponent().tryGetWeapon();
            if (weapon == null) {
                return UndoAction.DO_NOTHING;
            }

            if (amount == 1) {
                return weapon.decreaseCharges();
            }

            UndoBuilder result = new UndoBuilder(amount);
            for (int i = 0; i < amount; i++) {
                result.addUndo(weapon.decreaseCharges());
            }
            return result;
        };
    }

    public static TargetlessAction<DamageSource> bouncingBlade(
            @NamedArg("maxBounces") int maxBounces,
            @NamedArg("baseDamage") int baseDamage) {

        Predicate<Minion> minionFilter = (minion) -> {
            MinionBody body = minion.getBody();
            int currentHp = body.getCurrentHp();
            return currentHp > 0 && !body.isImmune() && body.getMinHpProperty().getValue() < currentHp;
        };

        return (World world, DamageSource actor) -> {
            UndoBuilder result = new UndoBuilder();

            UndoableResult<Damage> damageRef = actor.createDamage(baseDamage);
            result.addUndo(damageRef.getUndoAction());

            List<Minion> targets = new ArrayList<>();
            for (int i = 0; i < maxBounces; i++) {
                targets.clear();
                world.getPlayer1().getBoard().collectMinions(targets, minionFilter);
                world.getPlayer2().getBoard().collectMinions(targets, minionFilter);

                Minion selected = ActionUtils.pickRandom(world, targets);
                if (selected == null) {
                    break;
                }

                result.addUndo(selected.damage(damageRef.getResult()));
                if (selected.getBody().getCurrentHp() <= 0) {
                    break;
                }
            }
            return result;
        };
    }

    public static TargetlessAction<PlayerProperty> drawCard(@NamedArg("costReduction") int costReduction) {
        return drawCard(costReduction, WorldEventFilter.ANY);
    }

    public static TargetlessAction<PlayerProperty> drawCard(
            @NamedArg("costReduction") int costReduction,
            @NamedArg("costReductionFilter") WorldEventFilter<? super Player, ? super Card> costReductionFilter) {
        ExceptionHelper.checkNotNullArgument(costReductionFilter, "costReductionFilter");
        if (costReduction == 0) {
            return TargetlessActions.DRAW_FOR_SELF;
        }

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            UndoableResult<Card> cardRef = player.drawFromDeck();
            Card card = cardRef.getResult();
            if (card == null) {
                return cardRef.getUndoAction();
            }

            if (costReductionFilter.applies(world, player, card)) {
                card.decreaseManaCost(costReduction);
            }
            UndoAction addUndo = player.drawCardToHand(card);
            return () -> {
                addUndo.undo();
                cardRef.undo();
            };
        };
    }

    private TargetlessActions() {
        throw new AssertionError();
    }
}
