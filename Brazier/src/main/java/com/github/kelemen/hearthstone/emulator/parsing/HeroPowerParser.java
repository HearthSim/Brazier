package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.HeroPowerDef;
import com.github.kelemen.hearthstone.emulator.HeroPowerId;
import org.jtrim.utils.ExceptionHelper;

public final class HeroPowerParser implements EntityParser<HeroPowerDef> {
    private final JsonDeserializer objectParser;

    public HeroPowerParser(JsonDeserializer objectParser) {
        ExceptionHelper.checkNotNullArgument(objectParser, "objectParser");

        this.objectParser = objectParser;
    }

    private void parseSinglePlayAction(
            JsonTree actionElement,
            HeroPowerDef.Builder result) throws ObjectParsingException {

        result.addAction(CardParser.parsePlayAction(objectParser, actionElement));
    }

    private void parsePlayActions(JsonTree actionsElement, HeroPowerDef.Builder result) throws ObjectParsingException {
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

    @Override
    public HeroPowerDef fromJson(JsonTree root) throws ObjectParsingException {
        String name = ParserUtils.getStringField(root, "name");

        HeroPowerId id = new HeroPowerId(name);
        HeroPowerDef.Builder result = new HeroPowerDef.Builder(id);

        result.setManaCost(ParserUtils.getIntField(root, "manaCost"));

        JsonTree maxUseCountElement = root.getChild("maxUseCount");
        if (maxUseCountElement != null) {
            result.setMaxUseCount(maxUseCountElement.getAsInt());
        }

        JsonTree descriptionElement = root.getChild("description");
        if (descriptionElement != null) {
            result.setDescription(descriptionElement.getAsString());
        }

        parsePlayActions(root.getChild("action"), result);

        return result.create();
    }
}
