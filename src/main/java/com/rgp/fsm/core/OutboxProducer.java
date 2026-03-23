package com.rgp.fsm.core;

/**
 * SPI (Service Provider Interface) for the Transactional Outbox pattern.
 *
 * <p>Implement this interface to persist domain events to an outbox table
 * within the same database transaction as your state change. A separate
 * relay process (e.g., Debezium, polling) then publishes these events
 * to the message broker (RabbitMQ, Kafka, etc.).</p>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * @Component
 * public class JpaOutboxProducer implements OutboxProducer {
 *     @Override
 *     public void persist(String aggregateId, String eventName, Object payload) {
 *         outboxRepository.save(new OutboxEvent(aggregateId, eventName, payload));
 *     }
 * }
 * }</pre>
 */
public interface OutboxProducer {

    /**
     * Persist a domain event to the outbox.
     *
     * @param aggregateId the aggregate this event belongs to
     * @param eventName   the event type name (e.g., "ORDER_PAID", "PAY_FAILED")
     * @param payload     the event payload (can be a Map, DTO, or any serializable object)
     */
    void persist(String aggregateId, String eventName, Object payload);
}