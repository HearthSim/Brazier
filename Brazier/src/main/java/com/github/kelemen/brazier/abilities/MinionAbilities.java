package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.TargetlessAction;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.WorldActionEvents;
import com.github.kelemen.brazier.actions.WorldEventFilter;
import com.github.kelemen.brazier.event.WorldEvents;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.parsing.NamedArg;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

public final class MinionAbilities {
    public static <Self extends PlayerProperty> ActivatableAbility<Self> startOfTurnBuff(
            @NamedArg("filter") WorldEventFilter<? super Self, ? super Player> filter,
            @NamedArg("action") TargetlessAction<? super Self> action) {
        return triggerBuff(WorldEvents::turnStartsListeners, filter, action);
    }

    public static <Self extends PlayerProperty> ActivatableAbility<Self> endOfTurnBuff(
            @NamedArg("filter") WorldEventFilter<? super Self, ? super Player> filter,
            @NamedArg("action") TargetlessAction<? super Self> action) {
        return triggerBuff(WorldEvents::turnEndsListeners, filter, action);
    }

    public static <Self extends PlayerProperty, T> ActivatableAbility<Self> triggerBuff(
            Function<? super WorldEvents, WorldActionEvents<T>> listenersGetter,
            WorldEventFilter<? super Self, ? super T> filter,
            TargetlessAction<? super Self> action) {
        ExceptionHelper.checkNotNullArgument(listenersGetter, "listenersGetter");
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(action, "action");

        return (Self self) -> {
            WorldActionEvents<T> listeners = listenersGetter.apply(self.getWorld().getEvents());
            return listeners.addAction((World world, T object) -> {
                if (filter.applies(world, self, object)) {
                    return action.alterWorld(world, self);
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
