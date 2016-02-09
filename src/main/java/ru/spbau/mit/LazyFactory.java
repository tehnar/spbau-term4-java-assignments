package ru.spbau.mit;

import java.util.concurrent.atomic.AtomicReference;
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
        private Supplier<T> supplier;
        T result;

        MultiThreadLazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (this.supplier != null) {
                synchronized (this) {
                    if (this.supplier != null) {
                        result = supplier.get();
                        supplier = null;
                    }
                }
            }
            return result;
        }
    }

    private static class NonBlockingMultiThreadLazy<T> implements Lazy<T> {
        private static Supplier PROCESSING_RESULT = ()->null;

        private AtomicReference<Supplier<T>> supplierReference;
        T result;

        NonBlockingMultiThreadLazy(Supplier<T> supplier) {
            this.supplierReference = new AtomicReference<>(supplier);
        }

        public T get() {
            Supplier<T> supplier = supplierReference.get();
            if (supplier == null) {
                return result;
            }
            T newResult = supplier.get();
            supplier = supplierReference.getAndUpdate((v) -> v == null ? null : PROCESSING_RESULT);
            if (supplier == null) { //value is already evaluated
                return result;
            } else if (supplier != PROCESSING_RESULT) { //value was not evaluated in any thread
                result = newResult;
            } else while (supplier == PROCESSING_RESULT) {
                //some other thread have already evaluated value but not assigned it
                supplier = supplierReference.get();
            }

            supplierReference.set(null);
            return result;
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
