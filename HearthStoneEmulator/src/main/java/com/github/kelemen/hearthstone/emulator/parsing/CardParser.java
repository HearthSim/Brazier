package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.EntityId;
import com.github.kelemen.hearthstone.emulator.HearthStoneEntityDatabase;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.Secret;
import com.github.kelemen.hearthstone.emulator.SecretContainer;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.actions.BasicFilters;
import com.github.kelemen.hearthstone.emulator.actions.CardPlayAction;
import com.github.kelemen.hearthstone.emulator.actions.CardPlayArg;
import com.github.kelemen.hearthstone.emulator.actions.ManaCostAdjuster;
import com.github.kelemen.hearthstone.emulator.actions.PlayActionRequirement;
import com.github.kelemen.hearthstone.emulator.actions.PlayerAction;
import com.github.kelemen.hearthstone.emulator.actions.TargetNeed;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventActionDefs;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.cards.CardPlayActionDef;
import com.github.kelemen.hearthstone.emulator.cards.CardProvider;
import com.github.kelemen.hearthstone.emulator.cards.CardRarity;
import com.github.kelemen.hearthstone.emulator.cards.CardType;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

public final class CardParser implements EntityParser<CardDescr> {
    private final JsonDeserializer objectParser;
    private final HearthStoneEntityDatabase<MinionDescr> minionDb;
    private final EventNotificationParser<Secret> secretParser;

    public CardParser(
            HearthStoneEntityDatabase<MinionDescr> minionDb,
            JsonDeserializer objectParser) {
        ExceptionHelper.checkNotNullArgument(minionDb, "minionDb");
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");

        this.minionDb = minionDb;
        this.objectParser = objectParser;
        this.secretParser = new EventNotificationParser<>(
                Secret.class,
                objectParser,
                BasicFilters.NOT_SELF_TURN,
                CardParser::unregisterSecret);
    }

    private static UndoAction unregisterSecret(World world, Secret self, Object eventSource) {
        UndoAction removeUndo = self.getOwner().getSecrets().removeSecret(self);
        UndoAction eventUndo = world.getEvents().secretRevealedListeners().triggerEvent(self);
        return () -> {
            eventUndo.undo();
            removeUndo.undo();
        };
    }

    private void parseAbility(
            JsonTree abilityElement,
            CardDescr.Builder abilities) throws ObjectParsingException {
        if (abilityElement == null) {
            return;
        }

        // Unsafe but there is nothing we can do about it.
        @SuppressWarnings("unchecked")
        ActivatableAbility<? super Card> ability = (ActivatableAbility<? super Card>)objectParser.toJavaObject(
                abilityElement,
                ActivatableAbility.class,
                TypeCheckers.genericTypeChecker(ActivatableAbility.class, Card.class));
        abilities.setInHandAbility(ability);
    }

    private boolean parseMinion(JsonTree minionElement, CardDescr.Builder result) {
        if (minionElement == null) {
            return false;
        }

        String minionId = minionElement.getAsString();
        MinionDescr minion = minionDb.getById(new MinionId(minionId));
        result.setMinion(minion);
        return true;
    }

    private static CardType parseCardType(JsonTree cardTypeElement) throws ObjectParsingException {
        if (cardTypeElement == null) {
            return CardType.UNKNOWN;
        }

        String cardTypeStr = cardTypeElement.getAsString().toUpperCase(Locale.ROOT);
        try {
            return CardType.valueOf(cardTypeStr);
        } catch (IllegalArgumentException ex) {
            throw new ObjectParsingException("Unknown card type: " + cardTypeElement.getAsString());
        }
    }

    private static CardPlayAction getTargetedAction(
            JsonDeserializer objectParser,
            JsonTree actionElement) throws ObjectParsingException {

        if (actionElement.isJsonObject() && actionElement.getChild("class") == null) {
            JsonTree actionsDefElement = actionElement.getChild("actions");
            if (actionsDefElement == null) {
                throw new ObjectParsingException("Missing action definition for CardPlayAction.");
            }
            return objectParser.toJavaObject(actionsDefElement, CardPlayAction.class);
        }

        return objectParser.toJavaObject(actionElement, CardPlayAction.class);
    }

    public static CardPlayActionDef parsePlayAction(
            JsonDeserializer objectParser,
            JsonTree actionElement) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");
        ExceptionHelper.checkNotNullArgument(actionElement, "actionElement");

