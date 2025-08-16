package jpro.tasks;

import jpro.tasks.testsuite.TestRunner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        final var result = TestRunner.runTests(CommonTest.class);

        log.info("Test finished with result: {}", result);
    }
}