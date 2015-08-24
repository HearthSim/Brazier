package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.BoardLocationRef;
import com.github.kelemen.brazier.BoardSide;
import com.github.kelemen.brazier.BornEntity;
import com.github.kelemen.brazier.CardPlayEvent;
import com.github.kelemen.brazier.Damage;
import com.github.kelemen.brazier.DamageSource;
import com.github.kelemen.brazier.Deck;
import com.github.kelemen.brazier.EntityId;
import com.github.kelemen.brazier.Hand;
import com.github.kelemen.brazier.Hero;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.LabeledEntity;
import com.github.kelemen.brazier.ManaResource;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.RandomProvider;
import com.github.kelemen.brazier.Secret;
import com.github.kelemen.brazier.SecretContainer;
import com.github.kelemen.brazier.SummonLocationRef;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.UndoableResult;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.abilities.Aura;
import com.github.kelemen.brazier.abilities.AuraFilter;
import com.github.kelemen.brazier.abilities.AuraTargetProvider;
import com.github.kelemen.brazier.abilities.Auras;
import com.github.kelemen.brazier.abilities.Buff;
import com.github.kelemen.brazier.abilities.CardAuras;
import com.github.kelemen.brazier.abilities.TargetedActiveAura;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.cards.CardId;
import com.github.kelemen.brazier.cards.CardProvider;
import com.github.kelemen.brazier.cards.CardType;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.minions.MinionBody;
import com.github.kelemen.brazier.minions.MinionDescr;
import com.github.kelemen.brazier.minions.MinionId;
import com.github.kelemen.brazier.minions.MinionProvider;
import com.github.kelemen.brazier.parsing.NamedArg;
import com.github.kelemen.brazier.weapons.Weapon;
import com.github.kelemen.brazier.weapons.WeaponDescr;
import com.github.kelemen.brazier.weapons.WeaponProvider;
import java.util.ArrayList;
import java.util.Arrays;
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

public final class TargetlessActions {
    public static final TargetlessAction<PlayerProperty> DRAW_FOR_SELF = (World world, PlayerProperty actor) -> {
        return actor.getOwner().drawCardToHand();
    };

    public static final TargetlessAction<PlayerProperty> DRAW_FOR_OPPONENT = actWithOpponent(DRAW_FOR_SELF);

    public static final TargetlessAction<PlayerProperty> DISCARD_RANDOM_CARD = (world, actor) -> {
        Player player = actor.getOwner();
        Hand hand = player.getHand();
        int cardCount = hand.getCardCount();
        if (cardCount == 0) {
            return UndoAction.DO_NOTHING;
        }

        int cardIndex = world.getRandomProvider().roll(cardCount);
        // TODO: Show discarded card to the opponent.
        return hand.removeAtIndex(cardIndex);
    };

    public static final TargetlessAction<PlayerProperty> DISCARD_FROM_DECK = (world, actor) -> {
        Player player = actor.getOwner();
        Deck deck = player.getBoard().getDeck();
        if (deck.getNumberOfCards() <= 0) {
            return UndoAction.DO_NOTHING;
        }

        UndoableResult<Card> cardRef = deck.tryDrawOneCard();
        // TODO: Show discarded card to the opponent.
        return cardRef != null ? cardRef.getUndoAction() : UndoAction.DO_NOTHING;
    };

    public static final TargetlessAction<Minion> RESUMMON_RIGHT = (World world, Minion minion) -> {
        return minion.getLocationRef().summonRight(minion.getBaseDescr());
    };

