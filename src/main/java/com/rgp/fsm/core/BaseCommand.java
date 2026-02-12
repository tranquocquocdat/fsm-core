package com.rgp.fsm.core;

public interface BaseCommand<S, E> {
    /**
     * Bước 1: Kiểm tra nghiệp vụ (Validation). 
     * Ném Exception nếu dữ liệu không hợp lệ.
     */
    default void validate(TransitionContext<S, E> ctx) throws Exception {
        // Mặc định cho qua
    }

    /**
     * Bước 2: Thực thi thay đổi dữ liệu (Execution).
     */
    Object execute(TransitionContext<S, E> ctx) throws Exception;

    /**
     * Bước 3: Hoàn tác (Undo).
     */
    default void undo(TransitionContext<S, E> ctx) {
        // Mặc định không làm gì
    }
}