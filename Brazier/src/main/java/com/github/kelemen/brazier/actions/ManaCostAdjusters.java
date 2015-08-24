package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Hero;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.parsing.NamedArg;
import com.github.kelemen.brazier.weapons.Weapon;

public final class ManaCostAdjusters {
    public static final ManaCostAdjuster REDUCE_BY_WEAPON_ATTACK  = (Card card, int currentManaCost) -> {
        Player player = card.getOwner();
        Weapon weapon = player.tryGetWeapon();
        if (weapon != null) {
            return Math.max(0, currentManaCost - weapon.getAttack());
        }
        else {
            return currentManaCost;
        }
    };

    public static final ManaCostAdjuster REDUCE_BY_BOARD_POPULATION  = (Card card, int currentManaCost) -> {
        World world = card.getOwner().getWorld();
        int manaReduction = world.getPlayer1().getBoard().getMinionCount()
                + world.getPlayer2().getBoard().getMinionCount();
        return currentManaCost - manaReduction;
    };

    public static final ManaCostAdjuster REDUCE_BY_HERO_DAMAGE  = (Card card, int currentManaCost) -> {
        Hero hero = card.getOwner().getHero();
        int manaReduction = hero.getMaxHp() - hero.getCurrentHp();
        return currentManaCost - manaReduction;
    };

    public static final ManaCostAdjuster REDUCE_BY_HAND_SIZE = reduceByHandSize(0);
    public static final ManaCostAdjuster REDUCE_BY_HAND_SIZE_PLUS_ONE = reduceByHandSize(1);

    public static final ManaCostAdjuster REDUCE_BY_OPPONENTS_HAND_SIZE  = (Card card, int currentManaCost) -> {
        Player owner = card.getOwner();
        Player opponent = owner.getWorld().getOpponent(owner.getPlayerId());
        Hero hero = opponent.getHero();
        int manaReduction = hero.getOwner().getHand().getCardCount();
        return currentManaCost - manaReduction;
    };

    public static final ManaCostAdjuster REDUCE_BY_DEATH_THIS_TURN  = (Card card, int currentManaCost) -> {
        World world = card.getWorld();
        int deathCount = world.getPlayer1().getBoard().getGraveyard().getNumberOfMinionsDiedThisTurn()
                + world.getPlayer2().getBoard().getGraveyard().getNumberOfMinionsDiedThisTurn();
        return currentManaCost - deathCount;
    };


    public static ManaCostAdjuster reduceByHandSize(@NamedArg("extraCost") int extraCost) {
        return (Card card, int currentManaCost) -> {
            Hero hero = card.getOwner().getHero();
            int manaReduction = hero.getOwner().getHand().getCardCount();
            return currentManaCost - manaReduction + extraCost;
        };
    }

    public static ManaCostAdjuster reduceIfHaveDamaged(@NamedArg("reduction") int reduction) {
        return (Card card, int currentManaCost) -> {
            int damagedCount = card.getOwner().getBoard().countMinions(Minion::isDamaged);
            return damagedCount > 0 ? currentManaCost - reduction : currentManaCost;
        };
    }

    private ManaCostAdjusters() {
        throw new AssertionError();
    }
}
