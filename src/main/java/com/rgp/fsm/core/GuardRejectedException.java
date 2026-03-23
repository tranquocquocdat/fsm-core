package com.rgp.fsm.core;

/**
 * Thrown when a transition's guard predicate returns false.
 * Indicates the precondition for a state transition was not met.
 */
public class GuardRejectedException extends FsmException {
    private final Object fromState;
    private final Object event;

    public GuardRejectedException(Object fromState, Object event) {
        super("Guard rejected transition: " + fromState + " -> " + event);
        this.fromState = fromState;
        this.event = event;
    }

    public Object getFromState() { return fromState; }
    public Object getEvent() { return event; }
}
