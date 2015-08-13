package com.github.kelemen.hearthstone.emulator.cards;

import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;

public enum CardType {
    SPELL(Keywords.SPELL),
    MINION(Keywords.MINION),
    WEAPON(Keywords.WEAPON),
    HERO_POWER(Keywords.HERO_POWER),
    UNKNOWN(Keyword.create("unknown-card-type"));

    private final Keyword keyword;

    private CardType(Keyword keyword) {
        this.keyword = keyword;
    }

    public Keyword getKeyword() {
        return keyword;
    }
}
