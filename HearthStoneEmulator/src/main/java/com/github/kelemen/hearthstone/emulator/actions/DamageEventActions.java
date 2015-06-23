package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Damage;
import com.github.kelemen.hearthstone.emulator.DamageEvent;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;

public final class DamageEventActions {
    public static final WorldEventAction<PlayerProperty, DamageEvent> KILL_DAMAGE_TARGET_MINION = (world, self, eventSource) -> {
        return eventSource.getTarget().poison();
    };

    public static final WorldEventAction<PlayerProperty, DamageEvent> FREEZE_DAMAGE_TARGET = (world, self, eventSource) -> {
        TargetableCharacter target = eventSource.getTarget();
        return target.getAttackTool().freeze();
    };

    public static final WorldEventAction<PlayerProperty, DamageEvent> REFLECT_DAMAGE_TO_ENEMY_HERO = (world, self, eventSource) -> {
        Player player = self.getOwner();
        Player opponent = player.getOpponent();
        Damage damage = player.getSpellDamage(eventSource.getDamageDealt());
        return opponent.getHero().damage(damage);
    };

    private DamageEventActions() {
        throw new AssertionError();
    }
}
