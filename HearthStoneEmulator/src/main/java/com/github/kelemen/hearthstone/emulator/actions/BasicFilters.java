package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.BoardSide;
import com.github.kelemen.hearthstone.emulator.CardPlayEvent;
import com.github.kelemen.hearthstone.emulator.DamageEvent;
import com.github.kelemen.hearthstone.emulator.DamageRequest;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetRef;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class BasicFilters {
    public static final WorldEventFilter<Object, Object> ANY
            = (world, owner, eventSource) -> true;
    public static final WorldEventFilter<Object, Object> SELF
            = (world, owner, eventSource) -> owner == eventSource;
    public static final WorldEventFilter<Object, Object> NOT_SELF
            = (world, owner, eventSource) -> owner != eventSource;
    public static final WorldEventFilter<Object, DamageEvent> DAMAGE_SOURCE_SELF
            = (world, owner, eventSource) -> owner == eventSource.getDamageSource();
    public static final WorldEventFilter<Object, TargetRef> TARGET_SELF
            = (world, owner, eventSource) -> owner == eventSource.getTarget();
    public static final WorldEventFilter<PlayerProperty, Object> SELF_TURN
            = (world, owner, eventSource) -> owner.getOwner().getWorld().getCurrentPlayer() == owner.getOwner();
    public static final WorldEventFilter<PlayerProperty, Object> NOT_SELF_TURN
            = (world, owner, eventSource) -> !SELF_TURN.applies(world, owner, SELF);
    public static final WorldEventFilter<PlayerProperty, CardPlayEvent> CARD_TARGET_IS_HERO
            = (world, owner, eventSource) -> eventSource.getTarget() instanceof Hero;
    public static final WorldEventFilter<PlayerProperty, CardPlayEvent> CARD_TARGET_IS_MINION
            = (world, owner, eventSource) -> eventSource.getTarget() instanceof Minion;
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACKER_IS_SELF
            = (world, owner, eventSource) -> owner == eventSource.getAttacker();
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACKER_IS_HERO
            = (world, owner, eventSource) -> eventSource.getAttacker() instanceof Hero;
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACKER_IS_MINION
            = (world, owner, eventSource) -> eventSource.getAttacker() instanceof Minion;
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACK_TARGET_IS_MINION
            = (world, owner, eventSource) -> eventSource.getOriginalTarget()instanceof Minion;
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACK_TARGET_IS_OWN_HERO
            = (world, owner, eventSource) -> owner.getOwner().getHero() == eventSource.getOriginalTarget();
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACK_TARGET_IS_OWNER
            = (world, owner, eventSource) -> owner.getOwner() == eventSource.getOriginalTarget().getOwner();
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACK_TARGET_IS_ENEMY
            = (world, owner, eventSource) -> owner.getOwner() != eventSource.getOriginalTarget().getOwner();
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACKER_IS_OWNER
            = (world, owner, eventSource) -> owner.getOwner() == eventSource.getAttacker().getOwner();
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACKER_IS_ENEMY
            = (world, owner, eventSource) -> owner.getOwner() != eventSource.getAttacker().getOwner();
    public static final WorldEventFilter<PlayerProperty, AttackRequest> ATTACKER_IS_ALIVE
            = (world, owner, eventSource) -> !eventSource.getAttacker().isDead();
    public static final WorldEventFilter<PlayerProperty, DamageRequest> PREPARED_DAMAGE_IS_LETHAL = (world, owner, eventSource) -> {
        return eventSource.getTarget().isLethalDamage(eventSource.getDamage().getAttack());
    };
    public static final WorldEventFilter<PlayerProperty, Object> HAS_SECRET = (world, owner, eventSource) -> {
        return owner.getOwner().getSecrets().hasSecret();
    };
    public static final WorldEventFilter<PlayerProperty, Object> SELF_BOARD_IS_NOT_FULL = (world, owner, eventSource) -> {
        return !owner.getOwner().getBoard().isFull();
    };
    public static final WorldEventFilter<PlayerProperty, TargetRef> DAMAGE_TARGET_IS_OWN_HERO
            = (world, owner, eventSource) -> owner.getOwner().getHero() == eventSource.getTarget();

    public static final WorldEventFilter<Weapon, Object> SOURCE_WEAPON_HAS_CHARGE = (world, owner, eventSource) -> {
        return owner.getCharges() > 0;
    };

    public static final WorldEventFilter<PlayerProperty, TargetableCharacter> EVENT_SOURCE_IS_NOT_DAMAGED
            = (world, owner, eventSource) -> !eventSource.isDamaged();

    public static final WorldEventFilter<Object, TargetRef> TARGET_SURVIVES = (world, owner, eventSource) -> {
        return !eventSource.getTarget().isDead();
    };

    public static final WorldEventFilter<PlayerProperty, PlayerProperty> HAS_DIFFERENT_OWNER_PLAYER = (world, owner, eventSource) -> {
        return owner.getOwner() != eventSource.getOwner();
    };

    public static final WorldEventFilter<PlayerProperty, PlayerProperty> HAS_SAME_OWNER_PLAYER = (world, owner, eventSource) -> {
        return owner.getOwner() == eventSource.getOwner();
    };

    public static final WorldEventFilter<PlayerProperty, Minion> HAS_OTHER_OWNED_BUFF_TARGET = (world, owner, eventSource) -> {
        BoardSide board = owner.getOwner().getBoard();
        return board.findMinion((minion) -> minion.notScheduledToDestroy() && minion != eventSource) != null;
    };

    public static final WorldEventFilter<PlayerProperty, TargetRef> TARGET_HAS_SAME_OWNER_PLAYER = (world, owner, eventSource) -> {
        return owner.getOwner() == eventSource.getTarget().getOwner();
    };

    public static final WorldEventFilter<PlayerProperty, TargetableCharacter> EVENT_SOURCE_DAMAGED = (world, owner, eventSource) -> {
        return eventSource.isDamaged();
    };

     public static final WorldEventFilter<PlayerProperty, AttackRequest> HAS_MISSDIRECT_TARGET = (world, self, eventSource) -> {
         return hasValidTarget(world, validMisdirectTarget(eventSource));
     };

    public static final WorldEventFilter<Object, LabeledEntity> EVENT_SOURCE_IS_SPELL = eventSourceHasKeyword(Keywords.SPELL);
    public static final WorldEventFilter<Object, LabeledEntity> EVENT_SOURCE_IS_SECRET = eventSourceHasKeyword(Keywords.SECRET);

    public static final WorldEventFilter<Object, Minion> SUMMONED_DEATH_RATTLE = (world, owner, eventSource) -> {
        return eventSource.getProperties().isDeathRattle();
    };

    public static final WorldEventFilter<PlayerProperty, Object> OWNER_HAS_SECRET = (world, owner, eventSource) -> {
        return owner.getOwner().getSecrets().getSecrets().size() > 0;
    };

    private static boolean hasValidTarget(
            World world,
            Predicate<? super TargetableCharacter> filter) {
        return hasValidTarget(world.getPlayer1(), filter)
                || hasValidTarget(world.getPlayer2(), filter);
    }

    private static boolean hasValidTarget(
            Player player,
            Predicate<? super TargetableCharacter> filter) {
        if (filter.test(player.getHero())) {
            return true;
        }
        return player.getBoard().findMinion(filter) != null;
    }

    public static Predicate<TargetableCharacter> validMisdirectTarget(AttackRequest request) {
        return validMisdirectTarget(request.getOriginalTarget(), request.getAttacker());
    }

    public static Predicate<TargetableCharacter> validMisdirectTarget(TargetableCharacter attacker, TargetableCharacter defender) {
        return (target) -> {
             if (target == attacker || target == defender) {
                 return false;
             }
             if (target instanceof Minion) {
                 if (((Minion)target).getBody().isStealth()) {
                     return false;
                 }
             }
             return true;
        };
    }

    public static WorldEventFilter<Object, Object> minionDiedWithKeyword(
            @NamedArg("keywords") Keyword[] keywords) {
        Keyword[] keywordsCopy = keywords.clone();
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Object owner, Object eventSource) -> {
            return world.getPlayer1().getBoard().getGraveyard().hasWithKeyword(keywordsCopy)
                    || world.getPlayer2().getBoard().getGraveyard().hasWithKeyword(keywordsCopy);
        };
    }

    public static WorldEventFilter<PlayerProperty, Object> ownBoardSizeIsLess(@NamedArg("minionCount") int minionCount) {
        return (World world, PlayerProperty owner, Object eventSource) -> {
            return owner.getOwner().getBoard().getMinionCount() < minionCount;
        };
    }

    public static WorldEventFilter<Object, TargetableCharacter> targetAttackIsLess(@NamedArg("attack") int attack) {
        return (World world, Object owner, TargetableCharacter eventSource)
                -> eventSource.getAttackTool().getAttack()< attack;
    }

    public static WorldEventFilter<Object, LabeledEntity> eventSourceHasKeyword(@NamedArg("keywords") Keyword... keywords) {
        List<Keyword> requiredKeywords = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(requiredKeywords, "keywords");

        return (World world, Object owner, LabeledEntity eventSource) -> {
            return eventSource.getKeywords().containsAll(requiredKeywords);
        };
    }

    public static WorldEventFilter<Object, CardRef> cardMinionAttackEquals(@NamedArg("attack") int attack) {
        return (World world, Object owner, CardRef eventSource) -> {
            Minion minion = eventSource.getCard().getMinion();
            if (minion == null) {
                return false;
            }
            return minion.getAttackTool().getAttack() == attack;
        };
    }

    public static WorldEventFilter<Object, CardPlayRef> manaCostEquals(@NamedArg("manaCost") int manaCost) {
        return (World world, Object owner, CardPlayRef eventSource)
                -> eventSource.getManaCost() == manaCost;
    }

    public static WorldEventFilter<Object, LabeledEntity> targetHasKeyword(@NamedArg("keywords") Keyword... keywords) {
        List<Keyword> keywordsCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Object source, LabeledEntity target) -> {
            return target.getKeywords().containsAll(keywordsCopy);
        };
    }

    public static WorldEventFilter<Object, LabeledEntity> targetDoesntHaveKeyword(@NamedArg("keywords") Keyword... keywords) {
        Predicate<LabeledEntity> targetFilter = ActionUtils.excludedKeywordsFilter(keywords);

        return (World world, Object source, LabeledEntity target) -> {
            return targetFilter.test(target);
        };
    }

    public static WorldEventFilter<PlayerProperty, Object> handSizeIsLess(@NamedArg("size") int size) {
        return (World world, PlayerProperty owner, Object eventSource)
                -> owner.getOwner().getHand().getCardCount() < size;
    }

    private BasicFilters() {
        throw new AssertionError();
    }
}
