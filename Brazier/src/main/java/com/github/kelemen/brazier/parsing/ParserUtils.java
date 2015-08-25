package com.github.kelemen.brazier.parsing;

import com.github.kelemen.brazier.HearthStoneDb;
import com.github.kelemen.brazier.Hero;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.abilities.Aura;
import com.github.kelemen.brazier.abilities.AuraFilter;
import com.github.kelemen.brazier.abilities.Buff;
import com.github.kelemen.brazier.abilities.Buffs;
import com.github.kelemen.brazier.abilities.LivingEntitiesAbilities;
import com.github.kelemen.brazier.abilities.PermanentBuff;
import com.github.kelemen.brazier.actions.EntityFilter;
import com.github.kelemen.brazier.actions.EntityFilters;
import com.github.kelemen.brazier.actions.EntitySelector;
import com.github.kelemen.brazier.actions.PlayActionDef;
import com.github.kelemen.brazier.actions.PlayActionRequirement;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.actions.TargetedAction;
import com.github.kelemen.brazier.actions.TargetedActionCondition;
import com.github.kelemen.brazier.actions.TargetedEntitySelector;
import com.github.kelemen.brazier.actions.TargetlessAction;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.WorldAction;
import com.github.kelemen.brazier.actions.WorldObjectAction;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.cards.CardId;
import com.github.kelemen.brazier.cards.CardProvider;
import com.github.kelemen.brazier.cards.PlayAction;
import com.github.kelemen.brazier.events.WorldEventAction;
import com.github.kelemen.brazier.events.WorldEventActionDefs;
import com.github.kelemen.brazier.events.WorldEventFilter;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.minions.MinionDescr;
import com.github.kelemen.brazier.minions.MinionId;
import com.github.kelemen.brazier.minions.MinionProvider;
import com.github.kelemen.brazier.weapons.WeaponDescr;
import com.github.kelemen.brazier.weapons.WeaponId;
import com.github.kelemen.brazier.weapons.WeaponProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

public final class ParserUtils {
    private static final String[] DEFAULT_PACKAGES = {
        "com.github.kelemen.brazier.actions",
        "com.github.kelemen.brazier.abilities",
        "com.github.kelemen.brazier.events",
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
        result.setTypeMerger(TargetedAction.class, (Collection<? extends TargetedAction<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends TargetedAction<Object, Object>> unsafeElements
                    = (Collection<? extends TargetedAction<Object, Object>>)elements;
            return TargetedAction.merge(unsafeElements);
        });
        result.setTypeMerger(TargetlessAction.class, (Collection<? extends TargetlessAction<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends TargetlessAction<Object>> unsafeElements
                    = (Collection<? extends TargetlessAction<Object>>)elements;
            return TargetlessAction.merge(unsafeElements);
        });
        result.setTypeMerger(EntityFilter.class, (Collection<? extends EntityFilter<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends EntityFilter<Object>> unsafeElements
                    = (Collection<? extends EntityFilter<Object>>)elements;
            return EntityFilter.merge(unsafeElements);
        });
        result.setTypeMerger(TargetedActionCondition.class, (Collection<? extends TargetedActionCondition<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends TargetedActionCondition<Object, Object>> unsafeElements
                    = (Collection<? extends TargetedActionCondition<Object, Object>>)elements;
            return TargetedActionCondition.merge(unsafeElements);
        });
        result.setTypeMerger(Predicate.class, (Collection<? extends Predicate<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends Predicate<Object>> unsafeElements
                    = (Collection<? extends Predicate<Object>>)elements;
            return mergePredicates(unsafeElements);
        });
        result.setTypeMerger(TargetedEntitySelector.class, (Collection<? extends TargetedEntitySelector<?, ?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends TargetedEntitySelector<Object, Object, Object>> unsafeElements
                    = (Collection<? extends TargetedEntitySelector<Object, Object, Object>>)elements;
            return TargetedEntitySelector.merge(unsafeElements);
        });
        result.setTypeMerger(EntitySelector.class, (Collection<? extends EntitySelector<?, ?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends EntitySelector<Object, Object>> unsafeElements
                    = (Collection<? extends EntitySelector<Object, Object>>)elements;
            return EntitySelector.merge(unsafeElements);
        });
        result.setTypeMerger(Buff.class, (Collection<? extends Buff<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends Buff<Object>> unsafeElements
                    = (Collection<? extends Buff<Object>>)elements;
            return Buff.merge(unsafeElements);
        });
        result.setTypeMerger(PermanentBuff.class, (Collection<? extends PermanentBuff<?>> elements) -> {
            // Unsafe but there is nothing to do.
            @SuppressWarnings("unchecked")
            Collection<? extends PermanentBuff<Object>> unsafeElements
                    = (Collection<? extends PermanentBuff<Object>>)elements;
            return PermanentBuff.merge(unsafeElements);
        });
    }

