package com.rgp.fsm.core;

public interface OutboxProducer {
    void persist(String aggregateId, String eventName, Object payload);
}