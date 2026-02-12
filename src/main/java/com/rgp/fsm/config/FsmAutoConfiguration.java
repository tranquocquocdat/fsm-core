package com.rgp.fsm.config;

import com.rgp.fsm.core.StateHistoryProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FsmAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public StateHistoryProcessor<?, ?> defaultStateHistoryProcessor() {
        // Mặc định không làm gì, cho phép người dùng ghi đè bean này để lưu Audit Log
        return (ctx, nextState) -> {};
    }
}