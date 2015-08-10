package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.BoardSide;
import com.github.kelemen.hearthstone.emulator.BornEntity;
import com.github.kelemen.hearthstone.emulator.CardPlayEvent;
import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.Deck;
import com.github.kelemen.hearthstone.emulator.EntityId;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.HeroPowerId;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.ManaResource;
import com.github.kelemen.hearthstone.emulator.MultiTargeter;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.RandomProvider;
import com.github.kelemen.hearthstone.emulator.Secret;
import com.github.kelemen.hearthstone.emulator.SecretContainer;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.abilities.Aura;
import com.github.kelemen.hearthstone.emulator.abilities.AuraFilter;
import com.github.kelemen.hearthstone.emulator.abilities.AuraTargetProvider;
import com.github.kelemen.hearthstone.emulator.abilities.Auras;
import com.github.kelemen.hearthstone.emulator.abilities.CardAuras;
import com.github.kelemen.hearthstone.emulator.abilities.TargetedActiveAura;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardProvider;
import com.github.kelemen.hearthstone.emulator.cards.CardType;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jtrim.utils.ExceptionHelper;


public final class PlayerActions {
    public static final PlayerAction DO_NOTHING = (world, player) -> UndoAction.DO_NOTHING;

    public static final PlayerAction TRIGGER_DEATHRATTLES = (world, player) -> {
        return player.getBoard().forAllMinions((minion) -> minion.triggetDeathRattles());
    };

    public static final PlayerAction KILL_ALL_MINIONS = killAllMinions();

    private static final ActivatableAbility<PlayerProperty> HERO_IS_IMMUNE = (PlayerProperty self) -> {
        return self.getOwner().getHero().getImmuneProperty().addRemovableBuff(true);
    };

    public static final PlayerAction HERO_IS_IMMUNE_THIS_TURN = activateAbilityForThisTurn(HERO_IS_IMMUNE);

    public static final PlayerAction DISCARD_HAND = (world, player) -> {
        // TODO: Display discarded cards to opponent
        return player.getHand().discardAll();
    };

    public static final PlayerAction REMOVE_OVERLOAD = (world, player) -> {
        ManaResource mana = player.getManaResource();
        UndoAction thisTurnUndo = mana.setOverloadedMana(0);
        UndoAction nextTurnUndo = mana.setNextTurnOverload(0);
        return () -> {
            nextTurnUndo.undo();
            thisTurnUndo.undo();
        };
    };

