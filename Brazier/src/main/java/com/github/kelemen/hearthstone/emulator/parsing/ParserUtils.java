package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.HearthStoneDb;
import com.github.kelemen.hearthstone.emulator.HeroPowerId;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.abilities.Aura;
import com.github.kelemen.hearthstone.emulator.abilities.AuraFilter;
import com.github.kelemen.hearthstone.emulator.abilities.LivingEntitysAbilities;
import com.github.kelemen.hearthstone.emulator.actions.ActorlessTargetedAction;
import com.github.kelemen.hearthstone.emulator.actions.BattleCryTargetedAction;
import com.github.kelemen.hearthstone.emulator.actions.CardPlayAction;
import com.github.kelemen.hearthstone.emulator.actions.CharacterTargetedAction;
import com.github.kelemen.hearthstone.emulator.actions.DamageAction;
import com.github.kelemen.hearthstone.emulator.actions.MinionAction;
import com.github.kelemen.hearthstone.emulator.actions.PlayActionRequirement;
import com.github.kelemen.hearthstone.emulator.actions.PlayerAction;
import com.github.kelemen.hearthstone.emulator.actions.TargetNeed;
import com.github.kelemen.hearthstone.emulator.actions.TargetedMinionAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WeaponAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventActionDefs;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventFilter;
import com.github.kelemen.hearthstone.emulator.actions.WorldObjectAction;
import com.github.kelemen.hearthstone.emulator.actions2.EntityFilter;
import com.github.kelemen.hearthstone.emulator.actions2.EntitySelector;
import com.github.kelemen.hearthstone.emulator.actions2.TargetedAction;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.cards.CardProvider;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import com.github.kelemen.hearthstone.emulator.minions.MinionProvider;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponDescr;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponId;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

public final class ParserUtils {
    private static final String[] DEFAULT_PACKAGES = {
        "com.github.kelemen.hearthstone.emulator.actions",
        "com.github.kelemen.hearthstone.emulator.abilities",
    };

    public static JsonDeserializer createDefaultDeserializer(Supplier<HearthStoneDb> dbRef) {
        ExceptionHelper.checkNotNullArgument(dbRef, "dbRef");

        JsonDeserializer.Builder result = new JsonDeserializer.Builder(ParserUtils::resolveClassName);

        addCustomStringParsers(dbRef, result);
        addTypeConversions(result);
        addTypeMergers(result);

        return result.create();
    }

