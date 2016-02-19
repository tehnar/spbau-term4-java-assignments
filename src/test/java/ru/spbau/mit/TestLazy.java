package ru.spbau.mit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created by Сева on 09.02.2016.
 */
public class TestLazy {

    private static class TestSupplier implements Supplier<Integer> {
        private final Random random = new Random(123);

        @Override
        public Integer get() {
            try {
                Thread.sleep(random.nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return random.nextInt();
        }
    }

    private static class SideEffectSupplier implements Supplier<Integer> {
        public List<Integer> sideEffects = Collections.synchronizedList(new ArrayList<>());
        @Override
        public Integer get() {
            int result = new TestSupplier().get();
            sideEffects.add(result);
            return result;
        }
    }

    private static class NullSupplier implements Supplier<Object> {
        private Object value = null;
        @Override
        public synchronized Object get() {
            if (value == null) {
                value = new Object();
                return null;
            }
            return value;
        }
    }

    @Rule
    public ExpectedException lazyNullSupplierException = ExpectedException.none();

    private List runLazyManyThreads(Lazy lazy) {
        List<Thread> threads = new ArrayList<>();
        final List results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 100; i++) {
            threads.add(new Thread(() -> results.add(lazy.get())));
        }

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    @Test
    public void TestOneThreadedLazy() {
        SideEffectSupplier supplier = new SideEffectSupplier();
        Lazy<Integer> lazy = LazyFactory.createLazy(supplier);
        for (int i = 0; i < 100; i++) {
            assertEquals(lazy.get(), lazy.get());
        }
        assertEquals(supplier.sideEffects.size(), 1);
    }

    @Test
    public void TestLazyNullSupplier() {
        lazyNullSupplierException.expect(IllegalArgumentException.class);
        LazyFactory.createLazy(null);

        lazyNullSupplierException.expect(IllegalArgumentException.class);
        LazyFactory.createMultiThreadLazy(null);

        lazyNullSupplierException.expect(IllegalArgumentException.class);
        LazyFactory.createNonBlockingMultiThreadLazy(null);
    }

    @Test
    public void TestNonBlockingMultiThreadedLazy() {
        Lazy<Integer> lazy = LazyFactory.createNonBlockingMultiThreadLazy(new TestSupplier());

        List results = runLazyManyThreads(lazy);
        assertEquals(results.stream().distinct().count(), 1);
    }

    @Test
    public void TestMultiThreadedLazy() {
        SideEffectSupplier supplier = new SideEffectSupplier();
        Lazy<Integer> lazy = LazyFactory.createMultiThreadLazy(supplier);

        List result = runLazyManyThreads(lazy);
        assertEquals(result.stream().distinct().count(), 1);
        assertEquals(supplier.sideEffects.size(), 1);
    }

    @Test
    public void TestNullLazySupplier() {
        final boolean[] failed = {false};
        final Lazy<Object> oneThreadLazy = LazyFactory.createLazy(new NullSupplier());
        assertEquals(oneThreadLazy.get(), null);

        final Lazy<Object> multiThreadLazy = LazyFactory.createMultiThreadLazy(new NullSupplier());
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                if (multiThreadLazy.get() != null) {
                    failed[0] = true;
                }
            }).start();
        }
        assertEquals(false, failed[0]);

        final Lazy<Object> atomicMultiThreadLazy = LazyFactory.createMultiThreadLazy(new NullSupplier());
        for (int i = 0; i < 10; i++) {
            new Thread(()-> {
                if (atomicMultiThreadLazy.get() != null) {
                    failed[0] = true;
                }
            }).start();
        }
        assertEquals(false, failed[0]);
    }
}
