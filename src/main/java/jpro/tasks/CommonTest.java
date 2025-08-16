package jpro.tasks;

import jpro.tasks.testsuite.annotation.*;
import lombok.extern.slf4j.Slf4j;

import static jpro.tasks.testsuite.AssertionUtils.assertEquals;
import static jpro.tasks.testsuite.AssertionUtils.assertTrue;

@Slf4j
public class CommonTest {

    @BeforeSuite
    static void startUpSuite() {
        log.info("Before Suite");
    }

    @AfterSuite
    static void tearDownSuite() {
        log.info("After Suite");
    }

    @BeforeEach
    void startUp() {
        log.info("Before Each");
    }

    @AfterEach
    void tearDown() {
        log.info("After Each");
    }

    @Test
    @Order(7)
    void simpleTest() {
        log.info("Third test");
        assertEquals(1, 1);
    }

    @Test
    void erroredTest() {
        log.info("Last test");
        throw new RuntimeException("This test fails");
    }

    @Test
    @Disabled
    void disabledTest() {
        log.info("This should not be seen");
    }

    @Test
    @Order
    void calculationsTest() {
        log.info("Second Test");
        assertEquals(1, 2);
    }

    @Test
    @Order(1)
    void testIsItTrue() {
        log.info("First Test");
        assertTrue(true);
    }
}
