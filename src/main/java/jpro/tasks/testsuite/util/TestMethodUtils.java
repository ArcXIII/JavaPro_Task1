package jpro.tasks.testsuite.util;

import jpro.tasks.testsuite.annotation.Disabled;
import jpro.tasks.testsuite.annotation.Order;
import jpro.tasks.testsuite.annotation.Test;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Comparator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestMethodUtils {
    public static Comparator<Method> compareByOrder() {
        return Comparator.comparingInt(m -> {
            final var order = m.getAnnotation(Order.class);
            if (order != null) {
                return normalizeOrder(order.value());
            } else {
                return normalizeOrder(m.getAnnotation(Test.class).order());
            }
        });
    }

    private static int normalizeOrder(int order) {
        if (order > 10) {
            return 10;
        } else return Math.max(order, 0);
    }

    public static Comparator<Method> compareByName() {
        return Comparator.comparing(m -> {
            final var a = m.getAnnotation(Test.class);
            if (a != null) {
                return a.value();
            } else {
                return m.getName();
            }
        });
    }

    public static String getTestName(Method m) {
        final var a = m.getDeclaredAnnotation(Test.class);
        return a.value().isEmpty() ? m.getName() : a.value();
    }

    public static boolean isDisabled(final Method meth) {
        return meth.getDeclaredAnnotation(Disabled.class) != null;
    }
}
