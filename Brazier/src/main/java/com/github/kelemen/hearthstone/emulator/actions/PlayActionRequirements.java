package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.BoardSide;
import com.github.kelemen.hearthstone.emulator.Hand;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class PlayActionRequirements {
    public static final PlayActionRequirement COMBO = (player) -> {
        return player.getCardsPlayedThisTurn() > 0;
    };

    public static final PlayActionRequirement NO_COMBO = (player) -> !COMBO.meetsRequirement(player);

    public static final PlayActionRequirement HAS_SPACE_ON_OWN_BOARD = (player) -> {
        return !player.getBoard().isFull();
    };

    public static final PlayActionRequirement OPPONENT_BOARD_NOT_EMPTY = opponentBoardIsLarger(0);

    public static final PlayActionRequirement BOARD_IS_EMPTY = (player) -> {
        World world = player.getWorld();
        return world.getPlayer1().getBoard().getMinionCount() <= 0
                && world.getPlayer2().getBoard().getMinionCount() <= 0 ;
    };

    public static final PlayActionRequirement BOARD_IS_NOT_EMPTY = not(BOARD_IS_EMPTY);

    public static final PlayActionRequirement EMPTY_HAND = (player) -> {
        return player.getHand().getCardCount() == 0;
    };

    public static final PlayActionRequirement HAS_WEAPON = (player) -> {
        return player.tryGetWeapon() != null;
    };

    public static final PlayActionRequirement DOESN_HAVE_WEAPON = not(HAS_WEAPON);

    public static PlayActionRequirement hasCardInHand(@NamedArg("keywords") Keyword... keywords) {
        ArrayList<Keyword> keywordCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordCopy, "keywords");

        return (Player player) -> {
            Hand hand = player.getHand();
            return hand.findCard((card) -> card.getKeywords().containsAll(keywordCopy)) != null;
        };
    }

    public static PlayActionRequirement stealBattleCryNeeds(@NamedArg("maxAttack") int maxAttack) {
        return (Player player) -> {
            BoardSide opponentBoard = player.getOpponent().getBoard();

            boolean hasTarget = opponentBoard
                    .findMinion((minion) -> minion.getAttackTool().getAttack() <= maxAttack) != null;
            if (hasTarget) {
                return player.getBoard().getMinionCount() + 1 < Player.MAX_BOARD_SIZE;
            }
            else {
                return !player.getBoard().isFull();
            }
        };
    }

    public static PlayActionRequirement hasOnOwnBoard(@NamedArg("keywords") Keyword... keywords) {
        ArrayList<Keyword> keywordCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordCopy, "keywords");

        return (Player player) -> {
            return player.getBoard().findMinion((minion) -> minion.getKeywords().containsAll(keywordCopy)) != null;
        };
    }

    public static PlayActionRequirement doesntHaveOnOwnBoard(@NamedArg("keywords") Keyword... keywords) {
        return not(hasOnOwnBoard(keywords));
    }

    public static PlayActionRequirement not(@NamedArg("condition") PlayActionRequirement condition) {
        return (player) -> !condition.meetsRequirement(player);
    }

    public static PlayActionRequirement doesntHaveAllOnOwnBoard(@NamedArg("minions") MinionId[] minions) {
        List<MinionId> minionsCopy = new ArrayList<>(Arrays.asList(minions));
        ExceptionHelper.checkNotNullElements(minionsCopy, "minions");

        return (Player player) -> {
            BoardSide board = player.getBoard();
            Set<MinionId> remaining = new HashSet<>(minionsCopy);
            for (Minion minion: board.getAllMinions()) {
                remaining.remove(minion.getBaseDescr().getId());
            }
            return !remaining.isEmpty();
        };
    }

    public static PlayActionRequirement opponentBoardIsLarger(@NamedArg("minionCount") int minionCount) {
        return (Player player) -> {
            return player.getOpponent().getBoard().getMinionCount() > minionCount;
        };
    }

    public static PlayActionRequirement opponentsHpIsLess(@NamedArg("hp") int hp) {
        return (Player player)
                -> player.getOpponent().getHero().getCurrentHp() < hp;
    }

    public static PlayActionRequirement ownHpIsLess(@NamedArg("hp") int hp) {
        return (Player player) -> player.getHero().getCurrentHp() < hp;
    }

    public static PlayActionRequirement ownHpIsMore(@NamedArg("hp") int hp) {
        return (Player player) -> player.getHero().getCurrentHp() > hp;
    }

    public static PlayActionRequirement hasPlayerFlag(@NamedArg("flag") Keyword flag) {
        ExceptionHelper.checkNotNullArgument(flag, "flag");
        return (Player player) -> player.getAuraFlags().hasFlag(flag);
    }

    public static PlayActionRequirement doesntHavePlayerFlag(@NamedArg("flag") Keyword flag) {
        ExceptionHelper.checkNotNullArgument(flag, "flag");
        return (Player player) -> !player.getAuraFlags().hasFlag(flag);
    }


    private PlayActionRequirements() {
        throw new AssertionError();
    }
}
