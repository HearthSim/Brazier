package com.github.kelemen.brazier.parsing;

import java.lang.reflect.Type;

public interface TypeChecker {
    public void checkType(Type type) throws ObjectParsingException;
}
