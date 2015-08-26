package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.parsing.NamedArg;

public final class MinionAbilities {
    public static ActivatableAbility<PlayerProperty> spellPower(@NamedArg("spellPower") int spellPower) {
        return (PlayerProperty self) -> {
            BuffableIntProperty playersSpellPower = self.getOwner().getSpellPower();
            return playersSpellPower.addBuff(spellPower);
        };
    }

    public static ActivatableAbility<PlayerProperty> spellMultiplier(@NamedArg("mul") int mul) {
        return (PlayerProperty self) -> {
            BuffableIntProperty playersSpellPower = self.getOwner().getHeroDamageMultiplier();
            return playersSpellPower.addBuff((prev) -> prev * mul);
        };
    }

    public static ActivatableAbility<Minion> neighboursAura(
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return neighboursAura(AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> neighboursAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return Auras.aura(MinionAuras.NEIGHBOURS_MINION_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> sameBoardOthersAura(
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return sameBoardOthersAura(AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> sameBoardOthersAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return Auras.sameBoardAura(AuraFilter.and(MinionAuras.SAME_OWNER_OTHERS, filter), aura);
    }

    private MinionAbilities() {
        throw new AssertionError();
    }
}
