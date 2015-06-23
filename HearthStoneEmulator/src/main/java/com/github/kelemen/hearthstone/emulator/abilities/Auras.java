package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.BoardSide;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class Auras {
    public static final AuraFilter<PlayerProperty, PlayerProperty> SAME_OWNER = (world, source, target) -> {
        return source.getOwner() == target.getOwner();
    };

    public static final AuraFilter<PlayerProperty, Object> NOT_PLAYED_MINION_THIS_TURN = (world, source, target) -> {
        return source.getOwner().getMinionsPlayedThisTurn() == 0;
    };

    public static final AuraFilter<PlayerProperty, Object> OWNER_HAS_WEAPON = (world, source, target) -> {
        return source.getOwner().tryGetWeapon() != null;
    };

    public static final AuraFilter<PlayerProperty, PlayerProperty> NOT_SELF = (world, source, target) -> {
        return source != target;
    };

    public static AuraFilter<Object, LabeledEntity> targetHasKeyword(@NamedArg("keywords") Keyword... keywords) {
        List<Keyword> keywordsCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (World world, Object source, LabeledEntity target) -> {
            return target.getKeywords().containsAll(keywordsCopy);
        };
    }

    public static AuraFilter<Object, LabeledEntity> targetDoesntHaveKeyword(@NamedArg("keywords") Keyword... keywords) {
        Predicate<LabeledEntity> targetFilter = ActionUtils.excludedKeywordsFilter(keywords);

        return (World world, Object source, LabeledEntity target) -> {
            return targetFilter.test(target);
        };
    }

    public static AuraFilter<PlayerProperty, Object> ownBoardHas(@NamedArg("keywords") Keyword... keywords) {
        Predicate<LabeledEntity> minionFilter = ActionUtils.excludedKeywordsFilter(keywords);

        return (World world, PlayerProperty source, Object target) -> {
            BoardSide board = source.getOwner().getBoard();
            return board.findMinion(minionFilter) != null;
        };
    }

    public static AuraFilter<PlayerProperty, Object> opponentsHandLarger(@NamedArg("limit") int limit) {
        return (World world, PlayerProperty source, Object target) -> {
            return source.getOwner().getOpponent().getHand().getCardCount() > limit;
        };
    }

    private Auras() {
        throw new AssertionError();
    }
}
