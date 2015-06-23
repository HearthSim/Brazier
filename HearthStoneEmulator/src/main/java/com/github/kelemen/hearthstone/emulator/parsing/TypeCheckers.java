package com.github.kelemen.hearthstone.emulator.parsing;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import org.jtrim.utils.ExceptionHelper;

public final class TypeCheckers {
    private static void checkAssignable(Class<?> srcType, Type destType) throws ObjectParsingException {
        if (destType instanceof Class) {
            if (!((Class<?>)destType).isAssignableFrom(srcType)) {
                throw new ObjectParsingException("Not assignable. From: " + srcType.getName() + ". To: " + destType.getTypeName());
            }
            return;
        }

        throw new ObjectParsingException("Unsupported declaration type: " + destType.getClass().getName());
    }

    public static TypeChecker genericTypeChecker(Class<?> rawType, Class<?>... argumentTypes) {
        ExceptionHelper.checkNotNullArgument(rawType, "rawType");

        Class<?>[] argTypesCopy = argumentTypes.clone();
        ExceptionHelper.checkNotNullElements(argTypesCopy, "argumentTypes");

        return (Type type) -> {
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType)type;
                if (pType.getRawType() == rawType) {
                    Type[] typeArgs = pType.getActualTypeArguments();
                    if (typeArgs.length != argumentTypes.length) {
                        throw new ObjectParsingException(rawType.getName() +
                                " has an unexpected number of type arguments: "
                                + Arrays.toString(typeArgs));
                    }

                    for (int i = 0; i < argTypesCopy.length; i++) {
                        checkAssignable(argTypesCopy[i], typeArgs[i]);
                    }
                }
            }
        };
    }

    private TypeCheckers() {
        throw new AssertionError();
    }
}
