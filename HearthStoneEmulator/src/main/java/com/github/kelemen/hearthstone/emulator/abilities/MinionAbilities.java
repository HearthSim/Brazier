package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldActionEvents;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventFilter;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

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
            AuraAwareIntProperty playersSpellPower = self.getOwner().getSpellPower();
            return playersSpellPower.addAuraBuff(spellPower);
        };
    }

    public static ActivatableAbility<Minion> selfAura(
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return selfAura(AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> selfAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return aura(MinionAuras.SELF_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> neighboursAura(
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return neighboursAura(AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> neighboursAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return aura(MinionAuras.NEIGHBOURS_MINION_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> sameBoardOthersAura(
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return sameBoardOthersAura(AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> sameBoardOthersAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return sameBoardAura(AuraFilter.and(MinionAuras.SAME_OWNER_OTHERS, filter), aura);
    }

    public static ActivatableAbility<Minion> sameBoardAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return aura(MinionAuras.SAME_BOARD_MINION_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> boardAura(
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return aura(MinionAuras.MINION_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> boardAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Minion> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Minion> aura) {
        return aura(MinionAuras.MINION_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> ownCardAura(
            @NamedArg("aura") Aura<? super Minion, ? super Card> aura) {
        return aura(CardAuras.OWN_CARD_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> ownCardAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Card> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Card> aura) {
        return aura(CardAuras.OWN_CARD_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> cardAura(
            @NamedArg("aura") Aura<? super Minion, ? super Card> aura) {
        return aura(CardAuras.CARD_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> cardAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Card> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Card> aura) {
        return aura(CardAuras.CARD_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> ownHeroAura(
            @NamedArg("aura") Aura<? super Minion, ? super Hero> aura) {
        return aura(HeroAuras.OWN_HERO_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> ownHeroAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Hero> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Hero> aura) {
        return aura(HeroAuras.OWN_HERO_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> heroAura(
            @NamedArg("aura") Aura<? super Minion, ? super Hero> aura) {
        return aura(HeroAuras.HERO_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> heroAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Hero> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Hero> aura) {
        return aura(HeroAuras.HERO_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> ownPlayerAura(
            @NamedArg("aura") Aura<? super Minion, ? super Player> aura) {
        return aura(HeroAuras.OWN_PLAYER_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> ownPlayerAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Player> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Player> aura) {
        return aura(HeroAuras.OWN_PLAYER_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> playerAura(
            @NamedArg("aura") Aura<? super Minion, ? super Player> aura) {
        return aura(HeroAuras.PLAYER_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> playerAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Player> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Player> aura) {
        return aura(HeroAuras.PLAYER_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> ownWeaponAura(
            @NamedArg("aura") Aura<? super Minion, ? super Weapon> aura) {
        return aura(WeaponAuras.OWN_WEAPON_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> ownWeaponAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Weapon> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Weapon> aura) {
        return aura(WeaponAuras.OWN_WEAPON_PROVIDER, filter, aura);
    }

    public static ActivatableAbility<Minion> weaponAura(
            @NamedArg("aura") Aura<? super Minion, ? super Weapon> aura) {
        return aura(WeaponAuras.WEAPON_PROVIDER, AuraFilter.ANY, aura);
    }

    public static ActivatableAbility<Minion> weaponAura(
            @NamedArg("filter") AuraFilter<? super Minion, ? super Weapon> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Weapon> aura) {
        return aura(WeaponAuras.WEAPON_PROVIDER, filter, aura);
    }

    public static <Target> ActivatableAbility<Minion> aura(
            @NamedArg("target") AuraTargetProvider<? super Minion, ? extends Target> target,
            @NamedArg("filter") AuraFilter<? super Minion, ? super Target> filter,
            @NamedArg("aura") Aura<? super Minion, ? super Target> aura) {

        ExceptionHelper.checkNotNullArgument(target, "target");
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(aura, "aura");

        return (Minion self) -> self.getWorld().addAura(new TargetedActiveAura<>(self, target, filter, aura));
    }

    private MinionAbilities() {
        throw new AssertionError();
    }
}
