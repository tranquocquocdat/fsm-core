public record Transition<S, E>(
    S from,
    E event,
    S to,
    BaseCommand action,
    String eventToEmit // Tên event để ghi vào Outbox (có thể null)
) {}