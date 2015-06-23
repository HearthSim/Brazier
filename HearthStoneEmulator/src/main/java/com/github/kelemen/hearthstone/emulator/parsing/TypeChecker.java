package com.github.kelemen.hearthstone.emulator.parsing;

import java.lang.reflect.Type;

public interface TypeChecker {
    public void checkType(Type type) throws ObjectParsingException;
}
