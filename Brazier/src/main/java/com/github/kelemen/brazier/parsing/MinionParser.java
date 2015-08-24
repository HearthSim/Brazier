package com.github.kelemen.brazier.parsing;

import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.minions.MinionDescr;
import com.github.kelemen.brazier.minions.MinionId;
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

        boolean hasBattleCry = ParserUtils.parsePlayActionDefs(
                objectParser,
                root.getChild("battleCries"),
                Minion.class,
                result::addBattleCry);
        if (hasBattleCry) {
            result.addKeyword(Keywords.BATTLE_CRY);
        }

        result.setAbilities(ParserUtils.parseAbilities(Minion.class, objectParser, eventNotificationParser, root));

        return result.create();
    }
}
