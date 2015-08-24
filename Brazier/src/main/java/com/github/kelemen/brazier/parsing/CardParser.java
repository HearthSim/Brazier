package com.github.kelemen.brazier.parsing;

import com.github.kelemen.brazier.EntityId;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.Secret;
import com.github.kelemen.brazier.SecretContainer;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.actions.BasicFilters;
import com.github.kelemen.brazier.actions.ManaCostAdjuster;
import com.github.kelemen.brazier.actions.PlayActionDef;
import com.github.kelemen.brazier.actions.PlayActionRequirement;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.actions.TargetlessAction;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.cards.CardId;
import com.github.kelemen.brazier.cards.CardProvider;
import com.github.kelemen.brazier.cards.CardRarity;
import com.github.kelemen.brazier.cards.CardType;
import com.github.kelemen.brazier.cards.PlayAction;
import com.github.kelemen.brazier.events.WorldEventActionDefs;
import com.github.kelemen.brazier.minions.MinionDescr;
import com.github.kelemen.brazier.weapons.WeaponDescr;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

public final class CardParser implements EntityParser<CardDescr> {
    private final JsonDeserializer objectParser;
    private final MinionParser minionParser;
    private final WeaponParser weaponParser;
    private final EventNotificationParser<Secret> secretParser;

    public CardParser(JsonDeserializer objectParser) {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");

        this.objectParser = objectParser;
        this.minionParser = new MinionParser(objectParser);
        this.weaponParser = new WeaponParser(objectParser);
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

    private PlayAction<Card> secretAction(Supplier<CardDescr> cardRef, WorldEventActionDefs<Secret> secretActionDef) {
        ExceptionHelper.checkNotNullArgument(secretActionDef, "secretActionDef");
        return (World world, Card actor, Optional<TargetableCharacter> target) -> {
            CardDescr card = cardRef.get();
            Player player = actor.getOwner();
            Secret secret = new Secret(player, card, secretActionDef);
            return player.getSecrets().addSecret(secret);
        };
    }

    private boolean parseSecretPlayAction(
            JsonTree secretElement,
            EntityId secretId,
            Supplier<CardDescr> cardRef,
            CardDescr.Builder result) throws ObjectParsingException {

        if (secretElement == null) {
            return false;
        }

        WorldEventActionDefs<Secret> secretActionDef = secretParser.fromJson(secretElement);

        result.addOnPlayAction(new PlayActionDef<>(
                TargetNeed.NO_NEED,
                secretRequirement(secretId),
                secretAction(cardRef, secretActionDef)));
        return true;
    }

    @Override
    public CardDescr fromJson(JsonTree root) throws ObjectParsingException {
        return fromJsonWithCardType(root, null);
    }

    public CardDescr fromJson(JsonTree root, CardType predefinedCardType) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(predefinedCardType, "predefinedCardType");
        return fromJsonWithCardType(root, predefinedCardType);
    }

    private CardDescr fromJsonWithCardType(JsonTree root, CardType predefinedCardType) throws ObjectParsingException {
        String name = ParserUtils.getStringField(root, "name");
        int manaCost = ParserUtils.getIntField(root, "manaCost");
        Set<Keyword> keywords = new HashSet<>();
        CardType cardType = predefinedCardType != null
                ? predefinedCardType
                : parseCardType(root.getChild("type"));

        JsonTree minionElement = root.getChild("minion");
        if (minionElement != null && cardType == CardType.UNKNOWN) {
           if (cardType != CardType.UNKNOWN && cardType != CardType.MINION) {
                throw new ObjectParsingException("Minion containing card cannot have this type: " + cardType);
            }
            cardType = CardType.MINION;
        }

        JsonTree weaponElement = root.getChild("weapon");
        if (weaponElement != null) {
            if (cardType != CardType.UNKNOWN && cardType != CardType.WEAPON) {
                throw new ObjectParsingException("Weapon containing card cannot have this type: " + cardType);
            }
            cardType = CardType.WEAPON;
        }

        keywords.add(cardType.getKeyword());

        CardId cardId = new CardId(name);
        CardDescr.Builder result = new CardDescr.Builder(cardId, cardType, manaCost);

        keywords.add(cardType.getKeyword());
        keywords.add(Keywords.manaCost(manaCost));

        String description = ParserUtils.tryGetStringField(root, "description");
        if (description != null) {
            result.setDescription(description);
        }

        keywords.add(isCollectible(root.getChild("collectible"))
                ? Keywords.COLLECTIBLE
                : Keywords.NON_COLLECTIBLE);

        JsonTree keywordsElement = root.getChild("keywords");
        if (keywordsElement != null) {
            ParserUtils.parseKeywords(keywordsElement, keywords::add);
        }

        JsonTree rarityElement = root.getChild("rarity");
        CardRarity rarity = CardRarity.COMMON;
        if (rarityElement != null) {
            rarity = objectParser.toJavaObject(rarityElement, CardRarity.class);
        }

        result.setRarity(rarity);
        keywords.add(Keyword.create(rarity.name()));

        JsonTree displayName = root.getChild("displayName");
        if (displayName != null) {
            result.setDisplayName(displayName.getAsString());
        }

        JsonTree overloadElement = root.getChild("overload");
        if (overloadElement != null) {
            int overload = overloadElement.getAsInt();
            if (overload > 0) {
                keywords.add(Keywords.OVERLOAD);
            }
            result.setOverload(overload);
        }

        JsonTree drawActions = root.getChild("drawActions");
        if (drawActions != null) {
            @SuppressWarnings("unchecked")
            TargetlessAction<? super Card> onDrawAction = objectParser.toJavaObject(
                    drawActions,
                    TargetlessAction.class,
                    TypeCheckers.genericTypeChecker(TargetlessAction.class, Card.class));
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
        keywords.add(cardClass);

        ParserUtils.parsePlayActionDefs(objectParser, root.getChild("playActions"), Card.class, result::addOnPlayAction);
        parseCardAdjusters(root.getChild("manaCostAdjusters"), result);

        AtomicReference<CardDescr> cardRef = new AtomicReference<>();
        if (parseSecretPlayAction(root.getChild("secret"), cardId, cardRef::get, result)) {
            keywords.add(Keywords.SECRET);
        }

        parseAbility(root.getChild("inHandAbility"), result);

        keywords.forEach(result::addKeyword);
        // To ensure that we no longer add keywords from here on by accident.
        keywords = Collections.unmodifiableSet(keywords);


        if (minionElement == null && cardType == CardType.MINION) {
            throw new ObjectParsingException("Minion cards must have an explicit minion declaration.");
        }

        if (minionElement != null) {
            MinionDescr minion = minionParser.fromJson(minionElement, name, keywords, cardRef::get);
            result.setMinion(minion);
        }

        if (weaponElement != null) {
            WeaponDescr weapon = weaponParser.fromJson(weaponElement, name, keywords);
            result.setWeapon(weapon);
            PlayAction<Card> playAction = (World world, Card actor, Optional<TargetableCharacter> target) -> {
                return actor.getOwner().equipWeapon(weapon);
            };

            result.addOnPlayAction(new PlayActionDef<>(TargetNeed.NO_NEED, PlayActionRequirement.ALLOWED, playAction));
        }

        CardDescr card = result.create();
        cardRef.set(card);
        return card;
    }
}
