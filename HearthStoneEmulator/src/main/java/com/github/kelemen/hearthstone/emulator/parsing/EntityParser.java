package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.HearthStoneEntity;

public interface EntityParser<Entity extends HearthStoneEntity> {
    public Entity fromJson(JsonTree root) throws ObjectParsingException;
}
