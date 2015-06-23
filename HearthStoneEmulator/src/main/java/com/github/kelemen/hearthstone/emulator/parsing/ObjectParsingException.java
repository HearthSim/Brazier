package com.github.kelemen.hearthstone.emulator.parsing;

public class ObjectParsingException extends Exception {
    private static final long serialVersionUID = 1L;

    public ObjectParsingException(String message) {
        super(message);
    }

    public ObjectParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
