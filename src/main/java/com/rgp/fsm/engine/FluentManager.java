package com.rgp.fsm.engine;

import com.rgp.fsm.core.Transition;
import com.rgp.fsm.core.TransitionContext;
import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.OutboxProducer;
import com.rgp.fsm.core.StateHistoryProcessor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

public class FluentManager<S, E> {
    private final List<Transition<S, E>> transitions;

    public FluentManager(List<Transition<S, E>> transitions) {
        this.transitions = transitions;
    }

    @Transactional(rollbackFor = Exception.class)
    public StepResult<S> fire(String aggregateId, Integer version, S currentState, E event, Map<String, Object> params) throws Exception {
        // 1. Tìm bước chuyển
        var transition = transitions.stream()
            .filter(t -> t.from().equals(currentState) && t.event().equals(event))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Transition not found for: " + currentState + " -> " + event));

        var ctx = new TransitionContext<>(aggregateId, version, currentState, event, params);

        // 2. Guard Check (Luồng)
        if (transition.guard() != null && !transition.guard().test(ctx)) {
            throw new Exception("[Guard Alert] Trạng thái không cho phép: " + currentState + " -> " + event);
        }

        try {
            // 3. Validation Check (Nghiệp vụ sâu)
            transition.action().validate(ctx);

            // 4. Execute Action & Capture Output
            Object output = transition.action().execute(ctx);

            // 4. Per-Step Outbox
            if (transition.outboxProducer() != null && transition.eventToEmit() != null) {
                Object payload = (transition.payloadBuilder() != null) 
                    ? transition.payloadBuilder().apply(ctx) : params;
                transition.outboxProducer().persist(aggregateId, transition.eventToEmit(), payload);
            }

            // 5. Per-Step History
            if (transition.historyProcessor() != null) {
                transition.historyProcessor().process(ctx, transition.to());
            }

            return new StepResult<>(transition.to(), output);

        } catch (Exception ex) {
            // 6. THỰC THI HOÀN TÁC (Chỉ chạy nếu được cấu hình tường minh)
            if (transition.undoAction() != null) {
                transition.undoAction().execute(ctx);
            } else if (transition.callUndo()) {
                transition.action().undo(ctx); // Gọi hàm undo() mặc định của chính Command đó
            }
            
            if (transition.errorState() != null) {
                return new StepResult<>(transition.errorState(), null);
            }
            throw ex; 
        }
    }

    // --- FLUENT EXECUTION API ---
    public FireBuilder fire(String aggregateId) {
        return new FireBuilder(aggregateId);
    }

    public class FireBuilder {
        private final String aggregateId;
        private Integer v;
        private S s;
        private E e;
        private Map<String, Object> p = new java.util.HashMap<>();

        public FireBuilder(String aggregateId) { this.aggregateId = aggregateId; }
        
        public FireBuilder on(E event) { this.e = event; return this; }
        public FireBuilder from(S state) { this.s = state; return this; }
        public FireBuilder version(Integer version) { this.v = version; return this; }
        public FireBuilder params(Map<String, Object> params) { 
            if (params != null) this.p.putAll(params); 
            return this; 
        }

        // TÍNH NĂNG MỚI: BRIDGE - Nối dữ liệu từ bước trước sang bước sau
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
        
        public StepResult<S> execute() throws Exception {
            return FluentManager.this.fire(aggregateId, v, s, e, p);
        }
    }
}
