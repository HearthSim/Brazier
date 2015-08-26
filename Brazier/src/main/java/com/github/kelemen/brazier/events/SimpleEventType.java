package com.github.kelemen.brazier.events;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.Secret;
import com.github.kelemen.brazier.actions.AttackRequest;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.weapons.Weapon;

public enum SimpleEventType {
    DRAW_CARD("draw-card", Card.class),
    START_PLAY_CARD("start-play-card", CardPlayEvent.class),
    DONE_PLAY_CARD("done-play-card", CardPlayedEvent.class),
    PREPARE_DAMAGE("prepare-damage", DamageRequest.class),
    HERO_DAMAGED("hero-damaged", DamageEvent.class),
    MINION_DAMAGED("minion-damaged", DamageEvent.class),
    MINION_KILLED("minion-killed", Minion.class),
    WEAPON_DESTROYED("weapon-destroyed", Weapon.class),
    ARMOR_GAINED("armor-gained", ArmorGainedEvent.class),
    HERO_HEALED("hero-healed", DamageEvent.class),
    MINION_HEALED("minion-healed", DamageEvent.class),
    TURN_STARTS("turn-starts", Player.class),
    TURN_ENDS("turn-ends", Player.class),
    ATTACK_INITIATED("attack-initiated", AttackRequest.class),
    SECRET_REVEALED("secret-revealed", Secret.class);

    private final String eventName;
    private final Class<?> argType;

    private SimpleEventType(String eventName, Class<?> argType) {
        this.eventName = eventName;
        this.argType = argType;
    }

    public String getEventName() {
        return eventName;
    }

    public Class<?> getArgumentType() {
        return argType;
    }
}
