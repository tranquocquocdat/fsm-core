public interface BaseCommand {
    void execute(TransitionContext<?, ?> ctx) throws Exception;

    default void undo(TransitionContext<?, ?> ctx) {
        // Mặc định không làm gì, chỉ override khi cần rollback local
    }
}