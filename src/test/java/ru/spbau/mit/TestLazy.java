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
        final private Random random = new Random(123);

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
        public List<Integer> sideEffects = new ArrayList<>();
        @Override
        public Integer get() {
            int result = new TestSupplier().get();
            sideEffects.add(result);
            return result;
        }
    }

    private static class NullSupplier implements Supplier<Object> {

        @Override
        public Object get() {
            return null;
        }
    }

    @Rule
    public ExpectedException lazyNullSupplierException = ExpectedException.none();

    @Test
    public void TestOneThreadedLazy() {
        Lazy<Integer> lazy = LazyFactory.createLazy(new TestSupplier());
        assertEquals(lazy.get(), lazy.get());
    }

    @Test
    public void TestLazyNullSupplier() {
        lazyNullSupplierException.expect(IllegalArgumentException.class);
        Lazy<Integer> lazy = LazyFactory.createLazy(null);

        lazyNullSupplierException.expect(IllegalArgumentException.class);
        lazy = LazyFactory.createMultiThreadLazy(null);

        lazyNullSupplierException.expect(IllegalArgumentException.class);
        lazy = LazyFactory.createNonBlockingMultiThreadLazy(null);
    }

    @Test
    public void TestNonBlockingMultiThreadedLazy() {
        final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        Lazy<Integer> lazy = LazyFactory.createNonBlockingMultiThreadLazy(new TestSupplier());
        
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            threads.add(new Thread(() -> results.add(lazy.get())));
        }

        threads.forEach(java.lang.Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertEquals(results.stream().distinct().count(), 1);
    }

    @Test
    public void TestMultiThreadedLazy() {
        SideEffectSupplier supplier = new SideEffectSupplier();
        Lazy<Integer> lazy = LazyFactory.createMultiThreadLazy(supplier);

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            threads.add(new Thread(lazy::get));
        }

        threads.forEach(java.lang.Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertEquals(supplier.sideEffects.size(), 1);
    }

    @Test
    public void TestNullLazySupplier() {
        final Lazy<Object> oneThreadLazy = LazyFactory.createLazy(new NullSupplier());
        assertEquals(oneThreadLazy.get(), null);

        final Lazy<Object> multiThreadLazy = LazyFactory.createMultiThreadLazy(new NullSupplier());
        for (int i = 0; i < 10; i++)
            new Thread(()->assertEquals(multiThreadLazy.get(), null)).start();

        final Lazy<Object> atomicMultiThreadLazy = LazyFactory.createMultiThreadLazy(new NullSupplier());
        for (int i = 0; i < 10; i++)
            new Thread(()->assertEquals(atomicMultiThreadLazy.get(), null)).start();
    }
}