    public static final TargetlessAction<PlayerProperty> RESURRECT_DEAD_MINIONS = (world, actor) -> {
        Player player = actor.getOwner();
        List<Minion> deadMinions = player.getBoard().getGraveyard().getMinionsDiedThisTurn();
        if (deadMinions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder(deadMinions.size());
        for (Minion minion: deadMinions) {
            result.addUndo(player.summonMinion(minion.getBaseDescr()));
        }
        return result;
    };

    public static final TargetlessAction<PlayerProperty> DESTROY_OPPONENTS_WEAPON = (world, actor) -> {
        return actor.getOwner().destroyWeapon();
    };

    public static final TargetlessAction<PlayerProperty> DESTROY_OPPONENT_SECRET = (world, actor) -> {
        Player player = actor.getOwner();
        SecretContainer secrets = player.getOpponent().getSecrets();
        return secrets.removeAllSecrets();
    };

    public static final TargetlessAction<PlayerProperty> DISCARD_HAND = (world, actor) -> {
        Player player = actor.getOwner();
        // TODO: Display discarded cards to opponent
        return player.getHand().discardAll();
    };

    public static final TargetlessAction<PlayerProperty> REDUCE_WEAPON_DURABILITY = reduceWeaponDurability(1);

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

    public static final TargetlessAction<PlayerProperty> SUMMON_RANDOM_MINION_FROM_DECK = summonRandomMinionFromDeck(null);

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

    public static final TargetlessAction<Minion> COPY_SELF = (world, actor) -> {
        Player owner = actor.getOwner();
        if (owner.getBoard().isFull()) {
            return UndoAction.DO_NOTHING;
        }

        Minion copy = new Minion(actor.getOwner(), actor.getBaseDescr());

        UndoAction copyUndo = copy.copyOther(actor);
        UndoAction summonUndo = actor.getLocationRef().summonRight(copy);
        return () -> {
            summonUndo.undo();
            copyUndo.undo();
        };
    };

    public static final TargetlessAction<TargetableCharacter> SELF_DESTRUCT = (world, actor) -> {
        return actor.poison();
    };

    public static final TargetlessAction<PlayerProperty> REMOVE_OVERLOAD = (world, actor) -> {
        ManaResource mana = actor.getOwner().getManaResource();
        UndoAction thisTurnUndo = mana.setOverloadedMana(0);
        UndoAction nextTurnUndo = mana.setNextTurnOverload(0);
        return () -> {
            nextTurnUndo.undo();
            thisTurnUndo.undo();
        };
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

    public static final TargetlessAction<PlayerProperty> MIND_VISION = (world, actor) -> {
        Player player = actor.getOwner();
        Card card = player.getOpponent().getHand().getRandomCard();
        if (card == null) {
            return UndoAction.DO_NOTHING;
        }
        return player.getHand().addCard(card.getCardDescr());
    };

    public static final TargetlessAction<PlayerProperty> FISH_CARD_FOR_SELF = (world, actor) -> {
        Player player = actor.getOwner();
        if (world.getRandomProvider().roll(2) < 1) {
            return player.drawCardToHand();
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final TargetlessAction<PlayerProperty> SUMMON_DEAD_MINION = (world, actor) -> {
        Player player = actor.getOwner();
        List<Minion> deadMinions = player.getBoard().getGraveyard().getDeadMinions();
        if (deadMinions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        RandomProvider rng = world.getRandomProvider();
        Minion minion = deadMinions.get(rng.roll(deadMinions.size()));
        return player.summonMinion(minion.getBaseDescr());
    };

    public static final TargetlessAction<DamageSource> BLADE_FLURRY = (World world, DamageSource actor) -> {
        Player player = actor.getOwner();
        if (player.tryGetWeapon() == null) {
            return UndoAction.DO_NOTHING;
        }

        int damage = player.getHero().getAttackTool().getAttack();

        UndoAction destroyUndo = player.destroyWeapon();

        EntitySelector<DamageSource, TargetableCharacter> targets = EntitySelectors.enemyTargets();
        TargetlessAction<DamageSource> damageAction = damageTarget(targets, damage);

        UndoAction damageUndo = damageAction.alterWorld(world, actor);
        return () -> {
            damageUndo.undo();
            destroyUndo.undo();
        };
    };

    public static final TargetlessAction<Minion> EAT_DIVINE_SHIELDS = eatDivineShields(3, 3);

    public static final TargetlessAction<Minion> CONSUME_NEIGHBOURS = (World world, Minion actor) -> {
        int attackBuff = 0;
        int hpBuff = 0;

        UndoBuilder result = new UndoBuilder(4);

        Minion left = tryGetLeft(actor);
        if (left != null) {
            attackBuff += left.getAttackTool().getAttack();
            hpBuff += left.getBody().getCurrentHp();
            result.addUndo(TargetedActions.KILL_TARGET.alterWorld(world, actor, left));
        }

        Minion right = tryGetRight(actor);
        if (right != null) {
            attackBuff += right.getAttackTool().getAttack();
            hpBuff += right.getBody().getCurrentHp();
            result.addUndo(TargetedActions.KILL_TARGET.alterWorld(world, actor, right));
        }

        if (hpBuff != 0 && attackBuff != 0) {
            result.addUndo(actor.addAttackBuff(attackBuff));
            result.addUndo(actor.getBody().getHp().buffHp(hpBuff));
        }

        return result;
    };

    public static final TargetlessAction<PlayerProperty> DRAW_CARD_FOR_OPPONENTS_WEAPON = (world, actor) -> {
        Player player = actor.getOwner();
        Player opponent = player.getOpponent();
        Weapon weapon = opponent.tryGetWeapon();
        if (weapon == null) {
            return UndoAction.DO_NOTHING;
        }

        int charges = weapon.getCharges();
        UndoBuilder result = new UndoBuilder(charges + 1);
        result.addUndo(DESTROY_OPPONENTS_WEAPON.alterWorld(world, player));

        for (int i = 0; i < charges; i++) {
            result.addUndo(TargetlessActions.DRAW_FOR_SELF.alterWorld(world, player));
        }
        return result;
    };


    public static final TargetlessAction<PlayerProperty> STEAL_SECRET = (World world, PlayerProperty actor) -> {
        Player player = actor.getOwner();
        Player opponent = player.getOpponent();
        List<Secret> opponentSecrets = opponent.getSecrets().getSecrets();
        if (opponentSecrets.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        Map<EntityId, Secret> stealCandidates = new HashMap<>();
        opponentSecrets.forEach((secret) -> stealCandidates.put(secret.getSecretId(), secret));

        List<Secret> ourSecrets = player.getSecrets().getSecrets();
        ourSecrets.forEach((secret) -> stealCandidates.remove(secret.getSecretId()));

        if (stealCandidates.isEmpty()) {
            Secret selected = ActionUtils.pickRandom(world, opponentSecrets);
            if (selected == null) {
                return UndoAction.DO_NOTHING;
            }

            return opponent.getSecrets().removeSecret(selected);
        }
        else {
            Secret selected = ActionUtils.pickRandom(world, new ArrayList<>(stealCandidates.values()));
            if (selected == null) {
                return UndoAction.DO_NOTHING;
            }

            return player.getSecrets().stealActivatedSecret(opponent.getSecrets(), selected);
        }
    };

    public static final TargetlessAction<PlayerProperty> STEAL_RANDOM_MINION = (world, actor) -> {
        Player player = actor.getOwner();
        Player opponent = player.getOpponent();
        List<Minion> minions = opponent.getBoard().getAliveMinions();
        if (minions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        Minion stolenMinion = minions.get(world.getRandomProvider().roll(minions.size()));
        return player.getBoard().takeOwnership(stolenMinion);
    };

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

    public static final TargetlessAction<Minion> SUMMON_COPY_FOR_OPPONENT = (World world, Minion minion) -> {
        Player receiver = minion.getOwner().getOpponent();
        Minion newMinion = new Minion(receiver, minion.getBaseDescr());
        newMinion.copyOther(minion);

        return receiver.summonMinion(newMinion);
    };

    public static final TargetlessAction<PlayerProperty> DIVINE_FAVOR = (world, actor) -> {
        Player player = actor.getOwner();

        int playerHand = player.getHand().getCardCount();
        int opponentHand = player.getOpponent().getHand().getCardCount();
        if (playerHand >= opponentHand) {
            return UndoAction.DO_NOTHING;
        }

        int drawCount = opponentHand - playerHand;
        UndoBuilder result = new UndoBuilder(drawCount);
        for (int i = 0; i < drawCount; i++) {
            result.addUndo(player.drawCardToHand());
        }
        return result;
    };

    public static final TargetlessAction<PlayerProperty> ECHO_MINIONS = (world, actor) -> {
        Player player = actor.getOwner();
        BoardSide board = player.getBoard();
        List<Minion> minions = new ArrayList<>(board.getMaxSize());
        player.getBoard().collectMinions(minions);
        BornEntity.sortEntities(minions);

        UndoBuilder result = new UndoBuilder(minions.size());
        Hand hand = player.getHand();
        for (Minion minion: minions) {
            result.addUndo(hand.addCard(minion.getBaseDescr().getBaseCard()));
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

    public static <Actor, Target> TargetlessAction<Actor> forOtherTargets(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Target> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action) {
        return forOtherTargets(selector, action, false);
    }

    public static <Actor, Target> TargetlessAction<Actor> forOtherTargets(
            @NamedArg("selector") EntitySelector<? super Actor, ? extends Target> selector,
            @NamedArg("action") TargetedAction<? super Actor, ? super Target> action,
            @NamedArg("atomic") boolean atomic) {
        return forTargets(EntitySelectors.notSelf(selector), action, atomic);
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

    public static <Actor> TargetlessAction<Actor> doIf(
            @NamedArg("condition") Predicate<? super Actor> condition,
            @NamedArg("if") TargetlessAction<? super Actor> ifAction) {
        return doIf(condition, ifAction, TargetlessAction.DO_NOTHING);
    }

    public static <Actor> TargetlessAction<Actor> doIf(
            @NamedArg("condition") Predicate<? super Actor> condition,
            @NamedArg("if") TargetlessAction<? super Actor> ifAction,
            @NamedArg("else") TargetlessAction<? super Actor> elseAction) {
        ExceptionHelper.checkNotNullArgument(condition, "condition");
        ExceptionHelper.checkNotNullArgument(ifAction, "ifAction");
        ExceptionHelper.checkNotNullArgument(elseAction, "elseAction");

        return (World world, Actor actor) -> {
            return condition.test(actor)
                    ? ifAction.alterWorld(world, actor)
                    : elseAction.alterWorld(world, actor);
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> actWithOpponent(
            @NamedArg("action") TargetlessAction<? super Player> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor actor) -> {
            return action.alterWorld(world, actor.getOwner().getOpponent());
        };
    }

    public static <Actor> TargetlessAction<Actor> doMultipleTimes(
            @NamedArg("actionCount") int actionCount,
            @NamedArg("action") TargetlessAction<? super Actor> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Actor actor) -> {
            UndoBuilder result = new UndoBuilder(actionCount);
            for (int i = 0; i < actionCount; i++) {
                result.addUndo(action.alterWorld(world, actor));
            }
            return result;
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

    public static TargetlessAction<Minion> summonMinionLeft(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return (World world, Minion actor) -> {
            return actor.getLocationRef().summonLeft(minion.getMinion());
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

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> summonMinion(
            @NamedArg("minionCount") int minionCount,
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        if (minionCount <= 0) {
            return (world, player) -> UndoAction.DO_NOTHING;
        }
        if (minionCount == 1) {
            return summonMinion(minion);
        }
        return summonMinion(minionCount, minionCount, minion);
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> summonMinion(
            @NamedArg("minMinionCount") int minMinionCount,
            @NamedArg("maxMinionCount") int maxMinionCount,
            @NamedArg("minion") MinionProvider minion) {
        return (world, actor) -> {
            Player player = actor.getOwner();

            MinionDescr minionDescr = minion.getMinion();

            int minionCount = world.getRandomProvider().roll(minMinionCount, maxMinionCount);

            UndoBuilder result = new UndoBuilder(minionCount);
            for (int i = 0; i < minionCount; i++) {
                result.addUndo(player.summonMinion(minionDescr));
            }
            return result;
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

    public static TargetlessAction<Minion> summonSelectedRight(
            @NamedArg("minion") EntitySelector<? super Minion, ? extends MinionDescr> minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return (World world, Minion actor) -> {
            return minion.forEach(world, actor, (toSummon) -> actor.getLocationRef().summonRight(toSummon));
        };
    }

    public static <Actor extends DamageSource> TargetlessAction<Actor> damageTarget(
            @NamedArg("selector") EntitySelector<Actor, ? extends TargetableCharacter> selector,
            @NamedArg("damage") int damage) {
        return damageTarget(selector, damage, damage);
    }

    public static TargetlessAction<PlayerProperty> shuffleCardIntoDeck(@NamedArg("card") CardProvider card) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        return (World world, PlayerProperty actor) -> {
            Deck deck = actor.getOwner().getBoard().getDeck();
            return deck.putToRandomPosition(world.getRandomProvider(), card.getCard());
        };
    }

    public static <Actor extends DamageSource> TargetlessAction<Actor> damageTarget(
            @NamedArg("selector") EntitySelector<Actor, ? extends TargetableCharacter> selector,
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return forBornTargets(selector, TargetedActions.damageTarget(minDamage, maxDamage), true);
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
        return addSelectedCard((World world, Actor actor) -> Stream.of(card.getCard()));
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addSelectedCard(
            @NamedArg("card") EntitySelector<? super Actor, ? extends CardDescr> card) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        return addSelectedCard(0, card);
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> addSelectedCard(
            @NamedArg("costReduction") int costReduction,
            @NamedArg("card") EntitySelector<? super Actor, ? extends CardDescr> card) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        return (World world, Actor actor) -> {
            return addCards(world, actor, card, costReduction);
        };
    }

    private static <Actor extends PlayerProperty, CardType extends CardDescr> UndoAction addCards(
            World world,
            Actor actor,
            EntitySelector<? super Actor, CardType> card,
            int costReduction) {
        Player player = actor.getOwner();
        Hand hand = actor.getOwner().getHand();
        if (costReduction == 0) {
            return card.forEach(world, actor, hand::addCard);
        }
        else {
            return card.forEach(world, actor, (cardDescr) -> {
                Card toAdd = new Card(player, cardDescr);
                toAdd.decreaseManaCost(costReduction);
                return hand.addCard(toAdd);
            });
        }
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

    public static TargetlessAction<PlayerProperty> reduceWeaponDurability(@NamedArg("amount") int amount) {
        ExceptionHelper.checkArgumentInRange(amount, 1, Integer.MAX_VALUE, "amount");

        return (World world, PlayerProperty actor) -> {
            Weapon weapon = actor.getOwner().tryGetWeapon();
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

    public static TargetlessAction<PlayerProperty> getRandomFromDeck(
            @NamedArg("keywords") Keyword[] keywords) {
        return getRandomFromDeck(1, keywords);
    }

    public static TargetlessAction<PlayerProperty> getRandomFromDeck(
            @NamedArg("cardCount") int cardCount,
            @NamedArg("keywords") Keyword[] keywords) {
        return getRandomFromDeck(cardCount, keywords, null);
    }

    public static TargetlessAction<PlayerProperty> getRandomFromDeck(
            @NamedArg("keywords") Keyword[] keywords,
            @NamedArg("fallbackCard") CardProvider fallbackCard) {
        return getRandomFromDeck(1, keywords, fallbackCard);
    }

    public static TargetlessAction<PlayerProperty> getRandomFromDeck(
            @NamedArg("cardCount") int cardCount,
            @NamedArg("keywords") Keyword[] keywords,
            @NamedArg("fallbackCard") CardProvider fallbackCard) {

        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);
        return getRandomFromDeck(cardCount, cardFilter, fallbackCard);
    }

    public static TargetlessAction<PlayerProperty> getRandomFromDeck(
            int cardCount,
            Predicate<? super Card> cardFilter,
            CardProvider fallbackCard) {
        ExceptionHelper.checkArgumentInRange(cardCount, 1, Integer.MAX_VALUE, "cardCount");
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();

            UndoBuilder result = new UndoBuilder();

            boolean mayHaveCard = true;
            for (int i = 0; i < cardCount; i++) {
                UndoableResult<Card> selectedRef = mayHaveCard
                        ? ActionUtils.pollDeckForCard(player, cardFilter)
                        : null;

                Card selected;
                if (selectedRef == null) {
                    mayHaveCard = false;
                    selected = fallbackCard != null
                            ? new Card(player, fallbackCard.getCard())
                            : null;
                }
                else {
                    result.addUndo(selectedRef.getUndoAction());
                    selected = selectedRef.getResult();
                }

                if (selected == null) {
                    break;
                }

                result.addUndo(player.getHand().addCard(selected));
            }

            return result;
        };
    }

    public static TargetlessAction<Minion> mimironTransformation(@NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        Predicate<LabeledEntity> mechFilter = ActionUtils.includedKeywordsFilter(Keywords.RACE_MECH);

        return (World world, Minion actor) -> {
            Player player = actor.getOwner();

            List<Minion> mechs = new ArrayList<>();
            player.getBoard().collectAliveMinions(mechs, mechFilter);

            if (mechs.size() >= 3) {
                UndoBuilder result = new UndoBuilder(mechs.size() + 2);
                for (Minion mech: mechs) {
                    result.addUndo(mech.poison());
                }
                result.addUndo(world.endPhase());
                result.addUndo(player.summonMinion(minion.getMinion()));
                return result;
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public static <Actor> TargetlessAction<Actor> addThisTurnAbility(
            @NamedArg("ability") ActivatableAbility<? super Actor> ability) {
        ExceptionHelper.checkNotNullArgument(ability, "ability");
        return (world, actor) -> {
            return ActionUtils.doTemporary(world, () -> ability.activate(actor));
        };
    }

    public static TargetlessAction<PlayerProperty> summonRandomMinionFromDeck(
            @NamedArg("fallbackMinion") MinionProvider fallbackMinion) {

        Predicate<Card> appliedFilter = (card) -> card.getMinion() != null;
        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();

            UndoableResult<Card> cardRef = ActionUtils.pollDeckForCard(player, appliedFilter);
            if (cardRef == null && fallbackMinion == null) {
                return UndoAction.DO_NOTHING;
            }

            MinionDescr minion = cardRef != null
                    ? cardRef.getResult().getMinion().getBaseDescr()
                    : fallbackMinion.getMinion();
            assert minion != null;

            UndoAction summonUndo = player.summonMinion(minion);
            return () -> {
                summonUndo.undo();
                if (cardRef != null) {
                    cardRef.undo();
                }
            };
        };
    }

    public static TargetlessAction<Minion> summonRandomMinionFromHandRight(
            @NamedArg("keywords") Keyword[] keywords) {
        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);

        return (world, actor) -> {
            Hand hand = actor.getOwner().getHand();
            int cardIndex = hand.chooseRandomCardIndex(cardFilter);
            if (cardIndex < 0) {
                return UndoAction.DO_NOTHING;
            }

            UndoableResult<Card> removedCardRef = hand.removeAtIndex(cardIndex);
            Minion minion = removedCardRef.getResult().getMinion();
            assert minion != null;

            UndoAction summonUndo = actor.getLocationRef().summonRight(minion);
            return () -> {
                summonUndo.undo();
                removedCardRef.undo();
            };
        };
    }

    private static ActivatableAbility<Player> deactivateAfterPlay(
            ActivatableAbility<Player> ability,
            AuraFilter<? super Player, ? super Card> filter) {
        return deactivateAfterCardPlay(ability, (card) -> {
            return filter.isApplicable(card.getWorld(), card.getOwner(), card);
        });
    }

    private static ActivatableAbility<Player> deactivateAfterCardPlay(
            ActivatableAbility<Player> ability,
            Predicate<Card> deactivateCondition) {
        ExceptionHelper.checkNotNullArgument(ability, "ability");
        ExceptionHelper.checkNotNullArgument(deactivateCondition, "deactivateCondition");

        return (Player self) -> {
            UndoableUnregisterRefBuilder result = new UndoableUnregisterRefBuilder(2);

            UndoableUnregisterRef abilityRef = ability.activate(self);
            result.addRef(abilityRef);

            UndoableUnregisterRef listenerRef = self.getWorld().getEvents().startPlayingCardListeners().addAction((World world, CardPlayEvent playEvent) -> {
                if (deactivateCondition.test(playEvent.getCard())) {
                    return abilityRef.unregister();
                }
                else {
                    return UndoAction.DO_NOTHING;
                }
            });
            result.addRef(listenerRef);

            return result;
        };
    }

    public static TargetlessAction<PlayerProperty> experiment(
            @NamedArg("replaceCard") CardProvider replaceCard) {
        ExceptionHelper.checkNotNullArgument(replaceCard, "replaceCard");

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            UndoableResult<Card> cardRef = player.drawFromDeck();
            Card card = cardRef.getResult();
            if (card == null) {
                return cardRef.getUndoAction();
            }

            CardDescr cardDescr = card.getCardDescr();

            if (cardDescr.getCardType() == CardType.MINION) {
                UndoAction drawActionsUndo = WorldActionList.executeActionsNow(world, card, cardDescr.getOnDrawActions());
                UndoAction addCardUndo = player.getHand().addCard(replaceCard.getCard());
                UndoAction eventUndo = world.getEvents().drawCardListeners().triggerEvent(card);

                return () -> {
                    eventUndo.undo();
                    addCardUndo.undo();
                    drawActionsUndo.undo();
                    cardRef.undo();
                };
            }
            else {
                UndoAction drawUndo = player.drawCardToHand(cardRef.getResult());
                return () -> {
                    drawUndo.undo();
                    cardRef.undo();
                };
            }
        };
    }

    public static TargetlessAction<PlayerProperty> drawCardToFillHand(@NamedArg("targetHandSize") int targetHandSize) {
        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            int currentHandSize = player.getHand().getCardCount();
            if (currentHandSize >= targetHandSize) {
                return UndoAction.DO_NOTHING;
            }

            int drawCount = targetHandSize - currentHandSize;
            UndoBuilder result = new UndoBuilder(drawCount);
            for (int i = 0; i < drawCount; i++) {
                result.addUndo(player.drawCardToHand());
            }
            return result;
        };
    }

    public static TargetlessAction<Object> killAndReplaceMinions(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        return (World world, Object actor) -> {
            List<Minion> minions1 = new ArrayList<>(Player.MAX_BOARD_SIZE);
            List<Minion> minions2 = new ArrayList<>(Player.MAX_BOARD_SIZE);

            Player player1 = world.getPlayer1();
            Player player2 = world.getPlayer2();

            player1.getBoard().collectMinions(minions1);
            player2.getBoard().collectMinions(minions2);

            UndoBuilder result = new UndoBuilder();

            for (Minion killedMinion: minions1) {
                result.addUndo(killedMinion.poison());
            }
            for (Minion killedMinion: minions2) {
                result.addUndo(killedMinion.poison());
            }

            result.addUndo(world.endPhase());

            result.addUndo(TargetlessActions.summonMinion(minions1.size(), minion).alterWorld(world, player1));
            result.addUndo(TargetlessActions.summonMinion(minions2.size(), minion).alterWorld(world, player2));

            return result;
        };
    }

    public static TargetlessAction<Minion> replaceHero(
            @NamedArg("heroClass") Keyword heroClass,
            @NamedArg("heroPower") CardId heroPower) {

        return (World world, Minion actor) -> {
            Player player = actor.getOwner();
            UndoAction removeUndo = actor.getLocationRef().removeFromBoard();

            MinionBody body = actor.getBody();

            Hero hero = new Hero(player, body.getHp(), 0, heroClass, actor.getKeywords());
            hero.setCurrentHp(body.getCurrentHp());
            hero.setHeroPower(world.getDb().getHeroPowerDb().getById(heroPower));

            UndoAction setHeroUndo = player.setHero(hero);
            return () -> {
                setHeroUndo.undo();
                removeUndo.undo();
            };
        };
    }

    public static TargetlessAction<PlayerProperty> replaceHero(
            @NamedArg("maxHp") int maxHp,
            @NamedArg("armor") int armor,
            @NamedArg("heroPower") String heroPower,
            @NamedArg("heroClass") Keyword heroClass) {
        return replaceHero(maxHp, armor, heroPower, heroClass, new Keyword[0]);
    }

    public static TargetlessAction<PlayerProperty> replaceHero(
            @NamedArg("maxHp") int maxHp,
            @NamedArg("armor") int armor,
            @NamedArg("heroPower") String heroPower,
            @NamedArg("heroClass") Keyword heroClass,
            @NamedArg("keywords") Keyword[] keywords) {
        ExceptionHelper.checkNotNullArgument(heroPower, "heroPower");
        ExceptionHelper.checkNotNullArgument(heroClass, "heroClass");

        List<Keyword> keywordsCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            Hero hero = new Hero(player, maxHp, armor, heroClass, keywordsCopy);
            hero.setHeroPower(world.getDb().getHeroPowerDb().getById(new CardId(heroPower)));

            return player.setHero(hero);
        };
    }

    public static TargetlessAction<PlayerProperty> replaceHeroPower(
            @NamedArg("heroPower") CardId[] heroPower) {
        ExceptionHelper.checkArgumentInRange(heroPower.length, 1, Integer.MAX_VALUE, "heroPower.length");
        CardId[] heroPowerCopy = heroPower.clone();
        ExceptionHelper.checkNotNullElements(heroPowerCopy, "heroPower");


        return (World world, PlayerProperty actor) -> {
            Hero hero = actor.getOwner().getHero();

            CardId currentId = hero.getHeroPower().getPowerDef().getId();
            CardId newId = heroPowerCopy[0];
            for (int i = 0; i < heroPowerCopy.length; i++) {
                if (currentId.equals(heroPowerCopy[i])) {
                    int selectedIndex = i + 1;
                    newId = selectedIndex >= heroPowerCopy.length
                            ? heroPowerCopy[heroPowerCopy.length - 1]
                            : heroPowerCopy[selectedIndex];
                    break;
                }
            }

            return hero.setHeroPower(world.getDb().getHeroPowerDb().getById(newId));
        };
    }

    public static TargetlessAction<PlayerProperty> gainMana(@NamedArg("mana") int mana) {
        return (world, actor) -> {
            Player player = actor.getOwner();
            return player.setMana(player.getMana() + mana);
        };
    }

    public static TargetlessAction<PlayerProperty> setManaCostThisTurn(
            @NamedArg("manaCost") int manaCost,
            @NamedArg("filter") AuraFilter<? super Player, ? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, PlayerProperty actor) -> {
            ActivatableAbility<Player> aura = Auras.aura(
                    CardAuras.OWN_CARD_PROVIDER,
                    filter,
                    CardAuras.setManaCost(manaCost));
            aura = deactivateAfterPlay(aura, filter);
            aura = ActionUtils.toSingleTurnAbility(world, aura);

            return aura.activate(actor.getOwner());
        };
    }

    public static TargetlessAction<PlayerProperty> reduceManaCostThisTurn(
            @NamedArg("amount") int amount,
            @NamedArg("filter") AuraFilter<? super Player, ? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, PlayerProperty actor) -> {
            ActivatableAbility<Player> aura = Auras.aura(
                    CardAuras.OWN_CARD_PROVIDER,
                    filter,
                    CardAuras.increaseManaCost(-amount));
            aura = deactivateAfterPlay(aura, filter);
            aura = ActionUtils.toSingleTurnAbility(world, aura);

            return aura.activate(actor.getOwner());
        };
    }

    public static TargetlessAction<PlayerProperty> reduceManaCostNextCard(
            @NamedArg("amount") int amount,
            @NamedArg("filter") AuraFilter<? super Player, ? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, PlayerProperty actor) -> {
            ActivatableAbility<Player> aura = Auras.aura(
                    CardAuras.OWN_CARD_PROVIDER,
                    filter,
                    CardAuras.increaseManaCost(-amount));
            aura = deactivateAfterPlay(aura, filter);

            return aura.activate(actor.getOwner());
        };
    }

    public static <Target> TargetlessAction<PlayerProperty> untilTurnStartsAura(
            @NamedArg("target") AuraTargetProvider<? super Player, ? extends Target> target,
            @NamedArg("aura") Aura<? super Player, ? super Target> aura) {
        return untilTurnStartsAura(target, AuraFilter.ANY, aura);
    }

    public static <Target> TargetlessAction<PlayerProperty> untilTurnStartsAura(
            @NamedArg("target") AuraTargetProvider<? super Player, ? extends Target> target,
            @NamedArg("filter") AuraFilter<? super Player, ? super Target> filter,
            @NamedArg("aura") Aura<? super Player, ? super Target> aura) {

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            return ActionUtils.doUntilNewTurnStart(player.getWorld(), player, () -> {
                return player.getWorld().addAura(new TargetedActiveAura<>(player, target, filter, aura));
            });
        };
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> doOnEndOfTurn(
            @NamedArg("action") TargetlessAction<? super Actor> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Actor actor) -> {
            return ActionUtils.doOnEndOfTurn(world, () -> action.alterWorld(world, actor));
        };
    }

    private static Minion tryGetLeft(Minion minion) {
        SummonLocationRef locationRef = minion.getLocationRef();
        BoardLocationRef left = locationRef.tryGetLeft();
        return left != null ? left.getMinion() : null;
    }

    private static Minion tryGetRight(Minion minion) {
        SummonLocationRef locationRef = minion.getLocationRef();
        BoardLocationRef right = locationRef.tryGetRight();
        return right != null ? right.getMinion() : null;
    }

    public static <Actor extends PlayerProperty> TargetlessAction<Actor> drawAndPlayCard(@NamedArg("keywords") Keyword[] keywords) {
        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);
        return drawAndPlayCard(cardFilter);
    }

    private static <Actor extends PlayerProperty> TargetlessAction<Actor> drawAndPlayCard(Predicate<? super Card> cardFilter) {
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        return (World world, Actor actor) -> {
            Player player = actor.getOwner();
            UndoableResult<Card> cardRef = ActionUtils.pollDeckForCard(player, cardFilter);
            if (cardRef == null) {
                return UndoAction.DO_NOTHING;
            }

            UndoAction playUndo = player.playCardEffect(cardRef.getResult());

            return () -> {
                playUndo.undo();
                cardRef.undo();
            };
        };
    }

    private static CardDescr chooseCard(World world, List<CardDescr> cards) {
        int cardCount = cards.size();
        if (cardCount == 0) {
            return null;
        }

        if (cardCount == 1) {
            return cards.get(0);
        }

        return world.getUserAgent().selectCard(false, cards);
    }

    public static TargetlessAction<PlayerProperty> trackCard(@NamedArg("cardCount") int cardCount) {
        ExceptionHelper.checkArgumentInRange(cardCount, 1, Integer.MAX_VALUE, "cardCount");

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            Deck deck = player.getBoard().getDeck();

            UndoBuilder result = new UndoBuilder(cardCount + 1);
            List<CardDescr> choosenCards = new ArrayList<>(cardCount);
            for (int i = 0; i < cardCount; i++) {
                UndoableResult<Card> cardRef = deck.tryDrawOneCard();
                if (cardRef == null) {
                    break;
                }

                result.addUndo(cardRef.getUndoAction());
                choosenCards.add(cardRef.getResult().getCardDescr());
            }

            CardDescr chosenCard = chooseCard(world, choosenCards);
            if (chosenCard != null) {
                result.addUndo(player.getHand().addCard(chosenCard));
            }

            return result;
        };
    }

    public static TargetlessAction<PlayerProperty> unleashMinions(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        return (World world, PlayerProperty actor) -> {
            Player player = actor.getOwner();
            int minionCount = player.getOpponent().getBoard().getMinionCount();

            MinionDescr toSummon = minion.getMinion();
            UndoBuilder result = new UndoBuilder(minionCount);
            for (int i = 0; i < minionCount; i++) {
                result.addUndo(player.summonMinion(toSummon));
            }
            return result;
        };
    }

    private TargetlessActions() {
        throw new AssertionError();
    }
}
