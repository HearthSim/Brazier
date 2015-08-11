package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldActionEvents;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventFilter;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.function.Function;

public final class MinionAbilities {
    public static <Self extends PlayerProperty> ActivatableAbility<Self> startOfTurnBuff(
            @NamedArg("filter") WorldEventFilter<? super Self, ? super Player> filter,
            @NamedArg("action") WorldEventAction<? super Self, ? super Player> action) {
        return triggerBuff(WorldEvents::turnStartsListeners, filter, action);
    }

    public static <Self extends PlayerProperty> ActivatableAbility<Self> endOfTurnBuff(
            @NamedArg("filter") WorldEventFilter<? super Self, ? super Player> filter,
            @NamedArg("action") WorldEventAction<? super Self, ? super Player> action) {
        return triggerBuff(WorldEvents::turnEndsListeners, filter, action);
    }

    public static <Self extends PlayerProperty, T> ActivatableAbility<Self> triggerBuff(
            Function<? super WorldEvents, WorldActionEvents<T>> listenersGetter,
            WorldEventFilter<? super Self, ? super T> filter,
            WorldEventAction<? super Self, ? super T> action) {
        return (Self self) -> {
            WorldActionEvents<T> listeners = listenersGetter.apply(self.getWorld().getEvents());
            return listeners.addAction((World world, T object) -> {
                if (filter.applies(world, self, object)) {
                    return action.alterWorld(world, self, object);
                }
                else {
                    return UndoAction.DO_NOTHING;
                }
            });
        };
    }

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
