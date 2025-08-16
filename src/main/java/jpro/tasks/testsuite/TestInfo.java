package jpro.tasks.testsuite;

import lombok.Builder;

@Builder
public record TestInfo(
        String testName,
        TestResult testResult,
        Throwable failureReason
) {
}
