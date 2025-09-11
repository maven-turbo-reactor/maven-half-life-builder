package com.github.seregamorph.maven.halflife;

import java.util.concurrent.Callable;

/**
 * @author Sergey Chernov
 */
public class OrderedCallable<T> implements Callable<T>, Comparable<OrderedCallable<T>> {

    private final OrderKey orderKey;
    private final Callable<T> delegate;

    public OrderedCallable(OrderKey orderKey, Callable<T> delegate) {
        this.orderKey = orderKey;
        this.delegate = delegate;
    }

    @Override
    public int compareTo(OrderedCallable<T> that) {
        return this.orderKey.compareTo(that.orderKey);
    }

    @Override
    public T call() throws Exception {
        return delegate.call();
    }
}
