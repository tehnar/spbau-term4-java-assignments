package ru.spbau.mit;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

/**
 * Created by Сева on 09.02.2016.
 */
public class LazyFactory {
    private static class OneThreadLazy<T> implements Lazy<T> {
        private Supplier<T> supplier;
        T result;

        OneThreadLazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (supplier == null) {
                return result;
            }
            result = supplier.get();
            supplier = null;
            return result;
        }
    }

    private static class MultiThreadLazy<T> implements Lazy<T> {
        private static Object NONE = new Object();
        private Supplier<T> supplier;
        private volatile T result = (T) NONE;

        MultiThreadLazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (result == NONE) {
                synchronized (this) {
                    if (result == NONE) {
                        result = supplier.get();
                        supplier = null;
                    }
                }
            }
            return result;
        }
    }

    private static class NonBlockingMultiThreadLazy<T> implements Lazy<T> {
        private static Object NONE = new Object();
        private static final AtomicReferenceFieldUpdater<NonBlockingMultiThreadLazy, Object> UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(NonBlockingMultiThreadLazy.class, Object.class, "result");
        private volatile Object result = NONE;
        private Supplier<T> supplier;

        NonBlockingMultiThreadLazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (result == NONE) {
                T newResult = supplier.get();
                UPDATER.compareAndSet(this, NONE, newResult);
                supplier = null;
            }
            return (T) result;
        }
    }

    public static <T> Lazy<T> createLazy(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Expected non-null supplier!");
        }
        return new OneThreadLazy<>(supplier);
    }

    public static <T> Lazy<T> createMultiThreadLazy(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Expected non-null supplier!");
        }
        return new MultiThreadLazy<>(supplier);
    }

    public static <T> Lazy<T> createNonBlockingMultiThreadLazy(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Expected non-null supplier!");
        }
        return new NonBlockingMultiThreadLazy<>(supplier);
    }
}
