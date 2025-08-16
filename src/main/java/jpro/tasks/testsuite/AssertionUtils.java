package jpro.tasks.testsuite;

import jpro.tasks.testsuite.exception.TestAssertionError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssertionUtils {

    public static <A, B> void assertEquals(A expected, B actual) {
        if (!(expected.getClass().equals(actual.getClass()) && expected.equals(actual))) {
            throw new TestAssertionError("Expected [" + expected + "] but got [" + actual + "]");
        }
    }

    public static void assertTrue(Boolean actual) {
        if (actual == null || !actual) {
            throw new TestAssertionError("Assertion is not true");
        }
    }
}
