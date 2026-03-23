package com.rgp.fsm.core;

/**
 * Thrown when a command's execute() method fails.
 * Wraps the original exception from the command execution.
 */
public class CommandExecutionException extends FsmException {
    public CommandExecutionException(String message) {
        super(message);
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
