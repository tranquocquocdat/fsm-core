package com.rgp.fsm.engine;

/**
 * Result of a single state transition step.
 *
 * <p>Contains the new state after the transition and the output from the command's
 * {@link com.rgp.fsm.core.BaseCommand#execute(com.rgp.fsm.core.TransitionContext)} method.</p>
 *
 * <p>The output can be passed to the next step via {@link FluentManager.FireBuilder#bridge(StepResult)}
 * to enable data flow between saga steps.</p>
 *
 * @param state  the resulting state after the transition
 * @param output the output from the command execution (can be any type, or null)
 * @param <S> State enum type
 */
public record StepResult<S>(S state, Object output) {

    /**
     * Get the output cast to the expected type.
     *
     * <p>Convenience method to avoid manual casting:</p>
     * <pre>{@code
     * Map<String, Object> data = result.getOutput();
     * }</pre>
     *
     * @param <T> the expected output type
     * @return the output cast to T
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutput() {
        return (T) output;
    }
}
