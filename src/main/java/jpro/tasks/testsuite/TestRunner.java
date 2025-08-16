package jpro.tasks.testsuite;

import jpro.tasks.testsuite.annotation.Disabled;
import jpro.tasks.testsuite.annotation.Order;
import jpro.tasks.testsuite.annotation.Test;
import jpro.tasks.testsuite.exception.BadTestClassError;
import jpro.tasks.testsuite.exception.TestAssertionError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static jpro.tasks.testsuite.TestMethodType.*;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestRunner {
    public static <T> Map<TestResult, List<TestInfo>> runTests(Class<T> clazz) {

        final var meths = clazz.getDeclaredMethods();

        final var preparedMethods = collectAndValidate(meths);

        return runMethods(clazz, preparedMethods);
    }

    private static <T> Map<TestResult, List<TestInfo>> runMethods(Class<T> clazz, final Map<TestMethodType, List<Method>> preparedMethods) {
        final Object testClassInstance = getTestClassInstance(clazz);

        final var beforeSuite = preparedMethods.getOrDefault(BEFORE_SUITE, new ArrayList<>());
        final var afterSuite = preparedMethods.getOrDefault(AFTER_SUITE, new ArrayList<>());
        final var beforeEach = preparedMethods.getOrDefault(BEFORE_EACH, new ArrayList<>());
        final var afterEach = preparedMethods.getOrDefault(AFTER_EACH, new ArrayList<>());

        runPrepMeths(testClassInstance, beforeSuite);
        final var result = preparedMethods.getOrDefault(TestMethodType.TEST, new ArrayList<>()).stream()
                .sorted(Comparator.comparingInt(m -> {
                    final var order = m.getAnnotation(Order.class);
                    if (order != null) {
                        return order.value();
                    } else {
                        return Integer.MAX_VALUE;
                    }
                }))
                .map(runTestMethod(testClassInstance, beforeEach, afterEach))
                .collect(Collectors.groupingBy(TestInfo::testResult));
        runPrepMeths(testClassInstance, afterSuite);
        return result;
    }

    private static Function<Method, TestInfo> runTestMethod(final Object testClassInstance, final List<Method> beforeEach, final List<Method> afterEach) {
        return test -> {
            if (isDisabled(test)) {
                return TestInfo.builder()
                        .testName(test.getName())
                        .testResult(TestResult.SKIPPED)
                        .build();
            }


            runPrepMeths(testClassInstance, beforeEach);
            try {
                test.invoke(testClassInstance, (Object[]) null);
                runPrepMeths(testClassInstance, afterEach);
                return TestInfo.builder()
                        .testName(test.getName())
                        .testResult(TestResult.SUCCESS)
                        .build();
            } catch (IllegalAccessException e) {
                runPrepMeths(testClassInstance, afterEach);
                throw new BadTestClassError(e.getMessage());
            } catch (InvocationTargetException | BadTestClassError e) {
                runPrepMeths(testClassInstance, afterEach);
                if (e.getCause() instanceof TestAssertionError ex) {
                    return TestInfo.builder()
                            .testName(test.getName())
                            .testResult(TestResult.FAILED)
                            .failureReason(ex)
                            .build();
                } else {
                    return TestInfo.builder()
                            .testName(test.getName())
                            .testResult(TestResult.ERROR)
                            .failureReason(e.getCause())
                            .build();
                }
            }
        };
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

    private static Map<TestMethodType, List<Method>> collectAndValidate(final Method[] meths) {
        final var result = new EnumMap<TestMethodType, List<Method>>(TestMethodType.class);
        for (final var meth : meths) {
            final var annos = meth.getDeclaredAnnotations();
            for (final var anno : annos) {
                switch (anno.annotationType().getSimpleName()) {
                    case "AfterEach" -> {
                        checkNotStatic(meth);
                        meth.setAccessible(true);
                        putOrAdd(result, AFTER_EACH, meth);
                    }
                    case "BeforeEach" -> {
                        checkNotStatic(meth);
                        meth.setAccessible(true);
                        putOrAdd(result, BEFORE_EACH, meth);
                    }
                    case "Test" -> {
                        checkNotStatic(meth);
                        meth.setAccessible(true);
                        putOrAdd(result, TestMethodType.TEST, meth);
                    }
                    case "AfterSuite" -> {
                        checkIsStatic(meth);
                        meth.setAccessible(true);
                        putOrAdd(result, AFTER_SUITE, meth);
                    }
                    case "BeforeSuite" -> {
                        checkIsStatic(meth);
                        meth.setAccessible(true);
                        putOrAdd(result, BEFORE_SUITE, meth);
                    }
                    case "Disabled" -> checkDisabled(meth);
                    default -> log.info("Skipping unknown annotation: {}", anno.annotationType().getSimpleName());
                }
            }
        }
        return result;
    }

    private static <K, V> void putOrAdd(Map<K, List<V>> map, K key, V value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, new ArrayList<>(Collections.singletonList(value)));
        }
    }

    private static void checkDisabled(final Method meth) {
        final var testAnno = meth.getDeclaredAnnotation(Test.class);
        if (testAnno == null) {
            throw new BadTestClassError(format("Test method %s is annotated with @Disabled, but not annotated with @Test", meth.getName()));
        }
    }

    private static void checkNotStatic(final Method meth) {
        if (Modifier.isStatic(meth.getModifiers())) {
            throw new BadTestClassError(format("Method %s should not be static", meth.getName()));
        }
    }

    private static void checkIsStatic(final Method meth) {
        if (Modifier.isStatic(meth.getModifiers()))
            return;

        throw new BadTestClassError(format("%s should be static!", meth.getName()));
    }

    private static boolean isDisabled(final Method meth) {
        return meth.getDeclaredAnnotation(Disabled.class) != null;
    }
}