    private static void addTypeConversions(JsonDeserializer.Builder result) {
        result.addTypeConversion(WorldAction.class, WorldEventAction.class,
                (action) -> (world, self, eventSource) -> action.alterWorld(world));
        result.addTypeConversion(TargetlessAction.class, WorldEventAction.class, (action) -> {
            // Not truly safe but there is not much to do.
            // The true requirement is that the "Actor" extends the "Self" object of
            // WorldEventAction.
            @SuppressWarnings("unchecked")
            TargetlessAction<PlayerProperty> safeAction = (TargetlessAction<PlayerProperty>)action;
            return (World world, PlayerProperty self, Object eventSource) -> {
                return safeAction.alterWorld(world, self);
            };
        });

        result.addTypeConversion(TargetedAction.class, TargetlessAction.class, (action) -> {
            // Not truly safe but there is not much to do.
            // The true requirement is that the "Actor" extends the "Self" object of
            // WorldEventAction.
            @SuppressWarnings("unchecked")
            TargetedAction<Object, Object> safeAction = (TargetedAction<Object, Object>)action;
            return (World world, Object actor) -> {
                return safeAction.alterWorld(world, actor, actor);
            };
        });

        result.addTypeConversion(Predicate.class, EntityFilter.class, (predicate) -> {
            // Not truly safe but there is not much to do.
            @SuppressWarnings("unchecked")
            Predicate<Object> safePredicate = (Predicate<Object>)predicate;
            return EntityFilters.fromPredicate(safePredicate);
        });

        result.addTypeConversion(TargetlessAction.class, TargetedAction.class,
                (action) -> action.toTargetedAction());

        result.addTypeConversion(EntitySelector.class, TargetedEntitySelector.class,
                (action) -> action.toTargeted());

        result.addTypeConversion(Buff.class, PermanentBuff.class,
                (action) -> action.toPermanent());
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

        result.setCustomStringParser(Buff.class, (String str) -> {
            BuffDescr buffDescr = BuffDescr.tryCreate(str);
            return buffDescr != null ? Buffs.buffRemovable(buffDescr.attack, buffDescr.hp) : null;
        });
        result.setCustomStringParser(PermanentBuff.class, (String str) -> {
            BuffDescr buffDescr = BuffDescr.tryCreate(str);
            return buffDescr != null ? Buffs.buff(buffDescr.attack, buffDescr.hp) : null;
        });
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

    public static Set<Keyword> parseKeywords(JsonTree keywordsElement) {
        Set<Keyword> keywords = new HashSet<>();
        ParserUtils.parseKeywords(keywordsElement, keywords::add);
        return keywords;
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

    public static <Self extends PlayerProperty> LivingEntitiesAbilities<Self> parseAbilities(
            Class<Self> selfClass,
            JsonDeserializer objectParser,
            EventNotificationParser<Self> eventNotificationParser,
            JsonTree root) throws ObjectParsingException {

        ActivatableAbility<? super Self> ability = parseAbility(selfClass, objectParser, root.getChild("ability"));
        WorldEventActionDefs<Self> eventActionDefs = parseEventActionDefs(eventNotificationParser, root.getChild("triggers"));
        WorldEventAction<? super Self, ? super Self> deathRattle = parseDeathRattle(selfClass, eventNotificationParser, root);

        return new LivingEntitiesAbilities<>(ability, eventActionDefs, deathRattle);
    }

    public static <T> Predicate<T> mergePredicates(
            Collection<? extends Predicate<T>> filters) {
        List<Predicate<T>> filtersCopy = new ArrayList<>(filters);
        ExceptionHelper.checkNotNullElements(filtersCopy, "filters");

        int count = filtersCopy.size();
        if (count == 0) {
            return (arg) -> true;
        }
        if (count == 1) {
            return filtersCopy.get(0);
        }

        return (T arg) -> {
            for (Predicate<T> filter: filtersCopy) {
                if (!filter.test(arg)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static <Actor, Target> PlayAction<Actor> parseTargetedAction(
            JsonDeserializer objectParser,
            JsonTree actionElement,
            Class<Actor> actorType,
            Class<Target> targetType) throws ObjectParsingException {
        @SuppressWarnings("unchecked")
        TargetedAction<? super Actor, ? super Target> result = objectParser.toJavaObject(
                actionElement,
                TargetedAction.class,
                TypeCheckers.genericTypeChecker(TargetedAction.class, actorType, targetType));
        return (World world, Actor actor, Optional<TargetableCharacter> optTarget) -> {
            if (!optTarget.isPresent()) {
                return UndoAction.DO_NOTHING;
            }

            TargetableCharacter target = optTarget.get();
            if (targetType.isInstance(target)) {
                return result.alterWorld(world, actor, targetType.cast(target));
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private static <Actor> PlayAction<Actor> parseTargetedActionRaw(
            JsonDeserializer objectParser,
            JsonTree actionElement,
            TargetNeed targetNeed,
            Class<Actor> actorType) throws ObjectParsingException {

        // FIXME: Remove these unreliable tests after no longer needed
        if (!targetNeed.mayTargetHero()) {
            if (!targetNeed.mayTargetMinion()) {
                @SuppressWarnings("unchecked")
                TargetlessAction<Actor> result = objectParser.toJavaObject(
                        actionElement,
                        TargetlessAction.class,
                        TypeCheckers.genericTypeChecker(TargetlessAction.class, actorType));
                return (world, actor, target) -> result.alterWorld(world, actor);
            }
            else {
                return parseTargetedAction(objectParser, actionElement, actorType, Minion.class);
            }
        }
        else {
            if (!targetNeed.mayTargetMinion()) {
                return parseTargetedAction(objectParser, actionElement, actorType, Hero.class);
            }
            else {
                return parseTargetedAction(objectParser, actionElement, actorType, TargetableCharacter.class);
            }
        }
    }

    public static <Actor> PlayAction<Actor> parseTargetedAction(
            JsonDeserializer objectParser,
            JsonTree actionElement,
            TargetNeed targetNeed,
            Class<Actor> actorType) throws ObjectParsingException {

        if (actionElement.isJsonObject() && actionElement.getChild("class") == null) {
            JsonTree actionsDefElement = actionElement.getChild("actions");
            if (actionsDefElement == null) {
                throw new ObjectParsingException("Missing action definition for CardPlayAction.");
            }
            return parseTargetedActionRaw(objectParser, actionsDefElement, targetNeed, actorType);
        }

        return parseTargetedActionRaw(objectParser, actionElement, targetNeed, actorType);
    }

    public static <Actor extends PlayerProperty> PlayActionDef<Actor> parseSinglePlayActionDef(
            JsonDeserializer objectParser,
            JsonTree battleCryElement,
            Class<Actor> actorType) throws ObjectParsingException {

        TargetNeed targetNeed = ParserUtils.getTargetNeedOfAction(objectParser, battleCryElement);
        PlayActionRequirement requirement = ParserUtils.getPlayRequirementOfAction(objectParser, battleCryElement);
        PlayAction<Actor> action
                = parseTargetedAction(objectParser, battleCryElement, targetNeed, actorType);

        PlayActionRequirement actionCondition = ParserUtils.getActionConditionOfAction(objectParser, battleCryElement);
        action = addCondition(actionCondition, action);

        return new PlayActionDef<>(targetNeed, requirement, action);
    }

    public static <Actor extends PlayerProperty> boolean parsePlayActionDefs(
            JsonDeserializer objectParser,
            JsonTree actionDefsElement,
            Class<Actor> actorType,
            Consumer<PlayActionDef<Actor>> actionDefProcessor) throws ObjectParsingException {

        if (actionDefsElement == null) {
            return false;
        }

        if (actionDefsElement.isJsonArray()) {
            for (JsonTree singleActionDefElement: actionDefsElement.getChildren()) {
                actionDefProcessor.accept(parseSinglePlayActionDef(objectParser, singleActionDefElement, actorType));
            }
            return actionDefsElement.getChildCount() > 0;
        }
        else {
            actionDefProcessor.accept(parseSinglePlayActionDef(objectParser, actionDefsElement, actorType));
            return true;
        }
    }

    private static <Actor extends PlayerProperty> PlayAction<Actor> addCondition(
            PlayActionRequirement condition,
            PlayAction<Actor> action) {
        if (condition == PlayActionRequirement.ALLOWED) {
            return action;
        }

        return (World world, Actor actor, Optional<TargetableCharacter> target) -> {
            if (condition.meetsRequirement(actor.getOwner())) {
                return action.alterWorld(world, actor, target);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private static final class BuffDescr {
        public final int attack;
        public final int hp;

        public BuffDescr(int attack, int hp) {
            this.attack = attack;
            this.hp = hp;
        }

        public static BuffDescr tryCreate(String str) {
            String[] attackHp = str.split("/");
            if (attackHp.length != 2) {
                return null;
            }

            try {
                int attack = Integer.parseInt(attackHp[0].trim());
                int hp = Integer.parseInt(attackHp[1].trim());
                return new BuffDescr(attack, hp);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    private ParserUtils() {
        throw new AssertionError();
    }
}
