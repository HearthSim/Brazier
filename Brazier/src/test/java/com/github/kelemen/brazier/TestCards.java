package com.github.kelemen.brazier;

import com.github.kelemen.brazier.minions.MinionId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

public final class TestCards {
    public static final String ABUSIVE_SERGEANT = "Abusive Sergeant";
    public static final String ALARM_O_BOT = "Alarm-o-Bot";
    public static final String ALDOR_PEACEKEEPER = "Aldor Peacekeeper";
    public static final String ANCIENT_MAGE = "Ancient Mage";
    public static final String AVENGE = "Avenge";
    public static final String BLACKWING_CORRUPTOR = "Blackwing Corruptor";
    public static final String BLACKWING_TECHNICIAN = "Blackwing Technician";
    public static final String BLESSING_OF_KINGS = "Blessing of Kings";
    public static final String BLESSING_OF_WISDOM = "Blessing of Wisdom";
    public static final String BLUEGILL_WARRIOR = "Bluegill Warrior";
    public static final String BOLVAR_FORDRAGON = "Bolvar Fordragon";
    public static final String CONE_OF_COLD = "Cone of Cold";
    public static final String CONSECRATION = "Consecration";
    public static final String CULT_MASTER = "Cult Master";
    public static final String DAMAGED_GOLEM = "Damaged Golem";
    public static final String DARKBOMB = "Darkbomb";
    public static final String DEATHS_BITE = "Death's Bite";
    public static final String DEFIAS_BANDIT = "Defias Bandit";
    public static final String DEFIAS_RINGLEADER = "Defias Ringleader";
    public static final String DIRE_WOLF_ALPHA = "Dire Wolf Alpha";
    public static final String DREAD_CORSAIR = "Dread Corsair";
    public static final String EMPEROR_COBRA = "Emperor Cobra";
    public static final String EMPEROR_THAURISSAN = "Emperor Thaurissan";
    public static final String EVISCERATE = "Eviscerate";
    public static final String EXECUTE = "Execute";
    public static final String EXPLOSIVE_SHEEP = "Explosive Sheep";
    public static final String EXPLOSIVE_SHOT = "Explosive Shot";
    public static final String EXPLOSIVE_TRAP = "Explosive Trap";
    public static final String FACELESS_MANIPULATOR = "Faceless Manipulator";
    public static final String FIERY_WAR_AXE = "Fiery War Axe";
    public static final String FINICKY_CLOAKFIELD = "Finicky Cloakfield";
    public static final String FIREBALL = "Fireball";
    public static final String FIRE_ELEMENTAL = "Fire Elemental";
    public static final String FLAME_OF_AZZINOTH = "Flame of Azzinoth";
    public static final String FLAMESTRIKE = "Flamestrike";
    public static final String FREEZING_TRAP = "Freezing Trap";
    public static final String FROTHING_BERSERKER = "Frothing Berserker";
    public static final String FROST_NOVA = "Frost Nova";
    public static final String GRIM_PATRON = "Grim Patron";
    public static final String GURUBASHI_BERSERKER = "Gurubashi Berserker";
    public static final String HARVEST_GOLEM = "Harvest Golem";
    public static final String HAUNTED_CREEPER = "Haunted Creeper";
    public static final String HEADCRACK = "Headcrack";
    public static final String HELLFIRE = "Hellfire";
    public static final String HUNTERS_MARK = "Hunter's Mark";
    public static final String ICE_BARRIER = "Ice Barrier";
    public static final String ILLIDAN_STORMRAGE = "Illidan Stormrage";
    public static final String JARAXXUS = "Lord Jaraxxus";
    public static final String KEZAN_MYSTIC = "Kezan Mystic";
    public static final String KOBOLD_GEOMANCER = "Kobold Geomancer";
    public static final String KORKRON_ELITE = "Kor'kron Elite";
    public static final String LOST_TALLSTRIDER = "Lost Tallstrider";
    public static final String MALGANIS = "Mal'Ganis";
    public static final String MALYGOS = "Malygos";
    public static final String MANA_WRAITH = "Mana Wraith";
    public static final String MIRROR_ENTITY = "Mirror Entity";
    public static final String MOONFIRE = "Moonfire";
    public static final String PREPARATION = "Preparation";
    public static final String PYROBLAST = "Pyroblast";
    public static final String REDEMPTION = "Redemption";
    public static final String REPENTANCE = "Repentance";
    public static final String RESURRECT = "Resurrect";
    public static final String REVERSING_SWITCH = "Reversing Switch";
    public static final String SHIELD_BLOCK = "Shield Block";
    public static final String SCARLET_CRUSADER = "Scarlet Crusader";
    public static final String SHADOW_MADNESS = "Shadow Madness";
    public static final String SHADOW_STEP = "Shadowstep";
    public static final String SHATTERED_SUN_CLERIC = "Shattered Sun Cleric";
    public static final String SILENCE = "Silence";
    public static final String SLAM = "Slam";
    public static final String SLIME = "Slime";
    public static final String SLUDGE_BELCHER = "Sludge Belcher";
    public static final String SNIPE = "Snipe";
    public static final String SPECTRAL_SPIDER = "Spectral Spider";
    public static final String SOUL_OF_THE_FOREST = "Soul of the Forest";
    public static final String STARVING_BUZZARD = "Starving Buzzard";
    public static final String STRANGLETHORN_TIGER = "Stranglethorn Tiger";
    public static final String STONETUSK_BOAR = "Stonetusk Boar";
    public static final String STORMWIND_CHAMPION = "Stormwind Champion";
    public static final String STORMWIND_KNIGHT = "Stormwind Knight";
    public static final String SYLVANAS_WINDRUNNER = "Sylvanas Windrunner";
    public static final String THE_COIN = "The Coin";
    public static final String TRACKING = "Tracking";
    public static final String TREANT = "Treant";
    public static final String VOIDCALLER = "Voidcaller";
    public static final String VOIDWALKER = "Voidwalker";
    public static final String WARSONG_COMMANDER = "Warsong Commander";
    public static final String WATER_ELEMENTAL = "Water Elemental";
    public static final String WEE_SPELLSTOPPER = "Wee Spellstopper";
    public static final String WHIRLWIND = "Whirlwind";
    public static final String WISP = "Wisp";
    public static final String YETI = "Chillwind Yeti";