    private static void addTypeMergers(JsonDeserializer.Builder result) {
        result.setTypeMerger(PlayActionRequirement.class, (elements) -> PlayActionRequirement.merge(elements));
        result.setTypeMerger(ActorlessTargetedAction.class, (elements) -> ActorlessTargetedAction.mergeActions(elements));
        result.setTypeMerger(CardPlayAction.class, (elements) -> CardPlayAction.mergeActions(elements));
        result.setTypeMerger(WorldEventFilter.class, (Collection<? extends WorldEventFilter<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends WorldEventFilter<Object, Object>> unsafeElements
                    = (Collection<? extends WorldEventFilter<Object, Object>>)elements;
            return WorldEventFilter.merge(unsafeElements);
        });
        result.setTypeMerger(WorldEventAction.class, (Collection<? extends WorldEventAction<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends WorldEventAction<PlayerProperty, Object>> unsafeElements
                    = (Collection<? extends WorldEventAction<PlayerProperty, Object>>)elements;
            return WorldEventAction.merge(unsafeElements);
        });

        result.setTypeMerger(TargetNeed.class, (Collection<? extends TargetNeed> elements) -> {
            // Unsafe but there is nothing to do.
            TargetNeed mergedNeed = TargetNeed.NO_NEED;
            for (TargetNeed need: elements) {
                mergedNeed = mergedNeed.combine(need);
            }
            return mergedNeed;
        });
        result.setTypeMerger(ActivatableAbility.class, (Collection<? extends ActivatableAbility<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends ActivatableAbility<Object>> unsafeElements
                    = (Collection<? extends ActivatableAbility<Object>>)elements;
            return ActivatableAbility.merge(unsafeElements);
        });
        result.setTypeMerger(Aura.class, (Collection<? extends Aura<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<Aura<Object, Object>> unsafeElements
                    = (Collection<Aura<Object, Object>>)elements;
            return Aura.merge(unsafeElements);
        });
        result.setTypeMerger(AuraFilter.class, (Collection<? extends AuraFilter<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends AuraFilter<Object, Object>> unsafeElements
                    = (Collection<? extends AuraFilter<Object, Object>>)elements;
            return AuraFilter.merge(unsafeElements);
        });
        result.setTypeMerger(WorldObjectAction.class, (Collection<? extends WorldObjectAction<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends WorldObjectAction<Object>> unsafeElements
                    = (Collection<? extends WorldObjectAction<Object>>)elements;
            return WorldObjectAction.merge(unsafeElements);
        });
        result.setTypeMerger(MinionAction.class, (Collection<? extends MinionAction> elements) -> {
            return MinionAction.merge(elements);
        });
        result.setTypeMerger(WeaponAction.class, (Collection<? extends WeaponAction> elements) -> {
            return WeaponAction.merge(elements);
        });
        result.setTypeMerger(DamageAction.class, (Collection<? extends DamageAction> elements) -> {
            return DamageAction.merge(elements);
        });
        result.setTypeMerger(BattleCryTargetedAction.class, (Collection<? extends BattleCryTargetedAction> elements) -> {
            return BattleCryTargetedAction.merge(elements);
        });
        result.setTypeMerger(PlayerAction.class, (Collection<? extends PlayerAction> elements) -> {
            return PlayerAction.merge(elements);
        });
        result.setTypeMerger(TargetedMinionAction.class, (Collection<? extends TargetedMinionAction> elements) -> {
            return TargetedMinionAction.merge(elements);
        });
        result.setTypeMerger(TargetedAction.class, (Collection<? extends TargetedAction<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends TargetedAction<Object, Object>> unsafeElements
                    = (Collection<? extends TargetedAction<Object, Object>>)elements;
            return TargetedAction.merge(unsafeElements);
        });
        result.setTypeMerger(EntityFilter.class, (Collection<? extends EntityFilter<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends EntityFilter<Object>> unsafeElements
                    = (Collection<? extends EntityFilter<Object>>)elements;
            return EntityFilter.merge(unsafeElements);
        });
        result.setTypeMerger(EntitySelector.class, (Collection<? extends EntitySelector<?, ?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends EntitySelector<Object, Object, Object>> unsafeElements
                    = (Collection<? extends EntitySelector<Object, Object, Object>>)elements;
            return EntitySelector.merge(unsafeElements);
        });
    }

