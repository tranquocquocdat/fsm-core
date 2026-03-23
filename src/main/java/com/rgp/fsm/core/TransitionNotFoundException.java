package com.rgp.fsm.core;

/**
 * Thrown when no matching transition is found for the given state + event.
 */
public class TransitionNotFoundException extends FsmException {
    private final Object fromState;
    private final Object event;

    public TransitionNotFoundException(Object fromState, Object event) {
        super("No transition found for: " + fromState + " -> " + event);
        this.fromState = fromState;
        this.event = event;
    }

    public Object getFromState() { return fromState; }
    public Object getEvent() { return event; }
}
