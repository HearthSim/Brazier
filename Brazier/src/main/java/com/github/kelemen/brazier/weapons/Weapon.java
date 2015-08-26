package com.github.kelemen.brazier.weapons;

import com.github.kelemen.brazier.CharacterAbilities;
import com.github.kelemen.brazier.Damage;
import com.github.kelemen.brazier.DamageSource;
import com.github.kelemen.brazier.DestroyableEntity;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.LabeledEntity;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.UndoableResult;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.abilities.ActivatableAbilities;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.abilities.AuraAwareIntProperty;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.events.SimpleEventType;
import com.github.kelemen.brazier.events.WorldActionEvents;
import com.github.kelemen.brazier.events.WorldEventAction;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.utils.ExceptionHelper;

public final class Weapon implements DestroyableEntity, DamageSource, LabeledEntity {
    private final Player owner;
    private final WeaponDescr baseDescr;
    private final CharacterAbilities<Weapon> abilities;
    private final ActivatableAbility<Weapon> deathRattle;
    private final long birthDate;

    private final AuraAwareIntProperty attack;
    private int charges;

    private final AtomicBoolean scheduledToDestroy;

    public Weapon(Player owner, WeaponDescr weaponDescr) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(weaponDescr, "weaponDescr");

        this.owner = owner;
        this.baseDescr = weaponDescr;
        this.attack = new AuraAwareIntProperty(weaponDescr.getAttack());
        this.charges = weaponDescr.getCharges();
        this.birthDate = owner.getOwner().getWorld().getCurrentTime();
        this.abilities = new CharacterAbilities<>(this);
        this.scheduledToDestroy = new AtomicBoolean(false);

        WorldEventAction<? super Weapon, ? super Weapon> deathRattleAction = baseDescr.tryGetDeathRattle();
        this.deathRattle = deathRattleAction != null ? deathRattleToAbility(deathRattleAction) : null;
    }

    public UndoAction activatePassiveAbilities() {
        ActivatableAbilities<Weapon> ownedAbilities = abilities.getOwned();

        UndoBuilder result = new UndoBuilder();

        result.addUndo(ownedAbilities.addAndActivateAbility(baseDescr.getEventActionDefs()));

        ActivatableAbility<? super Weapon> ability = baseDescr.tryGetAbility();
        if (ability != null) {
            result.addUndo(ownedAbilities.addAndActivateAbility(ability));
        }

        if (deathRattle != null) {
            result.addUndo(ownedAbilities.addAndActivateAbility(deathRattle));
        }

        return result;
    }

    public UndoAction deactivateAllAbilities() {
        return abilities.deactivateAll();
    }

    @Override
    public UndoAction scheduleToDestroy() {
        if (!scheduledToDestroy.compareAndSet(false, true)) {
            return UndoAction.DO_NOTHING;
        }
        return () -> scheduledToDestroy.set(false);
    }

    @Override
    public boolean isScheduledToDestroy() {
        return scheduledToDestroy.get();
    }

    @Override
    public Set<Keyword> getKeywords() {
        return getBaseDescr().getKeywords();
    }

    @Override
    public long getBirthDate() {
        return birthDate;
    }

    @Override
    public Player getOwner() {
        return owner.getOwner();
    }

    public WeaponDescr getBaseDescr() {
        return baseDescr;
    }

    @Override
    public UndoableResult<Damage> createDamage(int damage) {
        return new UndoableResult<>(new Damage(this, damage));
    }

    public UndoAction increaseCharges() {
        return increaseCharges(1);
    }

    public UndoAction increaseCharges(int amount) {
        if (charges == Integer.MAX_VALUE || amount == 0) {
            return UndoAction.DO_NOTHING;
        }

        charges += amount;
        return () -> charges -= amount;
    }

    public UndoAction decreaseCharges() {
        if (charges == Integer.MAX_VALUE) {
            return UndoAction.DO_NOTHING;
        }

        charges--;
        return () -> charges++;
    }

    public int getAttack() {
        return attack.getValue();
    }

    public AuraAwareIntProperty getBuffableAttack() {
        return attack;
    }

    public UndoAction setAttack(int attack) {
        return this.attack.setValueTo(attack);
    }

    public int getCharges() {
        return charges;
    }

    public boolean canRetaliateWith() {
        return baseDescr.canRetaliateWith();
    }

    public boolean canTargetRetaliate() {
        return baseDescr.canTargetRetaliate();
    }

    @Override
    public UndoAction destroy() {
        UndoAction eventUndo = owner.getWorld().getEvents().triggerEvent(SimpleEventType.WEAPON_DESTROYED, this);

        // TODO: If we want to deactivate the abilities first, we have to
        //       adjust death-rattle handling not to get disabled.
        UndoAction deactivateUndo = deactivateAllAbilities();

        return () -> {
            deactivateUndo.undo();
            eventUndo.undo();
        };
    }

    private static ActivatableAbility<Weapon> deathRattleToAbility(
            WorldEventAction<? super Weapon, ? super Weapon> deathRattle) {
        ExceptionHelper.checkNotNullArgument(deathRattle, "deathRattle");

        return (Weapon self) -> {
            WorldActionEvents<Weapon> listeners = self.getWorld().getEvents()
                    .simpleListeners(SimpleEventType.WEAPON_DESTROYED, Weapon.class);
            return listeners.addAction((World world, Weapon target) -> {
                if (target == self) {
                    return deathRattle.alterWorld(world, self, target);
                }
                else {
                    return UndoAction.DO_NOTHING;
                }
            });
        };
    }

    @Override
    public String toString() {
        return "Weapon{" + ", attack=" + attack + ", charges=" + charges + '}';
    }
}
