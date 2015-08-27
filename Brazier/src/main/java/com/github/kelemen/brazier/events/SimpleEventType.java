package com.github.kelemen.brazier.events;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.Secret;
import com.github.kelemen.brazier.actions.AttackRequest;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.weapons.Weapon;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jtrim.utils.ExceptionHelper;

public enum SimpleEventType {
    DRAW_CARD("draw-card", Card.class),
    START_PLAY_CARD("start-play-card", CardPlayEvent.class, (world, self, arg) -> {
        return self != ((CardPlayEvent)arg).getCard().getMinion();
    }),
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
    ATTACK_INITIATED(true, "attack-initiated", AttackRequest.class),
    SECRET_REVEALED("secret-revealed", Secret.class);

    private static final Map<String, SimpleEventType> NAME_MAP;

    static {
        Map<String, SimpleEventType> nameMap = new HashMap<>();
        for (SimpleEventType eventType: values()) {
            nameMap.put(eventType.eventName, eventType);
        }
        NAME_MAP = Collections.unmodifiableMap(nameMap);
    }

    private final boolean greedyEvent;
    private final String eventName;
    private final Class<?> argType;
    private final WorldEventFilter<Object, Object> globalFilter;

    private SimpleEventType(String eventName, Class<?> argType) {
        this(false, eventName, argType);
    }

    private SimpleEventType(String eventName, Class<?> argType, WorldEventFilter<Object, Object> globalFilter) {
        this(false, eventName, argType, globalFilter);
    }

    private SimpleEventType(boolean greedyEvent, String eventName, Class<?> argType) {
        this(greedyEvent, eventName, argType, null);
    }

    private SimpleEventType(
            boolean greedyEvent,
            String eventName,
            Class<?> argType,
            WorldEventFilter<Object, Object> globalFilter) {
        this.greedyEvent = greedyEvent;
        this.eventName = eventName;
        this.argType = argType;
        this.globalFilter = globalFilter;
    }

    public WorldEventFilter<Object, Object> getGlobalFilter() {
        return globalFilter;
    }

    public <Self, Arg> WorldEventFilter<Self, Arg> addGlobalFilter(
            WorldEventFilter<Self, Arg> localFilter) {
        ExceptionHelper.checkNotNullArgument(localFilter, "localFilter");
        if (globalFilter == null) {
            return localFilter;
        }

        return (world, self, arg) -> {
            return localFilter.applies(world, self, arg) && globalFilter.applies(world, arg, eventName);
        };
    }

    public static SimpleEventType tryParse(String str) {
        return NAME_MAP.get(str.toLowerCase(Locale.ROOT));
    }

    public boolean isGreedyEvent() {
        return greedyEvent;
    }

    public String getEventName() {
        return eventName;
    }

    public Class<?> getArgumentType() {
        return argType;
    }
}
