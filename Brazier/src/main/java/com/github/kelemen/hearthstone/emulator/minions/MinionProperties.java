package com.github.kelemen.hearthstone.emulator.minions;

import com.github.kelemen.hearthstone.emulator.CharacterAbilities;
import com.github.kelemen.hearthstone.emulator.FreezeManager;
import com.github.kelemen.hearthstone.emulator.PreparedResult;
import com.github.kelemen.hearthstone.emulator.Silencable;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.abilities.AuraAwareBoolProperty;
import com.github.kelemen.hearthstone.emulator.abilities.AuraAwareIntProperty;
import com.github.kelemen.hearthstone.emulator.abilities.OwnedIntPropertyBuff;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.weapons.AttackTool;
import java.util.ArrayList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public final class MinionProperties implements Silencable {
    private final Minion minion;
    private final MinionAttackTool attackTool;
    private final MinionBody body;
    private final CharacterAbilities<Minion> abilities;
    private List<WorldEventAction<? super Minion, ? super Minion>> deathRattles;
    private boolean activated;

    public MinionProperties(Minion minion, MinionDescr baseDescr) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        ExceptionHelper.checkNotNullArgument(baseDescr, "baseDescr");

        this.minion = minion;
        this.attackTool = new MinionAttackTool(baseDescr);
        this.body = new MinionBody(minion, baseDescr);
        this.abilities = new CharacterAbilities<>(minion);
        this.deathRattles = new ArrayList<>();
        this.activated = false;

        WorldEventAction<? super Minion, ? super Minion> baseDeathRattle = baseDescr.tryGetDeathRattle();
        if (baseDeathRattle != null) {
            this.deathRattles.add(baseDeathRattle);
        }
    }

    private MinionProperties(
            Minion minion,
            MinionProperties baseProperties,
            CharacterAbilities<Minion> abilities) {

        ExceptionHelper.checkNotNullArgument(minion, "minion");
        ExceptionHelper.checkNotNullArgument(baseProperties, "baseProperties");
        ExceptionHelper.checkNotNullArgument(abilities, "abilities");

        this.minion = minion;
        this.attackTool = new MinionAttackTool(baseProperties.attackTool);
        this.body = baseProperties.body.copyFor(minion);
        this.abilities = abilities;
        this.deathRattles = new ArrayList<>(baseProperties.deathRattles);
        this.activated = false;
    }

    public PreparedResult<MinionProperties> copyFor(Minion other) {
        PreparedResult<CharacterAbilities<Minion>> newAbilities = abilities.copyFor(other);
        MinionProperties result = new MinionProperties(other, this, newAbilities.getResult());
        return new PreparedResult<>(result, () -> {
            UndoAction activateUndo = newAbilities.activate();
            boolean prevActivated = activated;
            result.activated = true;
            return () -> {
                result.activated = prevActivated;
                activateUndo.undo();
            };
        });
    }

    public UndoAction exhaust() {
        if (attackTool.exhausted) {
            return UndoAction.DO_NOTHING;
        }

        attackTool.exhausted = true;
        return () -> attackTool.exhausted = false;
    }

    public UndoAction activatePassiveAbilities() {
        UndoBuilder result = new UndoBuilder();
        if (activated) {
            return UndoAction.DO_NOTHING;
        }

        activated = true;
        result.addUndo(() -> activated = false);

        MinionDescr baseStats = body.getBaseStats();
        result.addUndo(addAndActivateAbility(baseStats.getEventActionDefs()));

        ActivatableAbility<? super Minion> ability = baseStats.tryGetAbility();
        if (ability != null) {
            result.addUndo(addAndActivateAbility(ability));
        }

        return result;
    }

    public UndoAction deactivateAllAbilities() {
        return abilities.deactivateAll();
    }

    public UndoAction refresh() {
        return attackTool.refresh();
    }

    public UndoAction refreshEndOfTurn() {
        return attackTool.refreshEndOfTurn();
    }

    public UndoAction updateAuras() {
        return body.applyAuras();
    }

    private World getWorld() {
        return minion.getWorld();
    }

    public UndoAction triggetDeathRattles() {
        return triggetDeathRattles(1);
    }

    public UndoAction triggetDeathRattles(int numberOfTriggers) {
        if (deathRattles.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder();
        World world = getWorld();
        for (WorldEventAction<? super Minion, ? super Minion> deathRattle: new ArrayList<>(deathRattles)) {
            for (int i = 0; i < numberOfTriggers; i++) {
                result.addUndo(deathRattle.alterWorld(world, minion, minion));
            }
        }
        return result;
    }

    private UndoAction removeDeathRattles() {
        if (deathRattles.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        List<WorldEventAction<? super Minion, ? super Minion>> prevDeathRattles = deathRattles;
        deathRattles = new ArrayList<>();
        return () -> deathRattles = prevDeathRattles;
    }

    @Override
    public UndoAction silence() {
        UndoBuilder result = new UndoBuilder();
        result.addUndo(abilities.silence());
        result.addUndo(attackTool.silence());
        result.addUndo(body.silence());
        result.addUndo(removeDeathRattles());
        return result;
    }

    public boolean isFrozen() {
        return attackTool.isFrozen();
    }

    public boolean isCharge() {
        return attackTool.charge.getValue();
    }

    public AttackTool getAttackTool() {
        return attackTool;
    }

    public AuraAwareIntProperty getBuffableAttack() {
        return attackTool.attack;
    }

    public MinionBody getBody() {
        return body;
    }

    public UndoAction addAndActivateAbility(ActivatableAbility<? super Minion> abilityRegisterTask) {
        if (minion.isDestroyed()) {
            return UndoAction.DO_NOTHING;
        }

        return abilities.getOwned().addAndActivateAbility(abilityRegisterTask);
    }

    public UndoAction setAttackFinalizer(OwnedIntPropertyBuff<? super Minion> newAttackFinalizer) {
        return attackTool.setAttackFinalizer(newAttackFinalizer);
    }

    public UndoAction addAttackBuff(int attack) {
        return attackTool.attack.addBuff(attack);
    }

    public UndoableUnregisterRef addRemovableAttackBuff(int attack) {
        return attackTool.attack.addRemovableBuff(attack);
    }

    public UndoAction setCharge(boolean newCharge) {
        return attackTool.setCharge(newCharge);
    }

    public AuraAwareIntProperty getMaxAttackCountProperty() {
        return attackTool.maxAttackCount;
    }

    public AuraAwareBoolProperty getChargeProperty() {
        return attackTool.charge;
    }

    public boolean isDeathRattle() {
        return !deathRattles.isEmpty();
    }

    public UndoAction addDeathRattle(WorldEventAction<? super Minion, ? super Minion> deathRattle) {
        ExceptionHelper.checkNotNullArgument(deathRattle, "deathRattle");

        deathRattles.add(deathRattle);
        return () -> {
            deathRattles.remove(deathRattles.size() - 1);
        };
    }

    private final class MinionAttackTool implements AttackTool {
        private final FreezeManager freezeManager;
        private final AuraAwareIntProperty attack;
        private boolean canAttack;
        private int attackCount;
        private final AuraAwareIntProperty maxAttackCount;
        private boolean exhausted;
        private final AuraAwareBoolProperty charge;
        private OwnedIntPropertyBuff<? super Minion> attackFinalizer;

        private boolean attackLeft;
        private boolean attackRight;

        public MinionAttackTool(MinionDescr baseDescr) {
            this.attack = new AuraAwareIntProperty(baseDescr.getAttack(), 0);
            this.freezeManager = new FreezeManager();
            this.maxAttackCount = new AuraAwareIntProperty(baseDescr.getMaxAttackCount());
            this.attackCount = 0;
            this.canAttack = baseDescr.isCanAttack();
            this.exhausted = true;
            this.charge = new AuraAwareBoolProperty(baseDescr.isCharge());
            this.attackLeft = baseDescr.isAttackLeft();
            this.attackRight = baseDescr.isAttackRight();
            this.attackFinalizer = baseDescr.getAttackFinalizer();
        }

        public MinionAttackTool(MinionAttackTool base) {
            this.attack = base.attack.copy();
            this.canAttack = base.canAttack;
            this.attackCount = base.attackCount;
            this.maxAttackCount = base.maxAttackCount.copy();
            this.freezeManager = base.freezeManager.copy();
            this.exhausted = base.exhausted;
            this.charge = base.charge.copy();
            this.attackLeft = base.attackLeft;
            this.attackRight = base.attackRight;
            this.attackFinalizer = base.attackFinalizer;
        }

        public UndoAction setAttackFinalizer(OwnedIntPropertyBuff<? super Minion> newAttackFinalizer) {
            ExceptionHelper.checkNotNullArgument(newAttackFinalizer, "newAttackFinalizer");
            OwnedIntPropertyBuff<? super Minion> prevAttackFinalizer = attackFinalizer;
            attackFinalizer = newAttackFinalizer;
            return () -> attackFinalizer = prevAttackFinalizer;
        }

        public UndoAction setCharge(boolean newCharge) {
            return charge.setValue(newCharge);
        }

        private UndoAction removeSwipeAttack() {
            if (!attackLeft && !attackRight) {
                return UndoAction.DO_NOTHING;
            }

            boolean prevAttackLeft = attackLeft;
            boolean prevAttackRight = attackRight;

            attackLeft = false;
            attackRight = false;

            return () -> {
                attackLeft = prevAttackLeft;
                attackRight = prevAttackRight;
            };
        }

        public UndoAction silence() {
            UndoBuilder result = new UndoBuilder();

            if (!canAttack) {
                canAttack = true;
                result.addUndo(() -> canAttack = false);
            }

            OwnedIntPropertyBuff<? super Minion> prevAttackFinalizer = attackFinalizer;
            if (prevAttackFinalizer != OwnedIntPropertyBuff.IDENTITY) {
                attackFinalizer = OwnedIntPropertyBuff.IDENTITY;
                result.addUndo(() -> attackFinalizer = prevAttackFinalizer);
            }

            result.addUndo(removeSwipeAttack());
            result.addUndo(maxAttackCount.silence());
            result.addUndo(freezeManager.silence());
            result.addUndo(attack.silence());
            result.addUndo(charge.silence());

            return result;
        }

        public UndoAction refresh() {
            boolean prevExhausted = exhausted;
            int prevAttackCount = attackCount;

            attackCount = 0;
            exhausted = false;

            return () -> {
                attackCount = prevAttackCount;
                exhausted = prevExhausted;
            };
        }

        public UndoAction refreshEndOfTurn() {
            return freezeManager.endTurn(attackCount);
        }

        @Override
        public UndoAction freeze() {
            return freezeManager.freeze();
        }

        @Override
        public int getAttack() {
            return attackFinalizer.buffProperty(minion, attack.getValue());
        }

        @Override
        public boolean canAttackWith() {
            if (!canAttack) {
                return false;
            }

            if (exhausted && !charge.getValue()) {
                return false;
            }

            return attackCount < maxAttackCount.getValue() && !isFrozen() && getAttack() > 0;
        }

        @Override
        public boolean canRetaliateWith() {
            return true;
        }

        @Override
        public boolean canTargetRetaliate() {
            return true;
        }

        @Override
        public boolean attacksLeft() {
            return attackLeft;
        }

        @Override
        public boolean attacksRight() {
            return attackRight;
        }

        @Override
        public UndoAction incUseCount() {
            attackCount++;
            return () -> attackCount--;
        }

        @Override
        public boolean isFrozen() {
            return freezeManager.isFrozen();
        }
    }
}
