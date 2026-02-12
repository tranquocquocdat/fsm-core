package com.rgp.fsm.impl;

import com.rgp.fsm.engine.FluentManager;
import com.rgp.fsm.engine.StateMachineBuilder;
import com.rgp.fsm.engine.StepResult;
import java.util.Map;

public class ExampleUsage {
    public static void main(String[] args) throws Exception {
        var outbox = new ConsoleDemo.ConsoleOutboxProducer();
        var history = new ConsoleDemo.ConsoleStateHistoryProcessor();

        // 1. Cấu hình State Machine (Cấu trúc mới: Per-step infra + Guard)
        FluentManager<OrderStatus, OrderEvent> fsm = new StateMachineBuilder<OrderStatus, OrderEvent>()
            .from(OrderStatus.CREATED).on(OrderEvent.PAY)
                .action(new PayOrderCommand())
                .to(OrderStatus.PAID)
                .ifSuccess()
                    .outbox(outbox, "ORDER_PAID", ctx -> Map.of("id", ctx.aggregateId(), "amount", ctx.params().get("amount")))
                    .history(history)
                .ifError()
                    .to(OrderStatus.CANCELLED)
                    .undo(ctx -> System.out.println("[Undo] Đã cấu hình: Rollback tiền cho " + ctx.aggregateId()))
                    .outbox(outbox, "PAY_FAILED")
            .and()
            
            .from(OrderStatus.PAID).on(OrderEvent.SHIP)
                .guard(ctx -> ctx.params().containsKey("carrier")) // <--- GUARD: Phải có carrier mới cho SHIP
                .action(new ShipOrderCommand())
                .to(OrderStatus.SHIPPING)
                .ifSuccess()
                    .outbox(outbox, "ORDER_SHIPPED")
            .and()
            .build();

        // 2. Thực thi Fluent với tính năng BRIDGE (Tự động nối dữ liệu)
        System.out.println("=== TEST BRIDGE AUTOMATION ===");

        StepResult<OrderStatus> payRes = fsm.fire("ORD-555")
            .version(1)
            .from(OrderStatus.CREATED)
            .on(OrderEvent.PAY)
            .params(Map.of("amount", 200.0))
            .execute();

        if (payRes.state() == OrderStatus.PAID) {
            // SỬ DỤNG .bridge() để truyền toàn bộ Map kết quả từ bước PAY sang SHIP
            StepResult<OrderStatus> shipRes = fsm.fire("ORD-555")
                .bridge(payRes) // <--- CÂY CẦU TỰ ĐỘNG GỬI MAP DATA SANG
                .version(2)
                .from(OrderStatus.PAID)
                .on(OrderEvent.SHIP)
                .params(Map.of("carrier", "GiaoHangNhanh")) // Thêm dữ liệu bổ sung nếu muốn
                .execute();
            
            System.out.println(">>> Giao hàng xong. Trạng thái cuối: " + shipRes.state());
            // Bên trong ShipOrderCommand giờ đây có thể lấy ctx.params().get("txId") từ bước trước!
        }
    }
}
