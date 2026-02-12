package com.rgp.fsm.impl;

import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.TransitionContext;
import java.util.Map;

public class PayOrderCommand implements BaseCommand<OrderStatus, OrderEvent> {
    @Override
    public void validate(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        Map<String, Object> params = ctx.params();
        Double amount = (Double) params.get("amount");
        
        if (amount == null || amount <= 0) {
            throw new Exception("Thanh toán thất bại: Số tiền phải lớn hơn 0!");
        }
        System.out.println("[Validate] Dữ liệu thanh toán hợp lệ.");
    }

    @Override
    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        System.out.println("[Action] Đang xử lý thanh toán thực tế cho đơn hàng: " + ctx.aggregateId());
        
        Double amount = (Double) ctx.params().get("amount");
        System.out.println("[Action] Thực hiện qua Cổng thanh toán: " + amount + " VND");
        
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
