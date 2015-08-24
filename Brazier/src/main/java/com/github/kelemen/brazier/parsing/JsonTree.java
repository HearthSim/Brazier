package com.github.kelemen.brazier.parsing;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;

public interface JsonTree {
    public JsonElement getElement();

    public int getChildCount();
    public JsonTree getChild(int index);
    public JsonTree getChild(String name);

    public default Iterable<JsonTree> getChildren() {
        return () -> {
            return new Iterator<JsonTree>() {
                private int nextIndex = 0;

                @Override
                public boolean hasNext() {
                    return nextIndex < getChildCount();
                }

                @Override
                public JsonTree next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("End of Children");
                    }
                    int currentIndex = nextIndex;
                    nextIndex++;
                    return getChild(currentIndex);
                }
            };
        };
    }

    public default boolean isJsonArray() {
        return getElement().isJsonArray();
    }

    public default boolean isJsonObject() {
        return getElement().isJsonObject();
    }

    public default boolean isJsonPrimitive() {
        return getElement().isJsonPrimitive();
    }

    public default boolean isJsonNull() {
        return getElement().isJsonNull();
    }

    public default JsonPrimitive getAsJsonPrimitive() {
        return getElement().getAsJsonPrimitive();
    }

    public default JsonNull getAsJsonNull() {
        return getElement().getAsJsonNull();
    }

    public default boolean getAsBoolean() {
        return getElement().getAsBoolean();
    }

    public default Number getAsNumber() {
        return getElement().getAsNumber();
    }

    public default String getAsString() {
        return getElement().getAsString();
    }

    public default double getAsDouble() {
        return getElement().getAsDouble();
    }

    public default float getAsFloat() {
        return getElement().getAsFloat();
    }

    public default long getAsLong() {
        return getElement().getAsLong();
    }

    public default int getAsInt() {
        return getElement().getAsInt();
    }

    public default byte getAsByte() {
        return getElement().getAsByte();
    }

    public default char getAsCharacter() {
        return getElement().getAsCharacter();
    }

    public default BigDecimal getAsBigDecimal() {
        return getElement().getAsBigDecimal();
    }

    public default BigInteger getAsBigInteger() {
        return getElement().getAsBigInteger();
    }

    public default short getAsShort() {
        return getElement().getAsShort();
    }
}