    public static MinionExpectations expectedMinion(String name, int attack, int hp) {
        MinionExpectations.Builder result = new MinionExpectations.Builder(new MinionId(name));
        result.setAttack(attack);
        result.setHp(hp);
        return result.create();
    }

    public static MinionExpectations expectedMinion(String name, int attack, int hp, boolean canAttackWith) {
        MinionExpectations.Builder result = new MinionExpectations.Builder(new MinionId(name));
        result.setAttack(attack);
        result.setHp(hp);
        result.setCanAttackWith(canAttackWith);
        return result.create();
    }

    private static MinionExpectations expectedMinionWithFlags(
            String name,
            int attack,
            int hp,
            MinionFlags... flags) {

        MinionExpectations.Builder result = new MinionExpectations.Builder(new MinionId(name));
        result.setFlags(new HashSet<>(Arrays.asList(flags)));
        result.setAttack(attack);
        result.setHp(hp);
        return result.create();
    }

    public static MinionExpectations expectedMinionWithFlags(
            String name,
            int attack,
            int hp,
            String... flags) {

        MinionFlags[] parsedFlags = new MinionFlags[flags.length];
        for (int i = 0; i < parsedFlags.length; i++) {
            parsedFlags[i] = MinionFlags.valueOf(flags[i].toUpperCase(Locale.ROOT));
        }
        return expectedMinionWithFlags(name, attack, hp, parsedFlags);
    }

    private TestCards() {
        throw new AssertionError();
    }
}
