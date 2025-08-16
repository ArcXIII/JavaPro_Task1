package jpro.tasks.testsuite.exception;

public class TestAssertionError extends RuntimeException {
    public TestAssertionError(final String message) {
        super(message);
    }
}
