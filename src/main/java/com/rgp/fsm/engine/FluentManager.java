package com.rgp.fsm.engine;

import com.rgp.fsm.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

/**
 * The core FSM engine that executes state transitions.
 *
 * <p>For each {@code fire()} call, the engine:</p>
 * <ol>
 *   <li>Finds the matching {@link Transition} for the current state + event</li>
 *   <li>Checks the guard predicate (if configured)</li>
 *   <li>Calls {@link BaseCommand#validate(TransitionContext)}</li>
 *   <li>Calls {@link BaseCommand#execute(TransitionContext)}</li>
 *   <li>On success: persists outbox event, records history</li>
 *   <li>On failure: runs undo, persists error outbox, transitions to error state</li>
 * </ol>
 *
 * <p><b>Note:</b> This class is NOT a Spring-managed bean. {@code @Transactional} will NOT work.
 * Wrap calls to {@code fire()} in a {@code @Transactional} method in your own {@code @Service}.</p>
 *
 * @param <S> State enum type
 * @param <E> Event enum type
 */
public class FluentManager<S, E> {

    private static final Logger log = LoggerFactory.getLogger(FluentManager.class);

    private final List<Transition<S, E>> transitions;

    public FluentManager(List<Transition<S, E>> transitions) {
        this.transitions = transitions;
    }

    /**
     * Execute a state transition.
     *
     * @param aggregateId  the aggregate identifier
     * @param version      the current version for optimistic locking (nullable)
     * @param currentState the current state of the aggregate
     * @param event        the event triggering the transition
     * @param params       arbitrary parameters for the command
     * @return the result containing the new state and command output
     * @throws TransitionNotFoundException if no matching transition exists
     * @throws GuardRejectedException      if the guard predicate returns false
     * @throws CommandExecutionException   if command validation or execution fails
     */
    public StepResult<S> fire(String aggregateId, Integer version, S currentState, E event, Map<String, Object> params) {
        log.debug("FSM fire: aggregate={}, state={}, event={}", aggregateId, currentState, event);

        // 1. Find transition
        var transition = transitions.stream()
            .filter(t -> t.from().equals(currentState) && t.event().equals(event))
            .findFirst()
            .orElseThrow(() -> {
                log.error("No transition found: state={}, event={}", currentState, event);
                return new TransitionNotFoundException(currentState, event);
            });

        var ctx = new TransitionContext<>(aggregateId, version, currentState, event, params);

        // 2. Guard check
        if (transition.guard() != null && !transition.guard().test(ctx)) {
            log.warn("Guard rejected: aggregate={}, state={}, event={}", aggregateId, currentState, event);
            throw new GuardRejectedException(currentState, event);
        }

        try {
            // 3. Validation
            log.debug("Validating command for: aggregate={}", aggregateId);
            transition.action().validate(ctx);

            // 4. Execute
            log.debug("Executing command for: aggregate={}", aggregateId);
            Object output = transition.action().execute(ctx);

            // 5. Success outbox
            if (transition.successOutbox() != null) {
                var outbox = transition.successOutbox();
                Object payload = (outbox.payloadBuilder() != null)
                    ? outbox.payloadBuilder().apply(ctx) : params;
                outbox.producer().persist(aggregateId, outbox.eventName(), payload);
                log.debug("Outbox persisted: aggregate={}, event={}", aggregateId, outbox.eventName());
            }

            // 6. History
            if (transition.historyProcessor() != null) {
                transition.historyProcessor().process(ctx, transition.to());
                log.debug("History recorded: aggregate={}, {} -> {}", aggregateId, currentState, transition.to());
            }

            log.info("Transition success: aggregate={}, {} --[{}]--> {}",
                aggregateId, currentState, event, transition.to());
            return new StepResult<>(transition.to(), output);

        } catch (FsmException ex) {
            // Re-throw FSM exceptions as-is (already typed)
            throw ex;
        } catch (Exception ex) {
            log.error("Command failed: aggregate={}, state={}, event={}, error={}",
                aggregateId, currentState, event, ex.getMessage());

            // 7. Error handling
            ErrorConfig<S, E> errorConfig = transition.errorConfig();
            if (errorConfig != null) {
                // Undo
                if (errorConfig.undoAction() != null) {
                    log.debug("Running custom undo for: aggregate={}", aggregateId);
                    errorConfig.undoAction().accept(ctx);
                } else if (errorConfig.callUndo()) {
                    log.debug("Running command undo for: aggregate={}", aggregateId);
                    transition.action().undo(ctx);
                }

                // 8. Error outbox
                if (errorConfig.outbox() != null) {
                    var errorOutbox = errorConfig.outbox();
                    Object errorPayload = (errorOutbox.payloadBuilder() != null)
                        ? errorOutbox.payloadBuilder().apply(ctx) : params;
                    errorOutbox.producer().persist(aggregateId, errorOutbox.eventName(), errorPayload);
                    log.debug("Error outbox persisted: aggregate={}, event={}", aggregateId, errorOutbox.eventName());
                }

                if (errorConfig.errorState() != null) {
                    log.info("Transition to error state: aggregate={}, {} --[{} FAILED]--> {}",
                        aggregateId, currentState, event, errorConfig.errorState());
                    return new StepResult<>(errorConfig.errorState(), null);
                }
            }

            throw new CommandExecutionException(
                "Command failed for: " + currentState + " --[" + event + "]--> " + transition.to(), ex);
        }
    }

    // --- FLUENT EXECUTION API ---

    /**
     * Start building a fluent fire command for the given aggregate.
     *
     * <p>Usage:</p>
     * <pre>{@code
     * StepResult<S> result = fsm.fire("ORD-1")
     *     .version(1)
     *     .from(CREATED)
     *     .on(PAY)
     *     .params(Map.of("amount", 100.0))
     *     .execute();
     * }</pre>
     *
     * @param aggregateId the aggregate identifier
     * @return a fluent builder for configuring and executing the fire
     */
    public FireBuilder fire(String aggregateId) {
        return new FireBuilder(aggregateId);
    }

    /**
     * Fluent builder for constructing and executing a fire command.
     */
    public class FireBuilder {
        private final String aggregateId;
        private Integer v;
        private S s;
        private E e;
        private Map<String, Object> p = new java.util.HashMap<>();

        public FireBuilder(String aggregateId) { this.aggregateId = aggregateId; }

        /** Set the triggering event. */
        public FireBuilder on(E event) { this.e = event; return this; }

        /** Set the current state. */
        public FireBuilder from(S state) { this.s = state; return this; }

        /** Set the version for optimistic locking. */
        public FireBuilder version(Integer version) { this.v = version; return this; }

        /** Add parameters for the command. */
        public FireBuilder params(Map<String, Object> params) {
            if (params != null) this.p.putAll(params);
            return this;
        }

        /**
         * Bridge data from a previous step's result into this step's params.
         *
         * <p>If the previous step's output is a {@code Map}, all entries are merged into params.
         * Otherwise, the output is stored under the key {@code "prev_output"}.</p>
         *
         * @param result the result from a previous step
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public FireBuilder bridge(StepResult<S> result) {
            if (result != null && result.output() != null) {
                if (result.output() instanceof Map) {
                    this.p.putAll((Map<String, Object>) result.output());
                } else {
                    this.p.put("prev_output", result.output());
                }
            }
            return this;
        }

        /**
         * Execute the fire command.
         *
         * @return the step result containing new state and output
         */
        public StepResult<S> execute() {
            return FluentManager.this.fire(aggregateId, v, s, e, p);
        }
    }
}
