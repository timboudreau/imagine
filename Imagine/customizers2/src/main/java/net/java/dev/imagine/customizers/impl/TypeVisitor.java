package net.java.dev.imagine.customizers.impl;

/**
 * Simple use of the visitor pattern to visit classes and interfaces implemented
 * by an object or type, and return the first non-null result.
 *
 * @author Tim Boudreau
 */
public abstract class TypeVisitor<T> {

    public final T visitTypesOf(Object o) {
        return visitTypes(o.getClass());
    }

    public final T visitTypes(Class<?> type) {
        if (type == null) {
            return null;
        }
        Class<?> test = type;
        T result = null;
        while (test != Object.class && result == null) {
            result = visit(test);
            test = test.getSuperclass();
        }
        if (result == null) {
            for (Class<?> i : type.getInterfaces()) {
                result = visit(test);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    protected abstract T visit(Class<?> type);
}
