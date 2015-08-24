package com.github.kelemen.brazier.minions;

import com.github.kelemen.brazier.Damage;
import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.UndoableIntResult;
import com.github.kelemen.brazier.abilities.AuraAwareBoolProperty;
import com.github.kelemen.brazier.abilities.AuraAwareIntProperty;
import com.github.kelemen.brazier.abilities.HpProperty;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.event.DamageEvent;
import com.github.kelemen.brazier.event.WorldEvents;
import org.jtrim.utils.ExceptionHelper;

public final class MinionBody implements Silencable {
    private final Minion owner;
    private final MinionDescr baseStats;

    private final HpProperty hp;
    private boolean poisoned;

    private boolean taunt;
    private boolean divineShield;
    private final AuraAwareBoolProperty stealth;
    private final AuraAwareBoolProperty untargetable;
    private final AuraAwareBoolProperty immune;
    private final AuraAwareIntProperty minHp;

    public MinionBody(Minion owner, MinionDescr baseStats) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(baseStats, "baseStats");

        this.owner = owner;
        this.baseStats = baseStats;

        this.hp = new HpProperty(baseStats.getHp());
        this.taunt = baseStats.isTaunt();
        this.divineShield = baseStats.isDivineShield();
        this.poisoned = false;
        this.untargetable = new AuraAwareBoolProperty(!baseStats.isTargetable());
        this.stealth = new AuraAwareBoolProperty(baseStats.isStealth());
        this.immune = new AuraAwareBoolProperty(false);
        this.minHp = new AuraAwareIntProperty(Integer.MIN_VALUE);
    }

    private MinionBody(Minion owner, MinionBody other) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(other, "other");

        this.owner = owner;
        this.baseStats = other.baseStats;
        this.hp = other.hp.copy();
        this.poisoned = other.poisoned;
        this.taunt = other.taunt;
        this.divineShield = other.divineShield;
        this.stealth = other.stealth.copy();
        this.untargetable = other.untargetable.copy();
        this.immune = other.immune.copy();
        this.minHp = other.minHp.copy();
    }

    public MinionBody copyFor(Minion minion) {
        return new MinionBody(minion, this);
    }

    public UndoAction poison() {
        if (poisoned) {
            return UndoAction.DO_NOTHING;
        }

        poisoned = true;
        return () -> poisoned = false;
    }

    public AuraAwareBoolProperty getUntargetableProperty() {
        return untargetable;
    }

    public AuraAwareBoolProperty getStealthProperty() {
        return stealth;
    }

    public AuraAwareBoolProperty getImmuneProperty() {
        return immune;
    }

    public AuraAwareIntProperty getMinHpProperty() {
        return minHp;
    }

    public boolean isTargetable() {
        return !untargetable.getValue();
    }

    public MinionDescr getBaseStats() {
        return baseStats;
    }

    public HpProperty getHp() {
        return hp;
    }

    public int getCurrentHp() {
        return hp.getCurrentHp();
    }

    public int getMaxHp() {
        return hp.getMaxHp();
    }

    public boolean isTaunt() {
        return taunt;
    }

    public boolean isDead() {
        return poisoned || hp.isDead();
    }

    public boolean isDivineShield() {
        return divineShield;
    }

    public boolean isStealth() {
        return stealth.getValue();
    }

    public boolean isImmune() {
        return immune.getValue();
    }

    public UndoAction setDivineShield(boolean newValue) {
        if (divineShield == newValue) {
            return UndoAction.DO_NOTHING;
        }
        boolean prevValue = divineShield;
        divineShield = newValue;
        return () -> divineShield = prevValue;
    }

    public UndoAction setStealth(boolean newValue) {
        return stealth.setValue(newValue);
    }

    public UndoAction setTaunt(boolean newValue) {
        if (newValue == taunt) {
            return UndoAction.DO_NOTHING;
        }

        boolean prevValue = taunt;
        taunt = newValue;
        return () -> taunt = prevValue;
    }

    public UndoableIntResult damage(Damage damage) {
        int attack = damage.getAttack();
        if (attack == 0) {
            return UndoableIntResult.ZERO;
        }
        if (attack > 0 && immune.getValue()) {
            return UndoableIntResult.ZERO;
        }

        if (divineShield && attack > 0) {
            divineShield = false;
            return new UndoableIntResult(0, () -> divineShield = true);
        }

        int currentHp = hp.getCurrentHp();
        int newHp = currentHp - attack;
        newHp = Math.max(minHp.getValue(), Math.min(newHp, hp.getMaxHp()));
        UndoAction hpUndo = hp.setCurrentHp(newHp);
        int damageDone = currentHp - newHp;

        WorldEvents events = owner.getWorld().getEvents();
        UndoAction eventUndo;
        DamageEvent event = new DamageEvent(damage.getSource(), owner, damageDone);
        if (damageDone < 0) {
            eventUndo = events.minionHealedListeners().triggerEvent(event);
        }
        else {
            eventUndo = events.minionDamagedListeners().triggerEvent(event);
        }

        return new UndoableIntResult(event.getDamageDealt(), () -> {
            eventUndo.undo();
            hpUndo.undo();
        });
    }

    @Override
    public UndoAction silence() {
        final boolean prevDivineShield = divineShield;
        final boolean prevTaunt = taunt;

        UndoAction stealthSilenceUndo = stealth.silence();
        UndoAction silenceUntargetableUndo = untargetable.silence();
        taunt = false;
        divineShield = false;

        UndoAction hpSilenceUndo = hp.silence();

        return () -> {
            hpSilenceUndo.undo();
            divineShield = prevDivineShield;
            taunt = prevTaunt;
            silenceUntargetableUndo.undo();
            stealthSilenceUndo.undo();
        };
    }

    public boolean isLethalDamage(int damage) {
        if (divineShield || isImmune()) {
            return false;
        }
        return damage >= getCurrentHp();
    }

    public UndoAction applyAuras() {
        return hp.applyAura();
    }
}
