package com.rgp.fsm.impl;

import com.rgp.fsm.core.BaseCommand;
import com.rgp.fsm.core.TransitionContext;

public class ShipOrderCommand implements BaseCommand<OrderStatus, OrderEvent> {
    @Override
    public void validate(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        String carrier = (String) ctx.params().get("carrier");
        if (carrier == null || carrier.isBlank()) {
            throw new Exception("Lỗi nghiệp vụ: Không tìm thấy nhà vận chuyển (Carrier)!");
        }
        System.out.println("[Validate] Nhà vận chuyển hợp lệ: " + carrier);
    }

    @Override
    public Object execute(TransitionContext<OrderStatus, OrderEvent> ctx) throws Exception {
        System.out.println("[Action] Đang tiến hành đóng gói và giao hàng cho: " + ctx.aggregateId());
        // Giả lập gọi sang đơn vị vận chuyển (GHTK, GHN...)
        String carrier = (String) ctx.params().get("carrier");
        System.out.println("[Action] Đã liên kết với: " + carrier);
        return "TRACK-ABC-123"; // Trả về mã vận đơn
    }

    @Override
    public void undo(TransitionContext<OrderStatus, OrderEvent> ctx) {
        System.out.println("[Command] UNDO: Hủy yêu cầu vận chuyển cho: " + ctx.aggregateId());
    }
}