    public static final PlayerAction STEAL_RANDOM_MINION = (world, player) -> {
        Player opponent = player.getOpponent();
        List<Minion> minions = opponent.getBoard().getAliveMinions();
        if (minions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        Minion stolenMinion = minions.get(world.getRandomProvider().roll(minions.size()));
        return player.getBoard().takeOwnership(stolenMinion);
    };

    public static final PlayerAction DIVINE_FAVOR = (world, player) -> {
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

    public static final PlayerAction ECHO_MINIONS = (world, player) -> {
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

    public static final PlayerAction BATTLE_RAGE = (world, player) -> {
        int cardsToDraw = player.getHero().isDamaged() ? 1 : 0;
        cardsToDraw += player.getBoard().countMinions(Minion::isDamaged);

        UndoBuilder result = new UndoBuilder(cardsToDraw);
        for (int i = 0; i < cardsToDraw; i++) {
            result.addUndo(player.drawCardToHand());
        }
        return result;
    };

    public static final PlayerAction BRAWL = (world, player) -> {
        List<Minion> minions = new ArrayList<>(2 * Player.MAX_BOARD_SIZE);
        world.getPlayer1().getBoard().collectMinions(minions);
        world.getPlayer2().getBoard().collectMinions(minions);

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

    public static final PlayerAction DRAW_FOR_SELF = (world, player) -> {
        return player.drawCardToHand();
    };

    public static final PlayerAction DRAW_FOR_OPPONENT = (world, player) -> {
        return player.getOpponent().drawCardToHand();
    };

    public static final PlayerAction FISH_CARD_FOR_SELF = (world, player) -> {
        if (world.getRandomProvider().roll(2) < 1) {
            return player.drawCardToHand();
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    };

    public static final PlayerAction DISCARD_RANDOM_CARD = (world, player) -> {
        Hand hand = player.getHand();
        int cardCount = hand.getCardCount();
        if (cardCount == 0) {
            return UndoAction.DO_NOTHING;
        }

        int cardIndex = world.getRandomProvider().roll(cardCount);
        // TODO: Show discarded card to the opponent.
        return hand.removeAtIndex(cardIndex);
    };

    public static final PlayerAction DISCARD_FROM_DECK = (world, player) -> {
        Deck deck = player.getBoard().getDeck();
        if (deck.getNumberOfCards() <= 0) {
            return UndoAction.DO_NOTHING;
        }

        UndoableResult<CardDescr> cardRef = deck.tryDrawOneCard();
        // TODO: Show discarded card to the opponent.
        return cardRef != null ? cardRef.getUndoAction() : UndoAction.DO_NOTHING;
    };

    public static final PlayerAction DESTROY_OPPONENTS_WEAPON = (world, player) -> {
        return player.getOpponent().destroyWeapon();
    };

    public static final PlayerAction REDUCE_OPPONENT_WEAPON_DURABILITY = reduceOpponentWeaponDurability(1);

    public static final PlayerAction DRAW_CARD_FOR_OPPONENTS_WEAPON = (world, player) -> {
        Player opponent = player.getOpponent();
        Weapon weapon = opponent.tryGetWeapon();
        if (weapon == null) {
            return UndoAction.DO_NOTHING;
        }

        int charges = weapon.getCharges();
        UndoBuilder result = new UndoBuilder(charges + 1);
        result.addUndo(DESTROY_OPPONENTS_WEAPON.alterWorld(world, player));

        for (int i = 0; i < charges; i++) {
            result.addUndo(DRAW_FOR_SELF.alterWorld(world, player));
        }
        return result;
    };

    public static final PlayerAction SUMMON_DEAD_MINION = (World world, Player player) -> {
        List<Minion> deadMinions = player.getBoard().getGraveyard().getDeadMinions();
        if (deadMinions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        RandomProvider rng = world.getRandomProvider();
        Minion minion = deadMinions.get(rng.roll(deadMinions.size()));
        return player.summonMinion(minion.getBaseDescr());
    };

    public static final PlayerAction RESURRECT_DEAD_MINIONS = (World world, Player player) -> {
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

    public static final PlayerAction BLADE_FLURRY = (World world, Player player) -> {
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

    public static final PlayerAction SUMMON_RANDOM_MINION_FROM_HAND = (World world, Player player) -> {
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

    public static final PlayerAction DESTROY_STEALTH = (world, player) -> {
        UndoAction destroy1Undo = destroyStealth(world.getPlayer1());
        UndoAction destroy2Undo = destroyStealth(world.getPlayer2());
        return () -> {
            destroy2Undo.undo();
            destroy1Undo.undo();
        };
    };

    public static final PlayerAction DESTROY_OPPONENT_SECRET = (world, player) -> {
        SecretContainer secrets = player.getOpponent().getSecrets();
        return secrets.removeAllSecrets();
    };

    public static final PlayerAction STEAL_SECRET = (World world, Player player) -> {
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

    public static final PlayerAction SUMMON_RANDOM_MINION_FROM_DECK = summonRandomMinionFromDeck(false);
    public static final PlayerAction SUMMON_RANDOM_MINION_FROM_DECK_FOR_OPPONENT = summonRandomMinionFromDeck(true);

    public static PlayerAction addRandomCard(@NamedArg("keywords") Keyword[] keywords) {
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Player player) -> {
            List<CardDescr> possibleCards = world.getDb().getCardDb().getByKeywords(keywordsCopy);
            CardDescr selected = ActionUtils.pickRandom(world, possibleCards);
            if (selected == null) {
                return UndoAction.DO_NOTHING;
            }

            return player.getHand().addCard(selected);
        };
    }

    public static PlayerAction bouncingBlade(
            @NamedArg("maxBounces") int maxBounces,
            @NamedArg("baseDamage") int baseDamage) {

        Predicate<Minion> minionFilter = (minion) -> {
            MinionBody body = minion.getBody();
            int currentHp = body.getCurrentHp();
            return currentHp > 0 && !body.isImmune() && body.getMinHpProperty().getValue() < currentHp;
        };

        return (World world, Player player) -> {
            Damage damage = player.getSpellDamage(1);
            List<Minion> targets = new ArrayList<>();

            UndoBuilder result = new UndoBuilder();
            for (int i = 0; i < maxBounces; i++) {
                targets.clear();
                world.getPlayer1().getBoard().collectMinions(targets, minionFilter);
                world.getPlayer2().getBoard().collectMinions(targets, minionFilter);

                Minion selected = ActionUtils.pickRandom(world, targets);
                if (selected == null) {
                    break;
                }

                result.addUndo(selected.damage(damage));
                if (selected.getBody().getCurrentHp() <= 0) {
                    break;
                }
            }
            return result;
        };
    }

    public static PlayerAction experiment(
            @NamedArg("replaceCard") CardProvider replaceCard) {
        return (World world, Player player) -> {
            UndoableResult<CardDescr> cardRef = player.drawFromDeck();
            CardDescr card = cardRef.getResult();
            if (card == null) {
                return cardRef.getUndoAction();
            }

            if (card.getCardType() == CardType.MINION) {
                UndoAction drawActionsUndo = WorldActionList.executeActionsNow(world, player, card.getOnDrawActions());
                UndoAction addCardUndo = player.getHand().addCard(replaceCard.getCard());
                UndoAction eventUndo = world.getEvents().drawCardListeners().triggerEvent(new Card(player, card));

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

    public static PlayerAction doForOpponent(@NamedArg("action") PlayerAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Player player) -> {
            return action.alterWorld(world, player.getOpponent());
        };
    }

    public static PlayerAction forAllMinions(@NamedArg("action") TargetedMinionAction action) {
        return forAllMinions(AuraFilter.ANY, action);
    }

    public static PlayerAction forAllMinions(
            @NamedArg("filter") AuraFilter<? super Player, ? super Minion> filter,
            @NamedArg("action") TargetedMinionAction action) {
        return forMinions(action, (player, targets) -> {
            World world = player.getWorld();
            Predicate<Minion> appliedFilter = (minion) -> {
                return filter.isApplicable(world, player, minion) && minion.notScheduledToDestroy();
            };
            world.getPlayer1().getBoard().collectMinions(targets, appliedFilter);
            world.getPlayer2().getBoard().collectMinions(targets, appliedFilter);
        });
    }

    public static PlayerAction forEnemyMinions(@NamedArg("action") TargetedMinionAction action) {
        return forEnemyMinions(WorldEventFilter.ANY, action);
    }

    public static PlayerAction forOwnMinions(@NamedArg("action") TargetedMinionAction action) {
        return forOwnMinions(WorldEventFilter.ANY, action);
    }

    public static PlayerAction forEnemyMinions(
            @NamedArg("filter") WorldEventFilter<? super Player, ? super Minion> filter,
            @NamedArg("action") TargetedMinionAction action) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(action, "action");
        return forMinions(action, (player, targets) -> {
            player.getOpponent().getBoard().collectMinions(targets, (minion) -> {
                return minion.notScheduledToDestroy() && filter.applies(player.getWorld(), player, minion);
            });
        });
    }

    public static PlayerAction forOwnMinions(
            @NamedArg("filter") WorldEventFilter<? super Player, ? super Minion> filter,
            @NamedArg("action") TargetedMinionAction action) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(action, "action");
        return forMinions(action, (player, targets) -> {
            player.getBoard().collectMinions(targets, (minion) -> {
                return minion.notScheduledToDestroy() && filter.applies(player.getWorld(), player, minion);
            });
        });
    }

    private static PlayerAction forMinions(
            TargetedMinionAction action,
            BiConsumer<Player, List<Minion>> minionCollector) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        ExceptionHelper.checkNotNullArgument(minionCollector, "minionCollector");

        return (World world, Player player) -> {
            List<Minion> targets = new ArrayList<>();
            minionCollector.accept(player, targets);

            if (targets.isEmpty()) {
                return UndoAction.DO_NOTHING;
            }

            BornEntity.sortEntities(targets);

            UndoBuilder result = new UndoBuilder(targets.size());
            for (Minion minion: targets) {
                result.addUndo(action.doAction(minion, new PlayTarget(player, minion)));
            }
            return result;
        };
    }

    public static PlayerAction forRandomEnemyMinions(
            @NamedArg("minionCount") int minionCount,
            @NamedArg("action") TargetedAction action) {
        return forRandomMinions(minionCount, action, (player, targets) -> {
            player.getOpponent().getBoard().collectMinions(targets, Minion::notScheduledToDestroy);
        });
    }

    private static PlayerAction forRandomMinions(
            int minionCount,
            TargetedAction action,
            BiConsumer<Player, List<Minion>> minionCollector) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        ExceptionHelper.checkNotNullArgument(minionCollector, "minionCollector");

        return (World world, Player player) -> {
            List<Minion> targets = new ArrayList<>();
            minionCollector.accept(player, targets);

            if (targets.isEmpty()) {
                return UndoAction.DO_NOTHING;
            }

            UndoBuilder result = new UndoBuilder(minionCount);
            RandomProvider rng = world.getRandomProvider();
            for (int i = 0; i < minionCount && !targets.isEmpty(); i++) {
                int index = rng.roll(targets.size());
                Minion selected = targets.remove(index);
                result.addUndo(action.alterWorld(world, new PlayTarget(player, selected)));
            }
            return result;
        };
    }

    public static PlayerAction reduceOpponentWeaponDurability(@NamedArg("amount") int amount) {
        ExceptionHelper.checkArgumentInRange(amount, 1, Integer.MAX_VALUE, "amount");

        return (World world, Player player) -> {
            Weapon weapon = player.tryGetWeapon();
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

    private static UndoAction destroyStealth(Player player) {
        return player.getBoard().forAllMinions((minion) -> minion.getProperties().getBody().setStealth(false));
    }

    public static PlayerAction getRandomOpponentCard(
            @NamedArg("fallbackCard") CardProvider fallbackCard,
            @NamedArg("keywords") Keyword[] keywords) {
        ExceptionHelper.checkNotNullArgument(fallbackCard, "fallbackCard");
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Player player) -> {
            Keyword[] allKeywords = new Keyword[keywordsCopy.length + 1];
            allKeywords[0] = player.getOpponent().getHero().getHeroClass();
            System.arraycopy(keywordsCopy, 0, allKeywords, 1, keywordsCopy.length);

            List<CardDescr> candidates = world.getDb().getCardDb().getByKeywords(allKeywords);
            CardDescr selected = ActionUtils.pickRandom(world, candidates);
            if (selected == null) {
                selected = fallbackCard.getCard();
            }

            return player.getHand().addCard(selected);
        };
    }

    public static PlayerAction getRandomFromDeck(
            @NamedArg("keywords") Keyword[] keywords) {
        return getRandomFromDeck(1, keywords);
    }

    public static PlayerAction getRandomFromDeck(
            @NamedArg("cardCount") int cardCount,
            @NamedArg("keywords") Keyword[] keywords) {
        return getRandomFromDeck(cardCount, keywords, null);
    }

    public static PlayerAction getRandomFromDeck(
            @NamedArg("keywords") Keyword[] keywords,
            @NamedArg("fallbackCard") CardProvider fallbackCard) {
        return getRandomFromDeck(1, keywords, fallbackCard);
    }

    public static PlayerAction getRandomFromDeck(
            @NamedArg("cardCount") int cardCount,
            @NamedArg("keywords") Keyword[] keywords,
            @NamedArg("fallbackCard") CardProvider fallbackCard) {

        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);
        return getRandomFromDeck(cardCount, cardFilter, fallbackCard);
    }

    public static PlayerAction getRandomFromDeck(
            int cardCount,
            Predicate<? super CardDescr> cardFilter,
            CardProvider fallbackCard) {
        ExceptionHelper.checkArgumentInRange(cardCount, 1, Integer.MAX_VALUE, "cardCount");
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        return (World world, Player player) -> {
            UndoBuilder result = new UndoBuilder();

            boolean mayHaveCard = true;
            for (int i = 0; i < cardCount; i++) {
                UndoableResult<CardDescr> selectedRef = mayHaveCard
                        ? ActionUtils.pollDeckForCard(player, cardFilter)
                        : null;

                CardDescr selected;
                if (selectedRef == null) {
                    mayHaveCard = false;
                    selected = fallbackCard != null ? fallbackCard.getCard() : null;
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

    public static PlayerAction addCard(@NamedArg("keywords") Keyword[] keywords) {
        return addCard(0, keywords);
    }

    public static PlayerAction addCard(
            @NamedArg("costReduction") int costReduction,
            @NamedArg("keywords") Keyword[] keywords) {
        return addCardToHand(costReduction, keywords, Function.identity());
    }

    private static PlayerAction addCardToHand(
            int costReduction,
            Keyword[] keywords,
            Function<Player, Player> playerGetter) {

        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");
        ExceptionHelper.checkNotNullArgument(playerGetter, "playerGetter");

        return (World world, Player player) -> {
            List<CardDescr> candidates = world.getDb().getCardDb().getByKeywords(keywordsCopy);
            CardDescr selected = ActionUtils.pickRandom(world, candidates);
            if (selected == null) {
                return UndoAction.DO_NOTHING;
            }

            Player receiver = playerGetter.apply(player);
            Card card = new Card(receiver, selected);
            if (costReduction != 0) {
                card.decreaseManaCost(costReduction);
            }

            Hand hand = receiver.getHand();
            return hand.addCard(card);
        };
    }

    public static PlayerAction drawCard(@NamedArg("costReduction") int costReduction) {
        return drawCard(costReduction, WorldEventFilter.ANY);
    }

    public static PlayerAction drawCard(
            @NamedArg("costReduction") int costReduction,
            @NamedArg("costReductionFilter") WorldEventFilter<? super Player, ? super Card> costReductionFilter) {
        ExceptionHelper.checkNotNullArgument(costReductionFilter, "costReductionFilter");
        if (costReduction == 0) {
            return DRAW_FOR_SELF;
        }

        return (World world, Player player) -> {
            UndoableResult<CardDescr> cardRef = player.drawFromDeck();
            CardDescr cardDescr = cardRef.getResult();
            if (cardDescr == null) {
                return cardRef.getUndoAction();
            }

            Card card = new Card(player, cardRef.getResult());
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

    public static PlayerAction summonRandomMinionFromDeck() {
        return summonRandomMinionFromDeck(false);
    }

    public static PlayerAction summonRandomMinionFromDeckForOpponent() {
        return summonRandomMinionFromDeck(true);
    }

    public static PlayerAction summonRandomMinionFromDeck(@NamedArg("opponent") boolean opponent) {
        return summonRandomMinionFromDeck(opponent, (minion) -> true);
    }

    public static PlayerAction summonRandomMinionFromDeck(
            boolean opponent,
            Predicate<? super CardDescr> cardFilter) {
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        Predicate<CardDescr> appliedFilter = (card) -> {
            return card.getMinion() != null && cardFilter.test(card);
        };

        return (World world, Player player) -> {
            Player summoner = opponent ? player.getOpponent() : player;
            if (summoner.getBoard().isFull()) {
                return UndoAction.DO_NOTHING;
            }

            UndoableResult<CardDescr> cardRef = ActionUtils.pollDeckForCard(summoner, appliedFilter);
            if (cardRef == null) {
                return UndoAction.DO_NOTHING;
            }

            MinionDescr minion = cardRef.getResult().getMinion();
            assert minion != null;

            UndoAction summonUndo = summoner.summonMinion(minion);
            return () -> {
                summonUndo.undo();
                cardRef.undo();
            };
        };
    }

    public static PlayerAction drawCardToFillHand(@NamedArg("targetHandSize") int targetHandSize) {
        return (World world, Player player) -> {
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

    public static PlayerAction drawAndPlayCard(@NamedArg("keywords") Keyword[] keywords) {
        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);
        return drawAndPlayCard(cardFilter);
    }

    public static PlayerAction drawAndPlayCard(Predicate<? super CardDescr> cardFilter) {
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        return (World world, Player player) -> {
            UndoableResult<CardDescr> cardRef = ActionUtils.pollDeckForCard(player, cardFilter);
            if (cardRef == null) {
                return UndoAction.DO_NOTHING;
            }

            UndoAction playUndo = player.playCardEffect(new Card(player, cardRef.getResult()));

            return () -> {
                playUndo.undo();
                cardRef.undo();
            };
        };
    }

    private static CardDescr chooseCard(World world, CardProvider[] providers) {
        if (providers.length == 1) {
            return providers[0].getCard();
        }

        int cardIndex = world.getRandomProvider().roll(providers.length);
        return providers[cardIndex].getCard();
    }

    public static PlayerAction shuffleCardIntoDeck(@NamedArg("card") CardProvider card) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        return (World world, Player player) -> {
            Deck deck = player.getBoard().getDeck();
            return deck.putToRandomPosition(world.getRandomProvider(), card.getCard());
        };
    }

    public static PlayerAction addCard(@NamedArg("card") CardProvider... card) {
        return addCard(false, card);
    }

    public static PlayerAction addCard(
            @NamedArg("delay") boolean delay,
            @NamedArg("card") CardProvider... card) {

        CardProvider[] cardCopy = card.clone();
        ExceptionHelper.checkNotNullElements(cardCopy, "card");
        ExceptionHelper.checkArgumentInRange(cardCopy.length, 1, Integer.MAX_VALUE, "card.length");

        return (World world, Player player) -> {
            Hand hand = player.getHand();
            CardDescr chosenCard = chooseCard(world, cardCopy);
            if (!delay) {
                return hand.addCard(chosenCard);
            }

            return ActionUtils.doOnEndOfTurn(world, () -> {
                return hand.addCard(chosenCard);
            });
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

    public static PlayerAction trackCard(@NamedArg("cardCount") int cardCount) {
        ExceptionHelper.checkArgumentInRange(cardCount, 1, Integer.MAX_VALUE, "cardCount");

        return (World world, Player player) -> {
            Deck deck = player.getBoard().getDeck();

            UndoBuilder result = new UndoBuilder();
            List<CardDescr> choosenCards = new ArrayList<>(cardCount);
            for (int i = 0; i < cardCount; i++) {
                UndoableResult<CardDescr> cardRef = deck.tryDrawOneCard();
                if (cardRef == null) {
                    break;
                }

                result.addUndo(cardRef.getUndoAction());
                choosenCards.add(cardRef.getResult());
            }

            CardDescr chosenCard = chooseCard(world, choosenCards);
            if (chosenCard != null) {
                result.addUndo(player.getHand().addCard(chosenCard));
            }

            return result;
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

    public static PlayerAction reduceManaCostNextCard(
            @NamedArg("amount") int amount,
            @NamedArg("filter") AuraFilter<? super Player, ? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Player player) -> {
            ActivatableAbility<Player> aura = Auras.aura(
                    CardAuras.OWN_CARD_PROVIDER,
                    filter,
                    CardAuras.increaseManaCost(-amount));
            aura = deactivateAfterPlay(aura, filter);

            return aura.activate(player);
        };
    }

    public static PlayerAction setManaCostThisTurn(
            @NamedArg("manaCost") int manaCost,
            @NamedArg("filter") AuraFilter<? super Player, ? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Player player) -> {
            ActivatableAbility<Player> aura = Auras.aura(
                    CardAuras.OWN_CARD_PROVIDER,
                    filter,
                    CardAuras.setManaCost(manaCost));
            aura = deactivateAfterPlay(aura, filter);
            aura = ActionUtils.toSingleTurnAbility(world, aura);

            return aura.activate(player);
        };
    }

    public static PlayerAction reduceManaCostThisTurn(
            @NamedArg("amount") int amount,
            @NamedArg("filter") AuraFilter<? super Player, ? super Card> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, Player player) -> {
            ActivatableAbility<Player> aura = Auras.aura(
                    CardAuras.OWN_CARD_PROVIDER,
                    filter,
                    CardAuras.increaseManaCost(-amount));
            aura = deactivateAfterPlay(aura, filter);
            aura = ActionUtils.toSingleTurnAbility(world, aura);

            return aura.activate(player);
        };
    }

    public static PlayerAction equipRandomWeapon(@NamedArg("keywords") Keyword[] keywords) {
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (world, player) -> {
            List<WeaponDescr> weaponList = world.getDb().getWeaponDb().getByKeywords(keywordsCopy);
            WeaponDescr selected = ActionUtils.pickRandom(world, weaponList);
            return selected != null ? player.equipWeapon(selected) : UndoAction.DO_NOTHING;
        };
    }

    public static PlayerAction equipWeapon(@NamedArg("weapon") WeaponProvider weapon) {
        return (world, player) -> {
            return player.equipWeapon(weapon.getWeapon());
        };
    }

    public static PlayerAction buffWeapon(
            @NamedArg("attack") int attack,
            @NamedArg("charges") int charges) {
        return (world, player) -> {
            Weapon weapon = player.tryGetWeapon();
            if (weapon == null) {
                return UndoAction.DO_NOTHING;
            }

            UndoAction attackBuffUndo = weapon.getBuffableAttack().addBuff(attack);
            UndoAction incChargesUndo = weapon.increaseCharges(charges);

            return () -> {
                incChargesUndo.undo();
                attackBuffUndo.undo();
            };
        };
    }

    public static PlayerAction summonMinion(
            @NamedArg("minionCount") int minionCount,
            @NamedArg("minion") MinionProvider minion) {
        return summonMinion(minionCount, minionCount, minion);
    }

    public static PlayerAction summonMinion(
            @NamedArg("minMinionCount") int minMinionCount,
            @NamedArg("maxMinionCount") int maxMinionCount,
            @NamedArg("minion") MinionProvider minion) {
        return (world, player) -> {
            MinionDescr minionDescr = minion.getMinion();

            int minionCount = world.getRandomProvider().roll(minMinionCount, maxMinionCount);

            UndoBuilder result = new UndoBuilder(minionCount);
            for (int i = 0; i < minionCount; i++) {
                result.addUndo(player.summonMinion(minionDescr));
            }
            return result;
        };
    }

    public static PlayerAction summonRandomMinion(
            @NamedArg("keywords") Keyword[] keywords) {

        Function<World, MinionDescr> minionProvider = ActionUtils.randomMinionProvider(keywords);

        return (world, player) -> {
            MinionDescr minion = minionProvider.apply(world);
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }
            return player.summonMinion(minion);
        };
    }

    public static PlayerAction summonMinion(@NamedArg("minion") MinionProvider minion) {
        return (world, player) -> {
            return player.summonMinion(minion.getMinion());
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

    public static PlayerAction summonRandomTotem(@NamedArg("totems") MinionProvider... totems) {
        MinionProvider[] totemsCopy = totems.clone();
        ExceptionHelper.checkNotNullElements(totemsCopy, "totems");
        return (world, player) -> {
            return rollRandomTotem(world, player, totems);
        };
    }

    public static PlayerAction decreaseCostOfHand(@NamedArg("amount") int amount) {
        return (world, player) -> {
            return player.getHand().withCards((card) -> card.decreaseManaCost(amount));
        };
    }

    public static PlayerAction addThisTurnAbility(@NamedArg("ability") ActivatableAbility<? super Player> ability) {
        ExceptionHelper.checkNotNullArgument(ability, "ability");
        return (world, player) -> {
            return ActionUtils.doTemporary(world, () -> ability.activate(player));
        };
    }

    public static PlayerAction addHeroAttackForThisTurn(@NamedArg("attack") int attack) {
        return (world, player) -> {
            return player.getHero().addExtraAttackForThisTurn(attack);
        };
    }

    public static PlayerAction setHeroHp(@NamedArg("hp") int hp) {
        return (World world, Player player) -> {
            Hero hero = player.getHero();
            if (hero.getMaxHp() >= hp) {
                return hero.setCurrentHp(hp);
            }
            else {
                UndoAction maxHpUndo = hero.setMaxHp(hp);
                UndoAction currentHpUndo = hero.setCurrentHp(hp);
                return () -> {
                    currentHpUndo.undo();
                    maxHpUndo.undo();
                };
            }
        };
    }

    public static PlayerAction armorUp(@NamedArg("armor") int armor) {
        return (world, player) -> {
            return player.getHero().armorUp(armor);
        };
    }

    public static PlayerAction damageOwnHero(@NamedArg("damage") int damage) {
        return damageOwnHero(damage, true);
    }

    public static PlayerAction damageOwnHero(
            @NamedArg("damage") int damage,
            @NamedArg("spell") boolean spell) {
        return (world, player) -> {
            Hero hero = player.getHero();
            return ActionUtils.attackWithHero(hero, spell, damage, hero);
        };
    }

    public static PlayerAction damageOpponentHero(@NamedArg("damage") int damage) {
        return damageOpponentHero(damage, true);
    }

    public static PlayerAction damageOpponentHero(
            @NamedArg("damage") int damage,
            @NamedArg("spell") boolean spell) {
        return (world, player) -> {
            Player opponent = player.getOpponent();
            return ActionUtils.attackWithHero(player, spell, damage, opponent.getHero());
        };
    }

    private static PlayerAction killAllMinions() {
        MultiTargeter.Builder targeterBuilder = new MultiTargeter.Builder();
        targeterBuilder.setEnemy(true);
        targeterBuilder.setSelf(true);
        targeterBuilder.setMinions(true);

        MultiTargeter targeter = targeterBuilder.create();

        return (World world, Player player) -> {
            return targeter.forTargets(player, TargetableCharacter::poison);
        };
    }

    public static PlayerAction dealDamageToEnemyTargets(@NamedArg("damage") int damage) {
        return dealDamageToEnemyTargets(true, damage);
    }

    public static PlayerAction dealDamageToEnemyTargets(
            @NamedArg("spell") boolean spell,
            @NamedArg("damage") int damage) {
        return dealDamageTo(spell, true, false, true, true, damage < 0, damage);
    }

    public static PlayerAction dealDamageToEnemyMinions(@NamedArg("damage") int damage) {
        return dealDamageTo(true, true, false, true, false, damage < 0, damage);
    }

    public static PlayerAction dealDamageToEnemyMinions(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return dealDamageTo(true, true, false, true, false, minDamage < 0, (target) -> true, minDamage, maxDamage);
    }

    public static PlayerAction dealDamageTo(
            @NamedArg("spell") boolean spell,
            @NamedArg("enemy") boolean enemy,
            @NamedArg("self") boolean self,
            @NamedArg("minions") boolean minions,
            @NamedArg("heroes") boolean heroes,
            @NamedArg("atomic") boolean atomic,
            @NamedArg("damage") int damage) {
        return dealDamageTo(spell, enemy, self, minions, heroes, atomic, (target) -> true, damage);
    }

    private static PlayerAction dealDamageTo(
            boolean spell,
            boolean enemy,
            boolean self,
            boolean minions,
            boolean heroes,
            boolean atomic,
            Predicate<? super TargetableCharacter> filter,
            int damage) {
        return dealDamageTo(spell, enemy, self, minions, heroes, atomic, filter, damage, damage);
    }

    private static PlayerAction dealDamageTo(
            boolean spell,
            boolean enemy,
            boolean self,
            boolean minions,
            boolean heroes,
            boolean atomic,
            Predicate<? super TargetableCharacter> filter,
            int minDamage,
            int maxDamage) {

        MultiTargeter.Builder targeterBuilder = new MultiTargeter.Builder();
        targeterBuilder.setEnemy(enemy);
        targeterBuilder.setSelf(self);
        targeterBuilder.setMinions(minions);
        targeterBuilder.setHeroes(heroes);
        targeterBuilder.setAtomic(atomic);
        targeterBuilder.setCustomFilter(filter);

        MultiTargeter targeter = targeterBuilder.create();

        if (minDamage == maxDamage) {
            int damage = minDamage;
            return (World world, Player player) -> {
                Damage appliedDamage = spell ? player.getSpellDamage(damage) : player.getBasicDamage(damage);
                return targeter.forTargets(player, (target) -> target.damage(appliedDamage));
            };
        }
        else {
            return (World world, Player player) -> {
                return targeter.forTargets(player, (target) -> {
                    int damage = world.getRandomProvider().roll(minDamage, maxDamage);
                    Damage appliedDamage = spell ? player.getSpellDamage(damage) : player.getBasicDamage(damage);
                    return target.damage(appliedDamage);
                });
            };
        }
    }

    public static PlayerAction dealDamageToAllMinions(
            @NamedArg("damage") int damage,
            @NamedArg("excludedKeywords") Keyword[] excludedKeywords) {
        Predicate<LabeledEntity> filter = ActionUtils.excludedKeywordsFilter(excludedKeywords);
        return dealDamageTo(true, true, true, true, false, damage < 0, filter, damage);
    }

    public static PlayerAction dealDamageToAllMinions(@NamedArg("damage") int damage) {
        return dealDamageToAll(true, false, damage < 0, damage);
    }

    public static PlayerAction dealDamageToAllTargets(@NamedArg("damage") int damage) {
        return dealDamageToAllTargets(true, damage);
    }

    public static PlayerAction dealDamageToAllTargets(
            @NamedArg("damage") int damage,
            @NamedArg("excludedKeywords") Keyword[] excludedKeywords) {
        Predicate<LabeledEntity> filter = ActionUtils.excludedKeywordsFilter(excludedKeywords);
        return dealDamageTo(true, true, true, true, true, damage < 0, filter, damage);
    }

    public static PlayerAction dealDamageToAllTargets(
            @NamedArg("spell") boolean spell,
            @NamedArg("damage") int damage) {
        return dealDamageTo(spell, true, true, true, true, damage < 0, damage);
    }

    public static PlayerAction dealDamageToAll(
            @NamedArg("minions") boolean minions,
            @NamedArg("heroes") boolean heroes,
            @NamedArg("atomic") boolean atomic,
            @NamedArg("damage") int damage) {

        return dealDamageTo(true, true, true, minions, heroes, atomic, damage);
    }

    public static PlayerAction dealMissleDamage(
            @NamedArg("missleCount") int missleCount) {

        return (World world, Player player) -> {
            int appliedMissleCount = missleCount + player.getSpellPower().getValue();

            UndoBuilder result = new UndoBuilder(appliedMissleCount + 1);
            UndoableResult<Damage> damageRef = player.getHero().createDamage(1);
            result.addUndo(damageRef);
            for (int i = 0; i < appliedMissleCount; i++) {
                TargetableCharacter target = rollEnemyTarget(world, player);
                if (target == null) {
                    break;
                }
                result.addUndo(target.damage(damageRef.getResult()));
            }
            return result;
        };
    }

    public static PlayerAction addDeathRattleToAll(
            @NamedArg("action") WorldEventAction<? super Minion, ? super Minion> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (World world, Player player) -> {
            UndoBuilder result = new UndoBuilder();
            for (Minion target: player.getBoard().getAllMinions()) {
                UndoAction addUndo = target.addDeathRattle(action);
                result.addUndo(addUndo);
            }
            return result;
        };
    }

    public static PlayerAction addWeaponAttack(@NamedArg("attack") int attack) {
        return (World world, Player player) -> {
            Weapon weapon = player.tryGetWeapon();
            if (weapon != null) {
                return weapon.getBuffableAttack().addBuff(attack);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private static TargetableCharacter rollEnemyTarget(World world, Player player) {
        return ActionUtils.rollAlivePlayerTarget(world, player.getOpponent());
    }

    public static PlayerAction dealBasicDamageToRandomEnemy(@NamedArg("damage") int damage) {
        return dealBasicDamageToRandomEnemy(damage, damage);
    }

    public static PlayerAction dealBasicDamageToRandomEnemy(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        ExceptionHelper.checkArgumentInRange(maxDamage, minDamage, Integer.MAX_VALUE, "maxDamage");

        return (world, player) -> {
            TargetableCharacter character = rollEnemyTarget(world, player);
            if (character == null) {
                return UndoAction.DO_NOTHING;
            }

            int damage = world.getRandomProvider().roll(minDamage, maxDamage);
            return character.damage(player.getBasicDamage(damage));
        };
    }

    public static PlayerAction gainMana(@NamedArg("mana") int mana) {
        return (world, player) -> player.setMana(player.getMana() + mana);
    }

    public static PlayerAction addManaCrystal(@NamedArg("amount") int amount) {
        return addManaCrystal(true, amount);
    }

    public static PlayerAction addManaCrystal(
            @NamedArg("empty") boolean empty,
            @NamedArg("amount") int amount) {
        return (World world, Player player) -> {
            ManaResource manaResource = player.getManaResource();
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

    public static PlayerAction addManaCrystalToOpponent(@NamedArg("amount") int amount) {
        return addManaCrystalToOpponent(true, amount);
    }

    public static PlayerAction addManaCrystalToOpponent(
            @NamedArg("empty") boolean empty,
            @NamedArg("amount") int amount) {
        return doForOpponent(addManaCrystal(empty, amount));
    }

    private static PlayerAction activateAbilityForThisTurn(ActivatableAbility<? super Player> ability) {
        ExceptionHelper.checkNotNullArgument(ability, "ability");
        return (World world, Player player) -> {
            ActivatableAbility<? super Player> singleTurnAbility = ActionUtils.toSingleTurnAbility(world, ability);
            return singleTurnAbility.activate(player);
        };
    }

    public static <Target> PlayerAction untilTurnStartsAura(
            @NamedArg("target") AuraTargetProvider<? super Player, ? extends Target> target,
            @NamedArg("aura") Aura<? super Player, ? super Target> aura) {
        return untilTurnStartsAura(target, AuraFilter.ANY, aura);
    }

    public static <Target> PlayerAction untilTurnStartsAura(
            @NamedArg("target") AuraTargetProvider<? super Player, ? extends Target> target,
            @NamedArg("filter") AuraFilter<? super Player, ? super Target> filter,
            @NamedArg("aura") Aura<? super Player, ? super Target> aura) {

        return (World world, Player player) -> {
            return ActionUtils.doUntilNewTurnStart(player.getWorld(), player, () -> {
                return player.getWorld().addAura(new TargetedActiveAura<>(player, target, filter, aura));
            });
        };
    }

    public static PlayerAction replaceHero(
            @NamedArg("maxHp") int maxHp,
            @NamedArg("armor") int armor,
            @NamedArg("heroPower") String heroPower,
            @NamedArg("heroClass") Keyword heroClass) {
        return replaceHero(maxHp, armor, heroPower, heroClass, new Keyword[0]);
    }

    public static PlayerAction replaceHero(
            @NamedArg("maxHp") int maxHp,
            @NamedArg("armor") int armor,
            @NamedArg("heroPower") String heroPower,
            @NamedArg("heroClass") Keyword heroClass,
            @NamedArg("keywords") Keyword[] keywords) {
        ExceptionHelper.checkNotNullArgument(heroPower, "heroPower");
        ExceptionHelper.checkNotNullArgument(heroClass, "heroClass");

        List<Keyword> keywordsCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Player player) -> {
            Hero hero = new Hero(player, maxHp, armor, heroClass, keywordsCopy);
            hero.setHeroPower(world.getDb().getHeroPowerDb().getById(new HeroPowerId(heroPower)));

            return player.setHero(hero);
        };
    }

    private static <Self, Target> Predicate<Target> toPredicate(
            World world,
            Self self,
            WorldEventFilter<? super Self, ? super Target> filter) {
        return (target) -> filter.applies(world, self, target);
    }

    public static PlayerAction applyTargetedActionToRandomMinion(
            @NamedArg("action") TargetedAction action) {
        return applyTargetedActionToRandomMinion(action, false);
    }

    public static PlayerAction applyTargetedActionToRandomMinion(
            @NamedArg("action") TargetedAction action,
            @NamedArg("collectDying") boolean collectDying) {
        return applyTargetedActionToRandomMinion(action, collectDying, WorldEventFilter.ANY);
    }

    public static PlayerAction applyTargetedActionToRandomMinion(
            @NamedArg("action") TargetedAction action,
            @NamedArg("filter") WorldEventFilter<? super Player, ? super Minion> filter) {
        return applyTargetedActionToRandomMinion(action, false, filter);
    }

    public static PlayerAction applyTargetedActionToRandomMinion(
            @NamedArg("action") TargetedAction action,
            @NamedArg("collectDying") boolean collectDying,
            @NamedArg("filter") WorldEventFilter<? super Player, ? super Minion> filter) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        WorldEventFilter<? super Player, ? super Minion> appliedFilter = (world, owner, eventSource) -> {
            if (eventSource.isScheduledToDestroy()) {
                return false;
            }
            return collectDying || !eventSource.isDead();
        };

        return (World world, Player player) -> {
            List<Minion> candidates = new ArrayList<>(2 * Player.MAX_BOARD_SIZE);

            Predicate<Minion> minionFilter = toPredicate(world, player, appliedFilter);
            world.getPlayer1().getBoard().collectMinions(candidates, minionFilter);
            world.getPlayer2().getBoard().collectMinions(candidates, minionFilter);

            Minion minion = ActionUtils.pickRandom(world, candidates);
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }

            return action.alterWorld(world, new PlayTarget(player, minion));
        };
    }

    public static PlayerAction applyTargetedActionToRandomEnemyMinion(
            @NamedArg("action") TargetedAction action) {
        return applyTargetedActionToRandomEnemyMinion(action, WorldEventFilter.ANY);
    }

    public static PlayerAction applyTargetedActionToRandomEnemyMinion(
            @NamedArg("action") TargetedAction action,
            @NamedArg("filter") WorldEventFilter<? super Player, ? super Minion> filter) {
        return applyTargetedActionToRandomPlayerMinion(action, filter, Player::getOpponent);
    }

    public static PlayerAction applyTargetedActionToRandomOwnMinion(
            @NamedArg("action") TargetedAction action) {
        return applyTargetedActionToRandomOwnMinion(action, WorldEventFilter.ANY);
    }

    public static PlayerAction applyTargetedActionToRandomOwnMinion(
            @NamedArg("action") TargetedAction action,
            @NamedArg("filter") WorldEventFilter<? super Player, ? super Minion> filter) {
        return applyTargetedActionToRandomPlayerMinion(action, filter, Function.identity());
    }

    private static PlayerAction applyTargetedActionToRandomPlayerMinion(
            TargetedAction action,
            WorldEventFilter<? super Player, ? super Minion> filter,
            Function<Player, Player> playerGetter) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(playerGetter, "playerGetter");

        return (World world, Player player) -> {
            List<Minion> candidates = new ArrayList<>(2 * Player.MAX_BOARD_SIZE);

            Player selectedPlayer = playerGetter.apply(player);
            Predicate<Minion> minionFilter = toPredicate(world, player, filter);
            selectedPlayer.getBoard().collectAliveMinions(candidates, minionFilter);

            Minion minion = ActionUtils.pickRandom(world, candidates);
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }

            return action.alterWorld(world, new PlayTarget(player, minion));
        };
    }

    public static PlayerAction unleashMinions(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        return (World world, Player player) -> {
            int minionCount = player.getOpponent().getBoard().getMinionCount();

            MinionDescr toSummon = minion.getMinion();
            UndoBuilder result = new UndoBuilder(minionCount);
            for (int i = 0; i < minionCount; i++) {
                result.addUndo(player.summonMinion(toSummon));
            }
            return result;
        };
    }

    private PlayerActions() {
        throw new AssertionError();
    }
}
