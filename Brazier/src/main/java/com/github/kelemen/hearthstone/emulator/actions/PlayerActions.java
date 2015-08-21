package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.brazier.actions.TargetlessActions;
import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.Deck;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.ManaResource;
import com.github.kelemen.hearthstone.emulator.MultiTargeter;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.RandomProvider;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.UndoableResult;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.cards.CardProvider;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponDescr;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;


public final class PlayerActions {
    public static final PlayerAction TRIGGER_DEATHRATTLES = (world, player) -> {
        return player.getBoard().forAllMinions((minion) -> minion.triggetDeathRattles());
    };

    public static final PlayerAction MIND_VISION = (world, player) -> {
        Card card = player.getOpponent().getHand().getRandomCard();
        if (card == null) {
            return UndoAction.DO_NOTHING;
        }
        return player.getHand().addCard(card.getCardDescr());
    };

    public static final PlayerAction KILL_ALL_MINIONS = killAllMinions();

    private static final ActivatableAbility<PlayerProperty> HERO_IS_IMMUNE = (PlayerProperty self) -> {
        return self.getOwner().getHero().getImmuneProperty().addRemovableBuff(true);
    };

    public static final PlayerAction HERO_IS_IMMUNE_THIS_TURN = activateAbilityForThisTurn(HERO_IS_IMMUNE);

    public static final PlayerAction REMOVE_OVERLOAD = (world, player) -> {
        ManaResource mana = player.getManaResource();
        UndoAction thisTurnUndo = mana.setOverloadedMana(0);
        UndoAction nextTurnUndo = mana.setNextTurnOverload(0);
        return () -> {
            nextTurnUndo.undo();
            thisTurnUndo.undo();
        };
    };

    public static final PlayerAction FISH_CARD_FOR_SELF = (world, player) -> {
        if (world.getRandomProvider().roll(2) < 1) {
            return player.drawCardToHand();
        }
        else {
            return UndoAction.DO_NOTHING;
        }
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

    public static PlayerAction stealFromOpponent(
            @NamedArg("cardCount") int cardCount) {
        return (World world, Player player) -> {
            Player opponent = player.getOpponent();
            List<Card> picked = ActionUtils.pickMultipleRandom(
                    world,
                    cardCount,
                    opponent.getBoard().getDeck().getCards());

            UndoBuilder result = new UndoBuilder(picked.size());
            Hand hand = player.getHand();
            for (Card card: picked) {
                result.addUndo(hand.addCard(card.getCardDescr()));
            }
            return result;
        };
    }

    public static PlayerAction doForOpponent(@NamedArg("action") PlayerAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        return (World world, Player player) -> {
            return action.alterWorld(world, player.getOpponent());
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
            return (world, player) -> TargetlessActions.DRAW_FOR_SELF.alterWorld(world, player);
        }

        return (World world, Player player) -> {
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

    public static PlayerAction summonRandomMinionFromDeck(
            boolean opponent,
            Predicate<? super Card> cardFilter) {
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        Predicate<Card> appliedFilter = (card) -> {
            return card.getMinion() != null && cardFilter.test(card);
        };

        return (World world, Player player) -> {
            Player summoner = opponent ? player.getOpponent() : player;
            if (summoner.getBoard().isFull()) {
                return UndoAction.DO_NOTHING;
            }

            UndoableResult<Card> cardRef = ActionUtils.pollDeckForCard(summoner, appliedFilter);
            if (cardRef == null) {
                return UndoAction.DO_NOTHING;
            }

            MinionDescr minion = cardRef.getResult().getMinion().getBaseDescr();

            UndoAction summonUndo = summoner.summonMinion(minion);
            return () -> {
                summonUndo.undo();
                cardRef.undo();
            };
        };
    }

    public static PlayerAction drawAndPlayCard(@NamedArg("keywords") Keyword[] keywords) {
        Predicate<LabeledEntity> cardFilter = ActionUtils.includedKeywordsFilter(keywords);
        return drawAndPlayCard(cardFilter);
    }

    public static PlayerAction drawAndPlayCard(Predicate<? super Card> cardFilter) {
        ExceptionHelper.checkNotNullArgument(cardFilter, "cardFilter");

        return (World world, Player player) -> {
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

    public static PlayerAction addCard(
            @NamedArg("card") CardProvider... card) {

        CardProvider[] cardCopy = card.clone();
        ExceptionHelper.checkNotNullElements(cardCopy, "card");
        ExceptionHelper.checkArgumentInRange(cardCopy.length, 1, Integer.MAX_VALUE, "card.length");

        return (World world, Player player) -> {
            Hand hand = player.getHand();
            CardDescr chosenCard = chooseCard(world, cardCopy);
            return hand.addCard(chosenCard);
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
                return hero.getHp().setMaxAndCurrentHp(hp);
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

    public static PlayerAction dealDamageToOwnTargets(@NamedArg("damage") int damage) {
        return dealDamageTo(true, false, true, true, true, damage < 0, damage);
    }

    public static PlayerAction dealDamageToOwnTargets(
            @NamedArg("minDamage") int minDamage,
            @NamedArg("maxDamage") int maxDamage) {
        return dealDamageTo(true, false, true, true, true, minDamage < 0, (target) -> true, minDamage, maxDamage);
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

    public static PlayerAction wildGrowth(
            @NamedArg("amount") int amount,
            @NamedArg("excessCard") CardProvider card) {
        return wildGrowth(true, amount, card);
    }

    public static PlayerAction wildGrowth(
            @NamedArg("empty") boolean empty,
            @NamedArg("amount") int amount,
            @NamedArg("excessCard") CardProvider card) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        return (World world, Player player) -> {
            ManaResource manaResource = player.getManaResource();
            if (manaResource.getManaCrystals() >= Player.MAX_MANA) {
                return player.getHand().addCard(card.getCard());
            }

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

    public static PlayerAction killAndReplaceMinions(
            @NamedArg("minion") MinionProvider minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        return (World world, Player player) -> {
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
            hero.setHeroPower(world.getDb().getHeroPowerDb().getById(new CardId(heroPower)));

            return player.setHero(hero);
        };
    }

    public static PlayerAction replaceHeroPower(
            @NamedArg("heroPower") CardId[] heroPower) {
        ExceptionHelper.checkArgumentInRange(heroPower.length, 1, Integer.MAX_VALUE, "heroPower.length");
        CardId[] heroPowerCopy = heroPower.clone();
        ExceptionHelper.checkNotNullElements(heroPowerCopy, "heroPower");


        return (World world, Player player) -> {
            Hero hero = player.getHero();

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
