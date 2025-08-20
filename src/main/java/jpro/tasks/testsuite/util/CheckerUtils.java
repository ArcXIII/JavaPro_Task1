package jpro.tasks.testsuite.util;

import jpro.tasks.testsuite.annotation.Test;
import jpro.tasks.testsuite.exception.BadTestClassError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.String.format;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CheckerUtils {

    public static void checkNotStatic(final Method meth) {
        if (Modifier.isStatic(meth.getModifiers())) {
            throw new BadTestClassError(format("Method %s should not be static", meth.getName()));
        }
    }

    public static void checkIsStatic(final Method meth) {
        if (Modifier.isStatic(meth.getModifiers()))
            return;

        throw new BadTestClassError(format("Method %s should be static!", meth.getName()));
    }

    public static void checkDisabled(final Method meth) {
        final var testAnno = meth.getDeclaredAnnotation(Test.class);
        if (testAnno == null) {
            throw new BadTestClassError(format("Test method %s is annotated with @Disabled, but not annotated with @Test", meth.getName()));
        }
    }
}
