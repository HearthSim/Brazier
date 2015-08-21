package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.brazier.actions.TargetedAction;
import com.github.kelemen.brazier.actions.TargetlessAction;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.BattleCryAction;
import com.github.kelemen.hearthstone.emulator.actions.PlayActionRequirement;
import com.github.kelemen.hearthstone.emulator.actions.TargetNeed;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import java.util.Set;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

public final class MinionParser {
    private final EventNotificationParser<Minion> eventNotificationParser;
    private final JsonDeserializer objectParser;

    public MinionParser(JsonDeserializer objectParser) {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");

        this.eventNotificationParser = new EventNotificationParser<>(Minion.class, objectParser);
        this.objectParser = objectParser;
    }

    private static TargetedAction<? super Minion, ? super TargetableCharacter> addCondition(
            PlayActionRequirement condition,
            TargetedAction<? super Minion, ? super TargetableCharacter> action) {
        if (condition == PlayActionRequirement.ALLOWED) {
            return action;
        }

        return (World world, Minion actor, TargetableCharacter target) -> {
            if (condition.meetsRequirement(actor.getOwner())) {
                return action.alterWorld(world, actor, target);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private <T> TargetedAction<? super Minion, ? super TargetableCharacter> parseBattleCryAction(
            JsonTree battleCryElement,
            Class<T> targetType) throws ObjectParsingException {
        @SuppressWarnings("unchecked")
        TargetedAction<? super Minion, ? super T> result = objectParser.toJavaObject(
                battleCryElement,
                TargetedAction.class,
                TypeCheckers.genericTypeChecker(TargetedAction.class, Minion.class, targetType));
        return (world, actor, target) -> {
            if (targetType.isInstance(target)) {
                return result.alterWorld(world, actor, targetType.cast(target));
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    private TargetedAction<? super Minion, ? super TargetableCharacter> parseBattleCryAction(
            JsonTree battleCryElement,
            TargetNeed targetNeed) throws ObjectParsingException {

        if (!targetNeed.mayTargetHero()) {
            if (!targetNeed.mayTargetMinion()) {
                @SuppressWarnings("unchecked")
                TargetlessAction<Minion> result = objectParser.toJavaObject(
                        battleCryElement,
                        TargetlessAction.class,
                        TypeCheckers.genericTypeChecker(TargetlessAction.class, Minion.class));
                return (world, actor, target) -> result.alterWorld(world, actor);
            }
            else {
                return parseBattleCryAction(battleCryElement, Minion.class);
            }
        }
        else {
            if (!targetNeed.mayTargetMinion()) {
                return parseBattleCryAction(battleCryElement, Hero.class);
            }
            else {
                return parseBattleCryAction(battleCryElement, TargetableCharacter.class);
            }
        }
    }

    private void parseSingleBattleCry(
            JsonTree battleCryElement,
            MinionDescr.Builder abilities) throws ObjectParsingException {

        TargetNeed targetNeed = ParserUtils.getTargetNeedOfAction(objectParser, battleCryElement);
        PlayActionRequirement requirement = ParserUtils.getPlayRequirementOfAction(objectParser, battleCryElement);
        TargetedAction<? super Minion, ? super TargetableCharacter> action = parseBattleCryAction(battleCryElement, targetNeed);

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

    public MinionDescr fromJson(
            JsonTree root,
            String name,
            Set<Keyword> keywords,
            Supplier<CardDescr> cardRef) throws ObjectParsingException {
        int attack = ParserUtils.getIntField(root, "attack");
        int hp = ParserUtils.getIntField(root, "hp");

        MinionId minionId = new MinionId(name);

        MinionDescr.Builder result = new MinionDescr.Builder(minionId, attack, hp, cardRef);

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

        keywords.forEach(result::addKeyword);

        if (parseBattleCries(root.getChild("battleCries"), result)) {
            result.addKeyword(Keywords.BATTLE_CRY);
        }

        result.setAbilities(ParserUtils.parseAbilities(Minion.class, objectParser, eventNotificationParser, root));

        return result.create();
    }
}
