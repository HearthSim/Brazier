package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.LabeledEntity;
import com.github.kelemen.hearthstone.emulator.PlayerId;
import com.github.kelemen.hearthstone.emulator.PlayerPredicate;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TargetNeeds {
    private static final PlayerPredicate<TargetableCharacter> CHARACTER_DAMAGED = (playerId, character) -> {
        return character.isDamaged();
    };

    public static final TargetNeed TARGET_DAMAGED = new TargetNeed(CHARACTER_DAMAGED, CHARACTER_DAMAGED);

    public static final TargetNeed IS_TAUNT = new TargetNeed(PlayerPredicate.ANY, (PlayerId playerId, Minion arg) -> {
        return arg.getBody().isTaunt();
    });

    public static TargetNeed attackIsLessThan(@NamedArg("attack") int attack) {
        PlayerPredicate<TargetableCharacter> filter = (playerId, character) -> {
            return character.getAttackTool().getAttack() < attack;
        };
        return new TargetNeed(filter, filter);
    }

    public static TargetNeed attackIsMoreThan(@NamedArg("attack") int attack) {
        PlayerPredicate<TargetableCharacter> filter = (playerId, character) -> {
            return character.getAttackTool().getAttack() > attack;
        };
        return new TargetNeed(filter, filter);
    }

    public static TargetNeed hasKeyword(@NamedArg("keywords") Keyword[] keywords) {
        List<Keyword> keywordsCopy = new ArrayList<>(Arrays.asList(keywords));
        PlayerPredicate<LabeledEntity> filter = (playerId, target) -> {
            return target.getKeywords().containsAll(keywordsCopy);
        };
        return new TargetNeed(filter, filter);
    }

    private TargetNeeds() {
        throw new AssertionError();
    }
}
