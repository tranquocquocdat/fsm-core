package com.rgp.fsm.config;

import com.rgp.fsm.core.OutboxProducer;
import com.rgp.fsm.core.StateHistoryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for FSM-Core.
 *
 * <p>Provides default no-op beans for infrastructure interfaces.
 * Consumers can override these by defining their own beans in their application context.</p>
 *
 * <p>Auto-discovered via {@code META-INF/spring.factories} (Spring Boot 2.x)
 * and {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} (Spring Boot 3.x).</p>
 */
@Configuration
public class FsmAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FsmAutoConfiguration.class);

    /**
     * Default no-op {@link StateHistoryProcessor}.
     * Override this bean to persist audit logs / event history.
     */
    @Bean
    @ConditionalOnMissingBean
    public StateHistoryProcessor<?, ?> defaultStateHistoryProcessor() {
        log.debug("Using default no-op StateHistoryProcessor. Override this bean to enable audit logging.");
        return (ctx, nextState) -> {};
    }

    /**
     * Default no-op {@link OutboxProducer}.
     * Override this bean to persist outbox events to your database.
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxProducer defaultOutboxProducer() {
        log.debug("Using default no-op OutboxProducer. Override this bean to enable the Transactional Outbox pattern.");
        return (aggregateId, eventName, payload) -> {};
    }
}