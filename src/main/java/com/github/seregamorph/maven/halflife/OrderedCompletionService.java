package com.github.seregamorph.maven.halflife;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Sergey Chernov
 */
class OrderedCompletionService<T> {

    private final ExecutorService executor;
    private final BlockingQueue<Try<T>> completionQueue;

    OrderedCompletionService(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor);
        this.completionQueue = new LinkedBlockingQueue<>();
    }

    Future<T> submit(int order, Callable<T> buildCallable) {
        Objects.requireNonNull(buildCallable);
        return executor.submit(new OrderedCallable<>(order, () -> {
            try {
                T result = buildCallable.call();
                completionQueue.add(Try.success(result));
                return result;
            } catch (Throwable e) {
                completionQueue.add(Try.failure(e));
                if (e instanceof Exception) {
                    throw e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }));
    }

    T take() throws InterruptedException, ExecutionException {
        Try<T> t = completionQueue.take();
        return t.get();
    }

    private abstract static class Try<T> {
        abstract T get() throws ExecutionException;

        static <T> Try<T> success(T value) {
            return new Try<T>() {
                @Override
                T get() {
                    return value;
                }
            };
        }

        static <T> Try<T> failure(Throwable e) {
            return new Try<T>() {
                @Override
                T get() throws ExecutionException {
                    throw new ExecutionException(e);
                }
            };
        }
    }
}
