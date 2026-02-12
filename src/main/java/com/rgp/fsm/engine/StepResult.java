package com.rgp.fsm.engine;

public record StepResult<S>(S state, Object output) {
    @SuppressWarnings("unchecked")
    public <T> T getOutput() {
        return (T) output;
    }
}
