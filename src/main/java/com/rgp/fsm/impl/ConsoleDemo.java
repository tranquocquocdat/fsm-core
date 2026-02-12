package com.rgp.fsm.impl;

import com.rgp.fsm.core.OutboxProducer;
import com.rgp.fsm.core.StateHistoryProcessor;
import com.rgp.fsm.core.TransitionContext;

public class ConsoleDemo {
    
    public static class ConsoleOutboxProducer implements OutboxProducer {
        @Override
        public void persist(String aggregateId, String eventName, Object payload) {
            System.out.println("[Outbox] PERSIST EVENT: " + eventName + " for ID: " + aggregateId);
            System.out.println("[Outbox] Payload: " + payload);
        }
    }

    public static class ConsoleStateHistoryProcessor implements StateHistoryProcessor<OrderStatus, OrderEvent> {
        @Override
        public void process(TransitionContext<OrderStatus, OrderEvent> ctx, OrderStatus nextState) {
            System.out.println("[Audit] HISTORY: " + ctx.aggregateId() + " [" + ctx.fromState() + " -> " + nextState + "] by " + ctx.event());
        }
    }
}