    private static void addTypeConversions(JsonDeserializer.Builder result) {
        result.addTypeConversion(WorldAction.class, PlayerAction.class,
                (action) -> (world, playerId) -> action.alterWorld(world));

        result.addTypeConversion(WorldAction.class, TargetedMinionAction.class,
                (action) -> (targeter, target) -> action.alterWorld(targeter.getWorld()));
        result.addTypeConversion(PlayerAction.class, TargetedMinionAction.class,
                (action) -> action.toTargetedAction().toTargetedMinionAction());
        result.addTypeConversion(CharacterTargetedAction.class, TargetedMinionAction.class,
                (action) -> action.toTargetedAction().toTargetedMinionAction());
        result.addTypeConversion(ActorlessTargetedAction.class, TargetedMinionAction.class,
                (action) -> action.toTargetedMinionAction());
        result.addTypeConversion(MinionAction.class, TargetedMinionAction.class,
                (action) -> action.toCharacterTargetedAction().toTargetedAction().toTargetedMinionAction());
        result.addTypeConversion(DamageAction.class, TargetedMinionAction.class,
                (action) -> action.toCharacterTargetedAction().toTargetedAction().toTargetedMinionAction());

        result.addTypeConversion(WorldAction.class, ActorlessTargetedAction.class,
                (action) -> (world, target) -> action.alterWorld(world));
        result.addTypeConversion(PlayerAction.class, ActorlessTargetedAction.class,
                (action) -> action.toTargetedAction());
        result.addTypeConversion(CharacterTargetedAction.class, ActorlessTargetedAction.class,
                (action) -> action.toTargetedAction());
        result.addTypeConversion(MinionAction.class, ActorlessTargetedAction.class,
                (action) -> action.toCharacterTargetedAction().toTargetedAction());
        result.addTypeConversion(DamageAction.class, ActorlessTargetedAction.class,
                (action) -> action.toCharacterTargetedAction().toTargetedAction());

        result.addTypeConversion(WorldAction.class, CardPlayAction.class,
                (action) -> (world, target) -> action.alterWorld(world));
        result.addTypeConversion(PlayerAction.class, CardPlayAction.class,
                (action) -> action.toTargetedAction().toCardPlayAction());
        result.addTypeConversion(CharacterTargetedAction.class, CardPlayAction.class,
                (action) -> action.toTargetedAction().toCardPlayAction());
        result.addTypeConversion(ActorlessTargetedAction.class, CardPlayAction.class,
                (action) -> action.toCardPlayAction());
        result.addTypeConversion(MinionAction.class, CardPlayAction.class,
                (action) -> action.toCharacterTargetedAction().toTargetedAction().toCardPlayAction());
        result.addTypeConversion(DamageAction.class, CardPlayAction.class,
                (action) -> action.toCharacterTargetedAction().toTargetedAction().toCardPlayAction());

        result.addTypeConversion(WorldAction.class, BattleCryTargetedAction.class,
                (action) -> (world, target) -> action.alterWorld(world));
        result.addTypeConversion(PlayerAction.class, BattleCryTargetedAction.class,
                (action) -> action.toBattleCryTargetedAction());
        result.addTypeConversion(CharacterTargetedAction.class, BattleCryTargetedAction.class,
                (action) -> action.toBattleCryTargetedAction());
        result.addTypeConversion(ActorlessTargetedAction.class, BattleCryTargetedAction.class,
                (action) -> action.toBattleCryTargetedAction());
        result.addTypeConversion(TargetedMinionAction.class, BattleCryTargetedAction.class,
                (action) -> action.toBattleCryTargetedAction());
        result.addTypeConversion(MinionAction.class, BattleCryTargetedAction.class,
                (action) -> action.toBattleCryTargetedAction());
        result.addTypeConversion(DamageAction.class, BattleCryTargetedAction.class,
                (action) -> action.toBattleCryTargetedAction());

        result.addTypeConversion(WorldAction.class, WorldEventAction.class,
                (action) -> (world, self, eventSource) -> action.alterWorld(world));
        result.addTypeConversion(PlayerAction.class, WorldEventAction.class,
                (action) -> (world, self, eventSource) -> action.alterWorld(world, self.getOwner()));

        result.addTypeConversion(DamageAction.class, MinionAction.class,
                (action) -> action.toMinionAction());

        result.addTypeConversion(DamageAction.class, WeaponAction.class,
                (action) -> action.toWeaponAction());
    }

    private static void addCustomStringParsers(Supplier<HearthStoneDb> dbRef, JsonDeserializer.Builder result) {
        result.setCustomStringParser(CardProvider.class, (str) -> toCardProvider(dbRef, str));
        result.setCustomStringParser(MinionProvider.class, (str) -> toMinionProvider(dbRef, str));
        result.setCustomStringParser(WeaponProvider.class, (str) -> toWeaponProvider(dbRef, str));
        result.setCustomStringParser(TargetNeed.class, ParserUtils::toTargetNeed);
        result.setCustomStringParser(Keyword.class, (str) -> Keyword.create(str));
        result.setCustomStringParser(CardId.class, (str) -> new CardId(str));
        result.setCustomStringParser(MinionId.class, (str) -> new MinionId(str));
        result.setCustomStringParser(WeaponId.class, (str) -> new WeaponId(str));
        result.setCustomStringParser(HeroPowerId.class, (str) -> new HeroPowerId(str));
    }

    private static TargetNeed toTargetNeed(String str) throws ObjectParsingException {
        String normNeedStr = str.toLowerCase(Locale.ROOT);
        switch (normNeedStr) {
            case "all-heroes":
                return TargetNeed.ALL_HEROES;
            case "all-minions":
                return TargetNeed.ALL_MINIONS;
            case "self-minions":
                return TargetNeed.SELF_MINIONS;
            case "enemy-minions":
                return TargetNeed.ENEMY_MINIONS;
            case "all":
                return TargetNeed.ALL_TARGETS;
            case "self":
                return TargetNeed.SELF_TARGETS;
            case "enemy":
                return TargetNeed.ENEMY_TARGETS;
            default:
                return null;
        }
    }

