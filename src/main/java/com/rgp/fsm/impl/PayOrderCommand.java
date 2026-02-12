package com.rgp.fsm.impl;

import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.TransitionContext;
import java.util.Map;

public class PayOrderCommand implements BaseCommand<OrderStatus, OrderEvent> {
    @Override
    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        System.out.println("[Command] Đang xử lý thanh toán cho đơn hàng: " + ctx.aggregateId());
        System.out.println("[Command] Version: " + ctx.version());
        
        // Logic giả lập: Kiểm tra số tiền
        Map<String, Object> params = ctx.params();
        Double amount = (Double) params.get("amount");
        
        System.out.println("[Command] Thanh toán thành công: " + amount + " VND");
        
        // Trả về một MAP dữ liệu để "Bridge" sang bước sau
        return Map.of(
            "txId", "TX-" + System.currentTimeMillis(),
            "paidAt", java.time.LocalDateTime.now(),
            "status", "SUCCESS"
        );
    }

    @Override
    public void undo(TransitionContext<OrderStatus, OrderEvent> ctx) {
        System.out.println("[Command] UNDO: Hoàn tác thanh toán cho đơn hàng: " + ctx.aggregateId());
    }
}
