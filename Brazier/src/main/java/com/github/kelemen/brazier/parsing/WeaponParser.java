package com.github.kelemen.brazier.parsing;

import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.weapons.Weapon;
import com.github.kelemen.brazier.weapons.WeaponDescr;
import com.github.kelemen.brazier.weapons.WeaponId;
import java.util.Collections;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class WeaponParser implements EntityParser<WeaponDescr> {
    private final JsonDeserializer objectParser;
    private final EventNotificationParser<Weapon> notificationParser;

    public WeaponParser(JsonDeserializer objectParser) {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");

        this.objectParser = objectParser;
        this.notificationParser = new EventNotificationParser<>(Weapon.class, objectParser);
    }

    @Override
    public WeaponDescr fromJson(JsonTree root) throws ObjectParsingException {
        String name = ParserUtils.getStringField(root, "name");

        JsonTree keywordsElement = root.getChild("keywords");
        Set<Keyword> keywords = keywordsElement != null
                ? ParserUtils.parseKeywords(keywordsElement)
                : Collections.emptySet();

        return fromJson(root, name, keywords);
    }

    public WeaponDescr fromJson(JsonTree root, String name, Set<Keyword> keywords) throws ObjectParsingException {
        int attack = ParserUtils.getIntField(root, "attack");
        int charges = ParserUtils.getIntField(root, "charges");

        WeaponDescr.Builder result = new WeaponDescr.Builder(new WeaponId(name), attack, charges);

        keywords.forEach(result::addKeyword);

        JsonTree maxAttackCountElement = root.getChild("maxAttackCount");
        if (maxAttackCountElement != null) {
            result.setMaxAttackCount(maxAttackCountElement.getAsInt());
        }

        JsonTree canRetaliateWithElement = root.getChild("canRetaliateWith");
        if (canRetaliateWithElement != null) {
            result.setCanRetaliateWith(canRetaliateWithElement.getAsBoolean());
        }

        JsonTree canTargetRetaliate = root.getChild("canTargetRetaliate");
        if (canTargetRetaliate != null) {
            result.setCanTargetRetaliate(canTargetRetaliate.getAsBoolean());
        }

        result.setAbilities(ParserUtils.parseAbilities(Weapon.class, objectParser, notificationParser, root));

        return result.create();
    }
}