        CardPlayAction action = getTargetedAction(objectParser, actionElement);
        TargetNeed targetNeed = ParserUtils.getTargetNeedOfAction(objectParser, actionElement);
        PlayActionRequirement requirement = ParserUtils.getPlayRequirementOfAction(objectParser, actionElement);
        return new CardPlayActionDef(targetNeed, requirement, action);
    }

    private void parseSinglePlayAction(
            JsonTree actionElement,
            CardDescr.Builder result) throws ObjectParsingException {

        result.addOnPlayAction(parsePlayAction(objectParser, actionElement));
    }

    private void parsePlayActions(JsonTree actionsElement, CardDescr.Builder result) throws ObjectParsingException {
        if (actionsElement == null) {
            return;
        }

        if (actionsElement.isJsonArray()) {
            for (JsonTree singleActionElement: actionsElement.getChildren()) {
                parseSinglePlayAction(singleActionElement, result);
            }
        }
        else {
            parseSinglePlayAction(actionsElement, result);
        }
    }

    private static boolean isCollectible(JsonTree collectibleElement) {
        return collectibleElement == null || collectibleElement.getAsBoolean();
    }

    private void parseSingleCardAdjuster(JsonTree cardAdjusters, CardDescr.Builder result) throws ObjectParsingException {
        ManaCostAdjuster adjuster = objectParser.toJavaObject(cardAdjusters, ManaCostAdjuster.class);
        result.addManaCostAdjuster(adjuster);
    }

    private void parseCardAdjusters(JsonTree cardAdjusters, CardDescr.Builder result) throws ObjectParsingException {
        if (cardAdjusters == null) {
            return;
        }

        if (cardAdjusters.isJsonArray()) {
            for (JsonTree singleCardAdjuster: cardAdjusters.getChildren()) {
                parseSingleCardAdjuster(singleCardAdjuster, result);
            }
        }
        else {
            parseSingleCardAdjuster(cardAdjusters, result);
        }
    }

    private PlayActionRequirement secretRequirement(EntityId secretId) {
        return (Player player) -> {
            SecretContainer secrets = player.getSecrets();
            return !secrets.isFull() && secrets.findById(secretId) == null;
        };
    }

    private CardPlayAction secretAction(Supplier<CardDescr> cardRef, WorldEventActionDefs<Secret> secretActionDef) {
        ExceptionHelper.checkNotNullArgument(secretActionDef, "secretActionDef");
        return (World world, CardPlayArg arg) -> {
            CardDescr card = cardRef.get();
            Player player = arg.getTarget().getCastingPlayer();
            Secret secret = new Secret(player, card, secretActionDef);
            return player.getSecrets().addSecret(secret);
        };
    }

    private void parseSecretPlayAction(
            JsonTree secretElement,
            EntityId secretId,
            Supplier<CardDescr> cardRef,
            CardDescr.Builder result) throws ObjectParsingException {

        if (secretElement == null) {
            return;
        }

        WorldEventActionDefs<Secret> secretActionDef = secretParser.fromJson(secretElement);

        result.addKeyword(Keywords.SECRET);

        result.addOnPlayAction(new CardPlayActionDef(
                TargetNeed.NO_NEED,
                secretRequirement(secretId),
                secretAction(cardRef, secretActionDef)));
    }

    @Override
    public CardDescr fromJson(JsonTree root) throws ObjectParsingException {
        String name = ParserUtils.getStringField(root, "name");
        int manaCost = ParserUtils.getIntField(root, "manaCost");
        CardType cardType = parseCardType(root.getChild("type"));

        JsonTree minionElement = root.getChild("minion");
        if (minionElement != null && cardType == CardType.UNKNOWN) {
            cardType = CardType.MINION;
        }

        CardId cardId = new CardId(name);
        CardDescr.Builder result = new CardDescr.Builder(cardId, cardType, manaCost);

        result.addKeyword(cardType.getKeyword());
        result.addKeyword(Keywords.manaCost(manaCost));

        String description = ParserUtils.tryGetStringField(root, "description");
        if (description != null) {
            result.setDescription(description);
        }

        result.addKeyword(isCollectible(root.getChild("collectible"))
                ? Keywords.COLLECTIBLE
                : Keywords.NON_COLLECTIBLE);

        JsonTree keywords = root.getChild("keywords");
        if (keywords != null) {
            ParserUtils.parseKeywords(keywords, result::addKeyword);
        }

        JsonTree rarityElement = root.getChild("rarity");
        CardRarity rarity = CardRarity.COMMON;
        if (rarityElement != null) {
            rarity = objectParser.toJavaObject(rarityElement, CardRarity.class);
        }

        result.setRarity(rarity);
        result.addKeyword(Keyword.create(rarity.name()));

        JsonTree displayName = root.getChild("displayName");
        if (displayName != null) {
            result.setDisplayName(displayName.getAsString());
        }

        JsonTree overloadElement = root.getChild("overload");
        if (overloadElement != null) {
            int overload = overloadElement.getAsInt();
            if (overload > 0) {
                result.addKeyword(Keywords.OVERLOAD);
            }
            result.setOverload(overload);
        }

        JsonTree drawActions = root.getChild("drawActions");
        if (drawActions != null) {
            PlayerAction onDrawAction = objectParser.toJavaObject(drawActions, PlayerAction.class);
            result.addOnDrawAction(onDrawAction);
        }

        JsonTree chooseOneElement = root.getChild("chooseOne");
        if (chooseOneElement != null) {
            CardProvider[] choices = objectParser.toJavaObject(chooseOneElement, CardProvider[].class);
            for (CardProvider choice: choices) {
                result.addChooseOneAction(choice);
            }
        }

        JsonTree classElement = root.getChild("class");
        if (classElement == null) {
            throw new ObjectParsingException("Class of card is unspecified for " + name);
        }
        Keyword cardClass = Keyword.create(classElement.getAsString());
        result.setCardClass(cardClass);
        result.addKeyword(cardClass);

        parsePlayActions(root.getChild("playActions"), result);
        parseCardAdjusters(root.getChild("manaCostAdjusters"), result);

        if (minionElement == null && cardType == CardType.MINION) {
            MinionDescr minion = minionDb.getById(new MinionId(name));
            result.setMinion(minion);
        }
        else if (parseMinion(minionElement, result)) {
            if (cardType != CardType.MINION) {
                throw new ObjectParsingException("Card type must be minion to allow having an explicit minion declaration.");
            }
        }
        else {
            if (cardType == CardType.MINION) {
                throw new ObjectParsingException("Minion cards must have an explicit minion declaration.");
            }
        }

        AtomicReference<CardDescr> cardRef = new AtomicReference<>();
        parseSecretPlayAction(root.getChild("secret"), cardId, cardRef::get, result);

        parseAbility(root.getChild("inHandAbility"), result);

        CardDescr card = result.create();
        cardRef.set(card);
        return card;
    }
}
