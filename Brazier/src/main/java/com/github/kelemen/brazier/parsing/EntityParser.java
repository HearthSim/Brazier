package com.github.kelemen.brazier.parsing;

import com.github.kelemen.brazier.HearthStoneEntity;

public interface EntityParser<Entity extends HearthStoneEntity> {
    public Entity fromJson(JsonTree root) throws ObjectParsingException;
}
