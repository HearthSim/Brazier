package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.LabeledEntity;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.Priorities;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;

public final class CardAuras {
    public static final AuraFilter<PlayerProperty, PlayerProperty> SAME_OWNER = MinionAuras.SAME_OWNER;
    public static final AuraFilter<PlayerProperty, PlayerProperty> SAME_OWNER_OTHERS = MinionAuras.SAME_OWNER_OTHERS;

    public static final AuraFilter<Object, LabeledEntity> TARGET_IS_MINION = Auras.targetHasKeyword(Keywords.MINION);
    public static final AuraFilter<Object, LabeledEntity> TARGET_IS_SPELL = Auras.targetHasKeyword(Keywords.SPELL);
    public static final AuraFilter<Object, LabeledEntity> TARGET_HAS_BATTLE_CRY = Auras.targetHasKeyword(Keywords.BATTLE_CRY);

    public static final AuraTargetProvider<Object, Card> CARD_PROVIDER = (World world, Object source) -> {
        List<Card> result = new ArrayList<>(2 * Player.MAX_HAND_SIZE);
        world.getPlayer1().getHand().collectCards(result);
        world.getPlayer2().getHand().collectCards(result);
        return result;
    };

    public static final AuraTargetProvider<PlayerProperty, Card> OWN_CARD_PROVIDER = (World world, PlayerProperty source) -> {
        return source.getOwner().getHand().getCards();
    };

    public static final AuraTargetProvider<PlayerProperty, Card> OPPONENT_CARD_PROVIDER = (World world, PlayerProperty source) -> {
        return source.getOwner().getOpponent().getHand().getCards();
    };

    public static final AuraFilter<Object, Card> MINION_CARD = (world, source, target) -> {
        return target.getCardDescr().getMinion() != null;
    };

    public static Aura<Object, Card> increaseManaCost(@NamedArg("amount") int amount) {
        return (World world, Object source, Card target) -> {
            return target.getRawManaCost().addExternalBuff(amount);
        };
    }

    public static Aura<Object, Card> decreaseManaCostWithLimit(@NamedArg("amount") int amount) {
        return decreaseManaCostWithLimit(amount, 1);
    }

    public static Aura<Object, Card> decreaseManaCostWithLimit(
            @NamedArg("amount") int amount,
            @NamedArg("limit") int limit) {
        return (World world, Object source, Card target) -> {
            return target.getRawManaCost().addRemovableBuff(
                    Priorities.LOWEST_PRIORITY,
                    true,
                    (prevValue) -> Math.max(1, prevValue - amount));
        };
    }

    public static Aura<Object, Card> setManaCost(@NamedArg("manaCost") int manaCost) {
        return (World world, Object source, Card target) -> {
            return target.getRawManaCost().addExternalBuff((prevValue) -> 0);
        };
    }

    private CardAuras() {
        throw new AssertionError();
    }
}
