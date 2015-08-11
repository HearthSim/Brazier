package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.HearthStoneEntityDatabase;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.BattleCryAction;
import com.github.kelemen.hearthstone.emulator.actions.BattleCryArg;
import com.github.kelemen.hearthstone.emulator.actions.BattleCryTargetedAction;
import com.github.kelemen.hearthstone.emulator.actions.PlayActionRequirement;
import com.github.kelemen.hearthstone.emulator.actions.TargetNeed;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

public final class MinionParser implements EntityParser<MinionDescr> {
    private final Supplier<HearthStoneEntityDatabase<CardDescr>> cardDbRef;
    private final EventNotificationParser<Minion> eventNotificationParser;
    private final JsonDeserializer objectParser;

    public MinionParser(
            Supplier<HearthStoneEntityDatabase<CardDescr>> cardDbRef,
            JsonDeserializer objectParser) {
        ExceptionHelper.checkNotNullArgument(cardDbRef, "cardDbRef");
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");

        this.cardDbRef = cardDbRef;
        this.eventNotificationParser = new EventNotificationParser<>(Minion.class, objectParser);
        this.objectParser = objectParser;
    }

    private CardDescr getCard(CardId cardId) {
        HearthStoneEntityDatabase<CardDescr> cardDb = cardDbRef.get();
        if (cardDb == null) {
            return null;
        }

        return cardDb.getById(cardId);
    }

    private static BattleCryTargetedAction addCondition(
            PlayActionRequirement condition,
            BattleCryTargetedAction action) {
        if (condition == PlayActionRequirement.ALLOWED) {
            return action;
        }

        return (World world, BattleCryArg arg) -> {
            if (condition.meetsRequirement(arg.getCastingPlayer())) {
                return action.alterWorld(world, arg);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private void parseSingleBattleCry(
            JsonTree battleCryElement,
            MinionDescr.Builder abilities) throws ObjectParsingException {

        BattleCryTargetedAction action = objectParser.toJavaObject(battleCryElement, BattleCryTargetedAction.class);
        TargetNeed targetNeed = ParserUtils.getTargetNeedOfAction(objectParser, battleCryElement);
        PlayActionRequirement requirement = ParserUtils.getPlayRequirementOfAction(objectParser, battleCryElement);

        PlayActionRequirement actionCondition = ParserUtils.getActionConditionOfAction(objectParser, battleCryElement);
        action = addCondition(actionCondition, action);

        abilities.addBattleCry(new BattleCryAction(targetNeed, requirement, action));
    }

    private boolean parseBattleCries(
            JsonTree battleCriesElement,
            MinionDescr.Builder abilities) throws ObjectParsingException {

        if (battleCriesElement == null) {
            return false;
        }

        if (battleCriesElement.isJsonArray()) {
            for (JsonTree singleBattleCryElement: battleCriesElement.getChildren()) {
                parseSingleBattleCry(singleBattleCryElement, abilities);
            }
            return battleCriesElement.getChildCount() > 0;
        }
        else {
            parseSingleBattleCry(battleCriesElement, abilities);
            return true;
        }
    }

    @Override
    public MinionDescr fromJson(JsonTree root) throws ObjectParsingException {
        String name = ParserUtils.getStringField(root, "name");
        int attack = ParserUtils.getIntField(root, "attack");
        int hp = ParserUtils.getIntField(root, "hp");

        String cardName = ParserUtils.tryGetStringField(root, "cardId");
        if (cardName == null) {
            cardName = name;
        }

        MinionId minionId = new MinionId(name);
        CardId cardId = new CardId(cardName);

        MinionDescr.Builder result = new MinionDescr.Builder(minionId, attack, hp, () -> getCard(cardId));

        JsonTree maxAttackCountElement = root.getChild("maxAttackCount");
        if (maxAttackCountElement != null) {
            result.setMaxAttackCount(maxAttackCountElement.getAsInt());
        }

        JsonTree canAttackElement = root.getChild("canAttack");
        if (canAttackElement != null) {
            result.setCanAttack(canAttackElement.getAsBoolean());
        }

        JsonTree displayNameElement = root.getChild("displayName");
        if (displayNameElement != null) {
            result.setDisplayName(displayNameElement.getAsString());
        }

        JsonTree tauntElement = root.getChild("taunt");
        if (tauntElement != null) {
            result.setTaunt(tauntElement.getAsBoolean());
        }

        JsonTree divineShieldElement = root.getChild("divineShield");
        if (divineShieldElement != null) {
            result.setDivineShield(divineShieldElement.getAsBoolean());
        }

        JsonTree chargeElement = root.getChild("charge");
        if (chargeElement != null) {
            result.setCharge(chargeElement.getAsBoolean());
        }

        JsonTree targetableElement = root.getChild("targetable");
        if (targetableElement != null) {
            result.setTargetable(targetableElement.getAsBoolean());
        }

        JsonTree attackWithHpElement = root.getChild("attackWithHp");
        if (attackWithHpElement != null) {
            if (attackWithHpElement.getAsBoolean()) {
                result.setAttackFinalizer((owner, prev) -> owner.getBody().getCurrentHp());
            }
        }

        JsonTree stealthElement = root.getChild("stealth");
        if (stealthElement != null) {
            result.setStealth(stealthElement.getAsBoolean());
        }

        JsonTree attackLeftElement = root.getChild("attackLeft");
        if (attackLeftElement != null) {
            result.setAttackLeft(attackLeftElement.getAsBoolean());
        }

        JsonTree attackRightElement = root.getChild("attackRight");
        if (attackRightElement != null) {
            result.setAttackRight(attackRightElement.getAsBoolean());
        }

        JsonTree keywords = root.getChild("keywords");
        if (keywords != null) {
            ParserUtils.parseKeywords(keywords, result::addKeyword);
        }

        if (parseBattleCries(root.getChild("battleCries"), result)) {
            result.addKeyword(Keywords.BATTLE_CRY);
        }

        result.setAbilities(ParserUtils.parseAbilities(Minion.class, objectParser, eventNotificationParser, root));

        return result.create();
    }
}
