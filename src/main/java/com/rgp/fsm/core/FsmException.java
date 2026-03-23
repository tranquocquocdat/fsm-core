package com.rgp.fsm.core;

/**
 * Base runtime exception for all FSM errors.
 * Using RuntimeException so callers aren't forced to catch checked exceptions.
 */
public class FsmException extends RuntimeException {
    public FsmException(String message) {
        super(message);
    }

    public FsmException(String message, Throwable cause) {
        super(message, cause);
    }
}
