package jpro.tasks.testsuite;

import jpro.tasks.testsuite.exception.BadTestClassError;
import jpro.tasks.testsuite.exception.TestAssertionError;
import jpro.tasks.testsuite.util.TestMethodUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static jpro.tasks.testsuite.TestMethodType.*;
import static jpro.tasks.testsuite.util.CheckerUtils.*;
import static jpro.tasks.testsuite.util.TestMethodUtils.*;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestRunner {
    public static <T> Map<TestResult, List<TestInfo>> runTests(Class<T> clazz) {

        final var meths = clazz.getDeclaredMethods();

        final var preparedMethods = collectAndValidate(meths);

        return runMethods(clazz, preparedMethods);
    }

    private static Map<TestMethodType, List<Method>> collectAndValidate(final Method[] meths) {
        final var result = new EnumMap<TestMethodType, List<Method>>(TestMethodType.class);
        for (final var meth : meths) {
            meth.setAccessible(true);
            final var annos = meth.getDeclaredAnnotations();
            for (final var anno : annos) {
                switch (anno.annotationType().getSimpleName()) {
                    case "AfterEach" -> {
                        checkNotStatic(meth);
                        result.computeIfAbsent(AFTER_EACH, k -> new ArrayList<>()).add(meth);
                    }
                    case "BeforeEach" -> {
                        checkNotStatic(meth);
                        result.computeIfAbsent(BEFORE_EACH, k -> new ArrayList<>()).add(meth);
                    }
                    case "Test" -> {
                        checkNotStatic(meth);
                        result.computeIfAbsent(TEST, k -> new ArrayList<>()).add(meth);
                    }
                    case "AfterSuite" -> {
                        checkIsStatic(meth);
                        result.computeIfAbsent(AFTER_SUITE, k -> new ArrayList<>()).add(meth);
                    }
                    case "BeforeSuite" -> {
                        checkIsStatic(meth);
                        result.computeIfAbsent(BEFORE_SUITE, k -> new ArrayList<>()).add(meth);
                    }
                    case "Disabled" -> checkDisabled(meth);
                    case "Order" -> { /*doNothing*/ }
                    default -> log.info("Skipping unknown annotation: {}", anno.annotationType().getSimpleName());
                }
            }
        }
        return result;
    }

    private static <T> Map<TestResult, List<TestInfo>> runMethods(Class<T> clazz, final Map<TestMethodType, List<Method>> preparedMethods) {
        final var beforeSuite = preparedMethods.getOrDefault(BEFORE_SUITE, new ArrayList<>());
        final var afterSuite = preparedMethods.getOrDefault(AFTER_SUITE, new ArrayList<>());
        final var beforeEach = preparedMethods.getOrDefault(BEFORE_EACH, new ArrayList<>());
        final var afterEach = preparedMethods.getOrDefault(AFTER_EACH, new ArrayList<>());

        runPrepMeths(null, beforeSuite);
        final var result = preparedMethods.getOrDefault(TestMethodType.TEST, new ArrayList<>()).stream()
                .sorted(compareByOrder().thenComparing(compareByName()))
                .map(runTestMethod(clazz, beforeEach, afterEach))
                .collect(Collectors.groupingBy(TestInfo::testResult));
        runPrepMeths(null, afterSuite);
        return result;
    }

    private static <T> Function<Method, TestInfo> runTestMethod(Class<T> testClass, final List<Method> beforeEach, final List<Method> afterEach) {
        return test -> {
            if (TestMethodUtils.isDisabled(test)) {
                return TestInfo.builder()
                        .testName(getTestName(test))
                        .testResult(TestResult.SKIPPED)
                        .build();
            }
            final Object testClassInstance = getTestClassInstance(testClass);
            runPrepMeths(testClassInstance, beforeEach);
            var testResult = runTestMethod(testClassInstance, test);
            runPrepMeths(testClassInstance, afterEach);
            return testResult;
        };
    }

    private static TestInfo runTestMethod(final Object testClassInstance, final Method test) {
        final var name = TestMethodUtils.getTestName(test);
        try {
            test.invoke(testClassInstance, (Object[]) null);
            return TestInfo.builder()
                    .testName(name)
                    .testResult(TestResult.SUCCESS)
                    .build();
        } catch (IllegalAccessException | InvocationTargetException | BadTestClassError e) {
            if (e.getCause() instanceof TestAssertionError ex) {
                return TestInfo.builder()
                        .testName(name)
                        .testResult(TestResult.FAILED)
                        .failureReason(ex)
                        .build();
            } else {
                return TestInfo.builder()
                        .testName(name)
                        .testResult(TestResult.ERROR)
                        .failureReason(e)
                        .build();
            }
        }
    }

    private static void runPrepMeths(final Object instance, List<Method> beforeSuite) {
        try {
            for (Method method : beforeSuite) {
                method.invoke(instance, (Object[]) null);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Unable to run support methods (After- or Before-): {}", e.getMessage(), e);
        }
    }

    private static <T> Object getTestClassInstance(final Class<T> clazz) {
        Object testClassInstance;
        try {
            final var ctr = clazz.getConstructor();
            testClassInstance = ctr.newInstance();
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new BadTestClassError("Unable to create instance of class: " + clazz.getName());
        }
        return testClassInstance;
    }

}
