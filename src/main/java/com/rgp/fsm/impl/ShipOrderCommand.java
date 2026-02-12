package com.rgp.fsm.impl;

import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.TransitionContext;

public class ShipOrderCommand implements BaseCommand<OrderStatus, OrderEvent> {
    @Override
    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        System.out.println("[Command] Đang tiến hành đóng gói và giao hàng cho: " + ctx.aggregateId());
        // Giả lập gọi sang đơn vị vận chuyển (GHTK, GHN...)
        String carrier = (String) ctx.params().getOrDefault("carrier", "GHTK");
        System.out.println("[Command] Đơn vị vận chuyển: " + carrier);
        return "TRACK-ABC-123"; // Trả về mã vận đơn
    }

    @Override
    public void undo(TransitionContext<OrderStatus, OrderEvent> ctx) {
        System.out.println("[Command] UNDO: Hủy yêu cầu vận chuyển cho: " + ctx.aggregateId());
    }
}