    private static CardProvider toCardProvider(Supplier<HearthStoneDb> dbRef, String cardId) {
        AtomicReference<CardDescr> cache = new AtomicReference<>(null);
        return () -> {
            CardDescr result = cache.get();
            if (result == null) {
                HearthStoneDb db = Objects.requireNonNull(dbRef.get(), "HearthStoneDb");
                result = db.getCardDb().getById(new CardId(cardId));
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        };
    }

    private static MinionProvider toMinionProvider(Supplier<HearthStoneDb> dbRef, String minionId) {
        AtomicReference<MinionDescr> cache = new AtomicReference<>(null);
        return () -> {
            MinionDescr result = cache.get();
            if (result == null) {
                HearthStoneDb db = Objects.requireNonNull(dbRef.get(), "HearthStoneDb");
                result = db.getMinionDb().getById(new MinionId(minionId));
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        };
    }

    private static WeaponProvider toWeaponProvider(Supplier<HearthStoneDb> dbRef, String weaponId) {
        AtomicReference<WeaponDescr> cache = new AtomicReference<>(null);
        return () -> {
            WeaponDescr result = cache.get();
            if (result == null) {
                HearthStoneDb db = Objects.requireNonNull(dbRef.get(), "HearthStoneDb");
                result = db.getWeaponDb().getById(new WeaponId(weaponId));
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        };
    }

    private static Class<?> resolveClassName(String unqualifiedClassName) throws ObjectParsingException {
        for (String packageName: DEFAULT_PACKAGES) {
            try {
                return Class.forName(packageName + '.' + unqualifiedClassName);
            } catch (ClassNotFoundException ex) {
                // Ignore and try another class.
            }
        }
        throw new ObjectParsingException("Cannot resolve class name: " + unqualifiedClassName);
    }

    public static JsonObject fromJsonFile(Path file) throws IOException {
        JsonParser jsonParser = new JsonParser();
        try (Reader inputReader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
            JsonElement result = jsonParser.parse(inputReader);
            if (result.isJsonObject()) {
                return result.getAsJsonObject();
            }
            else {
                throw new IOException("The JsonElement is not an object in " + file);
            }
        }
    }

    public static void parseKeywords(JsonTree keywords, Consumer<? super Keyword> keywordAdder) {
        ExceptionHelper.checkNotNullArgument(keywords, "keywords");
        ExceptionHelper.checkNotNullArgument(keywordAdder, "keywordAdder");

        if (keywords.isJsonPrimitive()) {
            keywordAdder.accept(Keyword.create(keywords.getAsString()));
        }
        else if (keywords.isJsonArray()) {
            for (JsonTree keywordElement: keywords.getChildren()) {
                keywordAdder.accept(Keyword.create(keywordElement.getAsString()));
            }
        }
    }

    public static String getStringField(JsonTree obj, String fieldName) throws ObjectParsingException {
        String result = tryGetStringField(obj, fieldName);
        if (result == null) {
            throw new ObjectParsingException("Missing required field: " + fieldName);
        }
        return result;
    }

    public static String tryGetStringField(JsonTree obj, String fieldName) {
        ExceptionHelper.checkNotNullArgument(obj, "obj");
        ExceptionHelper.checkNotNullArgument(fieldName, "fieldName");

        JsonTree fieldValue = obj.getChild(fieldName);
        return fieldValue != null ? fieldValue.getAsString() : null;
    }

    public static int getIntField(JsonTree obj, String fieldName) throws ObjectParsingException {
        JsonTree fieldValue = obj.getChild(fieldName);
        if (fieldValue == null) {
            throw new ObjectParsingException("Missing required field: " + fieldName);
        }

        return fieldValue.getAsInt();
    }

    public static TargetNeed getTargetNeedOfAction(
            JsonDeserializer objectParser,
            JsonTree actionElement) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");
        ExceptionHelper.checkNotNullArgument(actionElement, "actionElement");

        JsonTree needsElement = actionElement.getChild("targets");
        if (needsElement != null) {
            return objectParser.toJavaObject(needsElement, TargetNeed.class);
        }

        return TargetNeed.NO_NEED;
    }

    public static PlayActionRequirement getPlayRequirement(
            JsonDeserializer objectParser,
            JsonTree requiresElement) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");
        ExceptionHelper.checkNotNullArgument(requiresElement, "requiresElement");

        return objectParser.toJavaObject(requiresElement, PlayActionRequirement.class);
    }

    public static PlayActionRequirement getPlayRequirementOfAction(
            JsonDeserializer objectParser,
            JsonTree actionElement) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");
        ExceptionHelper.checkNotNullArgument(actionElement, "actionElement");

        JsonTree requiresElement = actionElement.getChild("requires");
        return requiresElement != null
                ?getPlayRequirement(objectParser, requiresElement)
                : PlayActionRequirement.ALLOWED;
    }

