package com.github.kelemen.brazier.minions;

import com.github.kelemen.brazier.Damage;
import com.github.kelemen.brazier.DestroyableEntity;
import com.github.kelemen.brazier.Hero;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PreparedResult;
import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.SummonLocationRef;
import com.github.kelemen.brazier.TargetId;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.TargeterDef;
import com.github.kelemen.brazier.UndoableIntResult;
import com.github.kelemen.brazier.UndoableResult;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.abilities.AuraAwareIntProperty;
import com.github.kelemen.brazier.actions.CardRef;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.actions.WorldEventAction;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.event.WorldEvents;
import com.github.kelemen.brazier.weapons.AttackTool;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.utils.ExceptionHelper;

public final class Minion implements TargetableCharacter, DestroyableEntity, Silencable, CardRef {
    private Player owner;
    private final TargetId minionId;
    private MinionProperties properties;

    private SummonLocationRef locationRef;
    private final long birthDate;

    private final AtomicBoolean scheduledToDestroy;
    private final AtomicBoolean destroyed;

    public Minion(Player owner, MinionDescr baseDescr) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(baseDescr, "baseDescr");

        this.owner = owner;
        this.minionId = new TargetId();
        this.properties = new MinionProperties(this, baseDescr);
        this.locationRef = null;
        this.birthDate = owner.getWorld().getCurrentTime();
        this.destroyed = new AtomicBoolean(false);
        this.scheduledToDestroy = new AtomicBoolean(false);
    }

    @Override
    public UndoAction scheduleToDestroy() {
        if (!scheduledToDestroy.compareAndSet(false, true)) {
            return UndoAction.DO_NOTHING;
        }

        UndoAction deactivateUndo = getProperties().deactivateAllAbilities();
        return () -> {
            deactivateUndo.undo();
            scheduledToDestroy.set(false);
        };
    }

    @Override
    public Card getCard() {
        return new Card(owner, getBaseDescr().getBaseCard());
    }

    public boolean notScheduledToDestroy() {
        return !isScheduledToDestroy();
    }

    @Override
    public boolean isScheduledToDestroy() {
        return scheduledToDestroy.get();
    }

    @Override
    public UndoAction destroy() {
        return getLocationRef().destroy();
    }

    public UndoAction transformTo(MinionDescr newDescr) {
        ExceptionHelper.checkNotNullArgument(newDescr, "newDescr");

        UndoBuilder result = new UndoBuilder();

        result.addUndo(properties.deactivateAllAbilities());

        MinionProperties prevProperties = properties;
        properties = new MinionProperties(this, newDescr);
        result.addUndo(() -> properties = prevProperties);

        result.addUndo(properties.activatePassiveAbilities());

        return result;
    }

    public UndoAction copyOther(Minion other) {
        ExceptionHelper.checkNotNullArgument(other, "other");

        UndoBuilder result = new UndoBuilder();

        result.addUndo(properties.deactivateAllAbilities());

        PreparedResult<MinionProperties> copiedProperties = other.properties.copyFor(this);

        MinionProperties prevProperties = properties;
        properties = copiedProperties.getResult();
        result.addUndo(() -> properties = prevProperties);

        result.addUndo(copiedProperties.activate());

        result.addUndo(properties.exhaust());

        return result;
    }

    public UndoAction exhaust() {
        return properties.exhaust();
    }

    public boolean isCharge() {
        return properties.isCharge();
    }

    @Override
    public long getBirthDate() {
        return birthDate;
    }

    @Override
    public boolean isTargetable(TargeterDef targeterDef) {
        boolean sameOwner = targeterDef.hasSameOwner(this);
        if (!sameOwner && getBody().isStealth()) {
            return false;
        }
        if (sameOwner && targeterDef.isDirectAttack()) {
            return false;
        }

        MinionBody body = properties.getBody();

        if (!body.isTargetable() && targeterDef.isHero()) {
            return false;
        }

        if (body.isTaunt()) {
            return true;
        }

        return !targeterDef.isDirectAttack() || !getOwner().getBoard().hasNonStealthTaunt();
    }

    public UndoAction addDeathRattle(WorldEventAction<? super Minion, ? super Minion> deathRattle) {
        return properties.addDeathRattle(deathRattle);
    }

    public UndoAction addAndActivateAbility(ActivatableAbility<? super Minion> abilityRegisterTask) {
        return properties.addAndActivateAbility(abilityRegisterTask);
    }

    public UndoAction addAttackBuff(int attack) {
        return properties.addAttackBuff(attack);
    }

    public UndoAction activatePassiveAbilities() {
        return properties.activatePassiveAbilities();
    }

    public MinionProperties getProperties() {
        return properties;
    }

    private UndoAction triggerKilledEvents() {
        WorldEvents events = getOwner().getWorld().getEvents();
        return events.minionKilledListeners().triggerEvent(this);
    }

    @Override
    public Set<Keyword> getKeywords() {
        return getBaseDescr().getBaseCard().getKeywords();
    }

    private UndoAction triggerDeathRattleOnDeath() {
        if (!properties.isDeathRattle()) {
            return UndoAction.DO_NOTHING;
        }

        int triggerCount = getOwner().getDeathRattleTriggerCount().getValue();
        return properties.triggetDeathRattles(triggerCount);
    }

    public UndoAction completeKillAndDeactivate(boolean triggerKill) {
        if (destroyed.compareAndSet(false, true)) {
            UndoAction eventUndo = triggerKill
                    ? triggerKilledEvents()
                    : UndoAction.DO_NOTHING;

            UndoAction deactivateUndo = properties.deactivateAllAbilities();
            UndoAction deathRattleUndo = triggerKill
                    ? triggerDeathRattleOnDeath()
                    : UndoAction.DO_NOTHING;

            return () -> {
                deathRattleUndo.undo();
                deactivateUndo.undo();
                eventUndo.undo();
                destroyed.set(false);
            };
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    }

    public UndoAction triggetDeathRattles() {
        return properties.triggetDeathRattles();
    }

    @Override
    public UndoAction silence() {
        return properties.silence();
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public UndoAction poison() {
        return getBody().poison();
    }

    public void setOwner(Player owner) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        this.owner = owner;
    }

    public AuraAwareIntProperty getBuffableAttack() {
        return properties.getBuffableAttack();
    }

    @Override
    public AttackTool getAttackTool() {
        return properties.getAttackTool();
    }

    /**
     * Returns {@code true} if this minion has been completely destroyed and might no longer
     * be added to the board.
     *
     * @return {@code true} if this minion has been completely destroyed and might no longer
     *   be added to the board, {@code false} otherwise
     */
    public boolean isDestroyed() {
        return destroyed.get();
    }

    @Override
    public boolean isDead() {
        return getBody().isDead();
    }

    @Override
    public boolean isDamaged() {
        MinionBody body = getBody();
        return body.getCurrentHp() < body.getMaxHp();
    }

    public SummonLocationRef getLocationRef() {
        return locationRef != null
                ? locationRef
                : SummonLocationRef.ignoreSummon(this);
    }

    public UndoAction setLocationRef(SummonLocationRef locationRef) {
        ExceptionHelper.checkNotNullArgument(locationRef, "locationRef");

        if (locationRef.getMinion() != this) {
            throw new IllegalArgumentException("LocationRef must contain this minion.");
        }

        SummonLocationRef prevRef = this.locationRef;
        this.locationRef = locationRef;

        return () -> this.locationRef = prevRef;
    }

    @Override
    public TargetId getTargetId() {
        return minionId;
    }

    public MinionDescr getBaseDescr() {
        return getBody().getBaseStats();
    }

    public MinionBody getBody() {
        return properties.getBody();
    }

    public UndoAction setCharge(boolean newCharge) {
        return properties.setCharge(newCharge);
    }

    @Override
    public UndoableResult<Damage> createDamage(int damage) {
        int preparedDamage = damage;
        if (damage < 0 && getOwner().getDamagingHealAura().getValue()) {
            preparedDamage = -damage;
        }
        return new UndoableResult<>(new Damage(this, preparedDamage), act());
    }

    @Override
    public boolean isLethalDamage(int damage) {
        return getBody().isLethalDamage(damage);
    }

    @Override
    public UndoableIntResult damage(Damage damage) {
        return Hero.doPreparedDamage(damage, this, (appliedDamage) -> getBody().damage(appliedDamage));
    }

    public UndoAction refresh() {
        return properties.refresh();
    }

    public UndoAction refreshEndOfTurn() {
        return properties.refreshEndOfTurn();
    }

    public UndoAction applyAuras() {
        return properties.updateAuras();
    }

    private UndoAction act() {
        return getBody().setStealth(false);
    }

    @Override
    public String toString() {
        MinionBody body = getBody();
        AttackTool attackTool = getAttackTool();
        int attack = attackTool.getAttack();

        MinionId id = body.getBaseStats().getId();
        int currentHp = body.getCurrentHp();

        StringBuilder result = new StringBuilder(64);
        result.append("Minion(");
        result.append(id);
        result.append(") ");
        result.append(attack);
        result.append("/");
        result.append(currentHp);

        if (body.isTaunt()) {
            result.append(" ");
            result.append(" TAUNT");
        }

        if (properties.isFrozen()) {
            result.append(" ");
            result.append(" FROZEN");
        }

        if (!body.isTargetable()) {
            result.append(" ");
            result.append(" UNTARGETABLE");
        }

        return result.toString();
    }
}
