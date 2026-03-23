package com.rgp.fsm.core;

/**
 * Thrown when a command's validate() method fails.
 * Indicates business rule validation did not pass.
 */
public class ValidationException extends FsmException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
