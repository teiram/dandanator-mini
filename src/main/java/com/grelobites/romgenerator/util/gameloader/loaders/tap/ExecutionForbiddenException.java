package com.grelobites.romgenerator.util.gameloader.loaders.tap;

public class ExecutionForbiddenException extends RuntimeException {
    public ExecutionForbiddenException() {
    }

    public ExecutionForbiddenException(String message) {
        super(message);
    }

    public ExecutionForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