    public static PlayActionRequirement getActionConditionOfAction(
            JsonDeserializer objectParser,
            JsonTree actionElement) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");
        ExceptionHelper.checkNotNullArgument(actionElement, "actionElement");

        JsonTree requiresElement = actionElement.getChild("actionCondition");
        return requiresElement != null
                ?getPlayRequirement(objectParser, requiresElement)
                : PlayActionRequirement.ALLOWED;
    }

    private static <Self extends PlayerProperty> WorldEventActionDefs<Self> parseEventActionDefs(
            EventNotificationParser<Self> eventNotificationParser,
            JsonTree triggersElement) throws ObjectParsingException {
        if (triggersElement == null) {
            return new WorldEventActionDefs.Builder<Self>().create();
        }

        return eventNotificationParser.fromJson(triggersElement);
    }

    private static <Self> ActivatableAbility<? super Self> parseAbility(
            Class<Self> selfClass,
            JsonDeserializer objectParser,
            JsonTree abilityElement) throws ObjectParsingException {
        if (abilityElement == null) {
            return null;
        }

        // Unsafe but there is nothing we can do about it.
        @SuppressWarnings("unchecked")
        ActivatableAbility<? super Self> ability = (ActivatableAbility<? super Self>)objectParser.toJavaObject(
                abilityElement,
                ActivatableAbility.class,
                TypeCheckers.genericTypeChecker(ActivatableAbility.class, selfClass));
        return ability;
    }

    private static <Self extends PlayerProperty> WorldEventAction<? super Self, ? super Self> parseDeathRattle(
            Class<Self> selfClass,
            EventNotificationParser<Self> eventNotificationParser,
            JsonTree root) throws ObjectParsingException {

        JsonTree deathRattleElement = root.getChild("deathRattle");
        if (deathRattleElement == null) {
            return null;
        }

        JsonTree deathRattleConditionElement = root.getChild("deathRattleCondition");
        WorldEventFilter<? super Self, ? super Self> deathRattleFilter = deathRattleConditionElement != null
                ? eventNotificationParser.parseFilter(selfClass, deathRattleConditionElement)
                : null;

        WorldEventAction<? super Self, ? super Self> action
                = eventNotificationParser.parseAction(selfClass, deathRattleElement);

        if (deathRattleFilter != null) {
            return (World world, Self self, Self eventSource) -> {
                if (!deathRattleFilter.applies(world, self, eventSource)) {
                    return UndoAction.DO_NOTHING;
                }
                return action.alterWorld(world, self, eventSource);
            };
        }
        else {
            return action;
        }
    }

    public static <Self extends PlayerProperty> LivingEntitysAbilities<Self> parseAbilities(
            Class<Self> selfClass,
            JsonDeserializer objectParser,
            EventNotificationParser<Self> eventNotificationParser,
            JsonTree root) throws ObjectParsingException {

        ActivatableAbility<? super Self> ability = parseAbility(selfClass, objectParser, root.getChild("ability"));
        WorldEventActionDefs<Self> eventActionDefs = parseEventActionDefs(eventNotificationParser, root.getChild("triggers"));
        WorldEventAction<? super Self, ? super Self> deathRattle = parseDeathRattle(selfClass, eventNotificationParser, root);

        return new LivingEntitysAbilities<>(ability, eventActionDefs, deathRattle);
    }

    private ParserUtils() {
        throw new AssertionError();
    }
}
