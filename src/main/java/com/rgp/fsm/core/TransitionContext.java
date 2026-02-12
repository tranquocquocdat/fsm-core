public record TransitionContext<S, E>(
    String aggregateId,      // ID của đơn hàng/giao dịch
    Integer version,         // Version hiện tại (Optimistic Locking)
    S fromState,             // Trạng thái hiện tại
    E event,                 // Sự kiện đang xảy ra
    Map<String, Object> params // Dữ liệu đi kèm (payload)
) {}