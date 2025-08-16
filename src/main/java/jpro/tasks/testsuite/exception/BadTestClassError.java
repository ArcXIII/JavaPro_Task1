package jpro.tasks.testsuite.exception;

public class BadTestClassError extends RuntimeException {
    public BadTestClassError(String message) {
        super(message);
    }
}
