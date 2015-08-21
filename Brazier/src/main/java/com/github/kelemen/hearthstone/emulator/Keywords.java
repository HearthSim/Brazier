package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.cards.CardRarity;

public final class Keywords {
    public static final Keyword MINION = Keyword.create("minion");
    public static final Keyword SPELL = Keyword.create("spell");
    public static final Keyword WEAPON = Keyword.create("weapon");
    public static final Keyword HERO_POWER = Keyword.create("hero-power");

    public static final Keyword SPARE_PART = Keyword.create("spare-part");
    public static final Keyword BATTLE_CRY = Keyword.create("battlecry");
    public static final Keyword OVERLOAD = Keyword.create("overload");

    public static final Keyword COLLECTIBLE = Keyword.create("collectible");
    public static final Keyword NON_COLLECTIBLE = Keyword.create("non-collectible");

    public static final Keyword CLASS_NEUTRAL = Keyword.create("neutral");
    public static final Keyword CLASS_DRUID = Keyword.create("druid");
    public static final Keyword CLASS_HUNTER = Keyword.create("hunter");
    public static final Keyword CLASS_MAGE = Keyword.create("mage");
    public static final Keyword CLASS_PALADIN = Keyword.create("paladin");
    public static final Keyword CLASS_PRIEST = Keyword.create("priest");
    public static final Keyword CLASS_ROUGE = Keyword.create("rouge");
    public static final Keyword CLASS_SHAMAN = Keyword.create("shaman");
    public static final Keyword CLASS_WARLOCK = Keyword.create("warlock");
    public static final Keyword CLASS_WARRIOR = Keyword.create("warrior");
    public static final Keyword CLASS_BOSS = Keyword.create("boss-class");

    public static final Keyword RARITY_COMMON = Keyword.create(CardRarity.COMMON.name());
    public static final Keyword RARITY_RARE = Keyword.create(CardRarity.RARE.name());
    public static final Keyword RARITY_EPIC = Keyword.create(CardRarity.EPIC.name());
    public static final Keyword RARITY_LEGENDARY = Keyword.create(CardRarity.LEGENDARY.name());

    public static final Keyword RACE_BEAST = Keyword.create("beast");
    public static final Keyword RACE_MECH = Keyword.create("mech");
    public static final Keyword RACE_DEMON = Keyword.create("demon");
    public static final Keyword RACE_DRAGON = Keyword.create("dragon");
    public static final Keyword RACE_PIRATE = Keyword.create("pirate");
    public static final Keyword RACE_TOTEM = Keyword.create("totem");
    public static final Keyword RACE_MURLOC = Keyword.create("murloc");

    public static final Keyword SECRET = Keyword.create("secret");

    public static final Keyword HORDE_WARRIOR = Keyword.create("horde-warrior");

    public static final Keyword BRAWLER = Keyword.create("brawler");

    public static Keyword manaCost(int manaCost) {
        return Keyword.create(manaCost + "-cost");
    }

    private Keywords() {
        throw new AssertionError();
    }
}
